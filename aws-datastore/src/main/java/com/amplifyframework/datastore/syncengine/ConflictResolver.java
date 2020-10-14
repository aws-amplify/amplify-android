/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.datastore.syncengine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.temporal.Temporal;
import com.amplifyframework.datastore.DataStoreConfiguration;
import com.amplifyframework.datastore.DataStoreConfigurationProvider;
import com.amplifyframework.datastore.DataStoreConflictHandler;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.appsync.AppSync;
import com.amplifyframework.datastore.appsync.AppSyncConflictUnhandledError;
import com.amplifyframework.datastore.appsync.ModelMetadata;
import com.amplifyframework.datastore.appsync.ModelWithMetadata;
import com.amplifyframework.datastore.syncengine.ConflictResolutionDecision.ResolutionStrategy;

import java.util.Date;
import java.util.Objects;

import io.reactivex.rxjava3.core.Single;

final class ConflictResolver {
    private final DataStoreConfigurationProvider configurationProvider;
    private final SyncTimeRegistry syncTimeRegistry;
    private final VersionRepository versionRepository;
    private final AppSync appSync;

    ConflictResolver(
            DataStoreConfigurationProvider configurationProvider,
            SyncTimeRegistry syncTimeRegistry,
            VersionRepository versionRepository,
            AppSync appSync) {
        this.configurationProvider = configurationProvider;
        this.syncTimeRegistry = syncTimeRegistry;
        this.versionRepository = versionRepository;
        this.appSync = appSync;
    }

    @NonNull
    <T extends Model> Single<ModelWithMetadata<T>> resolve(
            @NonNull PendingMutation<T> pendingMutation,
            @NonNull AppSyncConflictUnhandledError<T> conflictUnhandledError) {
        final DataStoreConflictHandler conflictHandler;
        try {
            DataStoreConfiguration configuration = configurationProvider.getConfiguration();
            conflictHandler = configuration.getDataStoreConflictHandler();
        } catch (DataStoreException badConfigurationProvider) {
            return Single.error(badConfigurationProvider);
        }

        return computeLocalVersion(pendingMutation)
            .map(localCopy -> ConflictData.create(localCopy, conflictUnhandledError.getServerVersion()))
            .flatMap(conflictData -> Single.<ResolutionData<T>>create(emitter ->
                conflictHandler.onConflictDetected(conflictData, decision -> {
                    // In all cases, we want to merge the desired version
                    // of the data back into the local store.
                    ResolutionStrategy strategy =
                        decision.getResolutionStrategy();
                    ModelWithMetadata<T> resolutionVersion =
                        computeResolutionVersion(conflictData, decision);
                    Class<T> clazz = pendingMutation.getClassOfMutatedItem();
                    ResolutionData<T> resolutionData =
                        new ResolutionData<>(clazz, resolutionVersion, strategy);
                    emitter.onSuccess(resolutionData);
                })
            ))
            .flatMap(this::resolve);
    }

    private <T extends Model> Single<ModelWithMetadata<T>> resolve(ResolutionData<T> resolutionData) {
        if (ResolutionStrategy.APPLY_REMOTE.equals(resolutionData.getResolutionStrategy())) {
            // No network operations to do. The resolution is just to return
            // the resolved data, so it can be applied locally.
            return Single.just(resolutionData.getResolvedModel());
        }

        ModelWithMetadata<T> resolvedModel = resolutionData.getResolvedModel();
        ModelMetadata resolutionMetadata = resolvedModel.getSyncMetadata();
        boolean isDeleted = Boolean.TRUE.equals(resolutionMetadata.isDeleted());
        Integer version = resolutionMetadata.getVersion() == null ? 0 : resolutionMetadata.getVersion();
        return Single
            .<GraphQLResponse<ModelWithMetadata<T>>>create(emitter -> {
                if (isDeleted) {
                    Class<T> clazz = resolutionData.getModelClass();
                    String modelId = resolutionMetadata.getId();
                    appSync.delete(clazz, modelId, version, emitter::onSuccess, emitter::onError);
                } else {
                    appSync.update(resolvedModel.getModel(), version, emitter::onSuccess, emitter::onError);
                }
            })
            .flatMap(response -> {
                if (response.hasErrors() || !response.hasData()) {
                    return Single.error(new DataStoreException(
                       "Error encountered while processing conflict data.",
                       "Please check your conflict handler logic."
                    ));
                }
                return Single.just(response.getData());
            });
    }

    private <T extends Model> Single<ModelWithMetadata<T>> computeLocalVersion(PendingMutation<T> pendingMutation) {
        // Convert the local PendingMutation to the same ModelWithMetadata shape that's
        // used for synchronized data.
        boolean isDeleted = PendingMutation.Type.DELETE.equals(pendingMutation.getMutationType());
        T model = pendingMutation.getMutatedItem();
        return lookupLastSyncTime(pendingMutation.getClassOfMutatedItem())
            .flatMap(lastChangedAt ->
                versionRepository.findModelVersion(model)
                    .map(version -> new ModelMetadata(model.getId(), isDeleted, version, lastChangedAt))
            )
            .map(localMetadata -> new ModelWithMetadata<>(model, localMetadata));
    }

    @NonNull
    private <T extends Model> Single<Temporal.Timestamp> lookupLastSyncTime(@NonNull Class<T> modelClazz) {
        return syncTimeRegistry.lookupLastSyncTime(modelClazz)
            .map(SyncTime::toLong)
            .map(Date::new)
            .map(Temporal.Timestamp::new);
    }

    @NonNull
    private <T extends Model> ModelWithMetadata<T> computeResolutionVersion(
            @NonNull ConflictData<T> conflictData, @NonNull ConflictResolutionDecision<T> decision) {
        // If the strategy is apply remote or retry local, we already know the version we'll use.
        // We simply extract it from the conflict data.
        if (ResolutionStrategy.APPLY_REMOTE.equals(decision.getResolutionStrategy())) {
            return conflictData.getRemote();
        } else if (ResolutionStrategy.RETRY_LOCAL.equals(decision.getResolutionStrategy())) {
            return conflictData.getLocal();
        }

        // If the strategy is to use a user-provided model, we need to compute
        // a ModelWithMetadata based on the current server state.
        return computeCustomVersion(conflictData.getRemote(), decision);
    }

    @NonNull
    private <T extends Model> ModelWithMetadata<T> computeCustomVersion(
            @NonNull ModelWithMetadata<T> serverVersion, @NonNull ConflictResolutionDecision<T> decision) {
        // When the provided model is non-null, we're doing an update.
        // When its null, that means "delete the version on the server."
        boolean isDeleted = false;
        T model = decision.getCustomModel();
        if (model == null) {
            isDeleted = true;
            model = serverVersion.getModel();
        }
        // The metadata we need to use is mostly like what's on the server
        // We just need to patch the model and isDeleted metadata based on
        // the contents of the provided model.
        ModelMetadata remoteMetadata = serverVersion.getSyncMetadata();
        Integer remoteVersion = remoteMetadata.getVersion();
        Temporal.Timestamp remoteLastChangedAt = remoteMetadata.getLastChangedAt();
        ModelMetadata retryMetadata =
            new ModelMetadata(model.getId(), isDeleted, remoteVersion, remoteLastChangedAt);
        return new ModelWithMetadata<>(model, retryMetadata);
    }

    /**
     * After the user selects a conflict resolution strategy, we had some
     * work to do internally to rectify the version of the model that we should
     * actually be using. This is a little POJO to store that information in a
     * central place. Compare to the {@link ConflictResolutionDecision} which
     * represents a user selection, not an internal system state. Namely, the
     * {@link ConflictResolutionDecision} stores a model, whereas this object stores
     * a model coupled with our understanding of its correct metadata.
     * @param <T> The type of model being resolved
     */
    static final class ResolutionData<T extends Model> {
        private final Class<T> modelClass;
        private final ModelWithMetadata<T> resolvedModel;
        private final ResolutionStrategy resolutionStrategy;

        ResolutionData(
                @NonNull Class<T> modelClass,
                @NonNull ModelWithMetadata<T> resolvedModel,
                @NonNull ResolutionStrategy resolutionStrategy) {
            this.modelClass = Objects.requireNonNull(modelClass);
            this.resolvedModel = Objects.requireNonNull(resolvedModel);
            this.resolutionStrategy = Objects.requireNonNull(resolutionStrategy);
        }

        /**
         * Gets the class of the resolved model data.
         * @return Class of resolved model data
         */
        public Class<T> getModelClass() {
            return modelClass;
        }

        /**
         * Gets the version of the model that is determined to
         * be resolved.
         * @return Resolved model data
         */
        public ModelWithMetadata<T> getResolvedModel() {
            return resolvedModel;
        }

        /**
         * Gets the strategy that is used for the resolution.
         * @return Resolution strategy
         */
        public ResolutionStrategy getResolutionStrategy() {
            return resolutionStrategy;
        }

        @Override
        public boolean equals(@Nullable Object thatObject) {
            if (this == thatObject) {
                return true;
            }
            if (thatObject == null || getClass() != thatObject.getClass()) {
                return false;
            }

            ResolutionData<?> that = (ResolutionData<?>) thatObject;

            if (!modelClass.equals(that.modelClass)) {
                return false;
            }
            if (!resolvedModel.equals(that.resolvedModel)) {
                return false;
            }
            return resolutionStrategy == that.resolutionStrategy;
        }

        @Override
        public int hashCode() {
            int result = modelClass.hashCode();
            result = 31 * result + resolvedModel.hashCode();
            result = 31 * result + resolutionStrategy.hashCode();
            return result;
        }

        @NonNull
        @Override
        public String toString() {
            return "ResolutionData{" +
                "modelClass=" + modelClass +
                ", resolutionData=" + resolvedModel +
                ", resolutionStrategy=" + resolutionStrategy +
                '}';
        }
    }
}
