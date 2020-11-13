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

import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.datastore.DataStoreConfigurationProvider;
import com.amplifyframework.datastore.DataStoreConflictHandler;
import com.amplifyframework.datastore.DataStoreConflictHandler.ConflictData;
import com.amplifyframework.datastore.DataStoreConflictHandler.ConflictResolutionDecision;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.appsync.AppSync;
import com.amplifyframework.datastore.appsync.AppSyncConflictUnhandledError;
import com.amplifyframework.datastore.appsync.ModelMetadata;
import com.amplifyframework.datastore.appsync.ModelWithMetadata;

import io.reactivex.rxjava3.core.Single;

/**
 * ConflictResolver is a helper utility for the {@link MutationProcessor}.
 * It is built specifically to handle ConflictUnhandledError that may be
 * returned when the {@link MutationProcessor} attempts to publish
 * local data up to AppSync.
 *
 * ConflictResolver's primary entry point is the
 * {@link ConflictResolver#resolve(PendingMutation, AppSyncConflictUnhandledError)}.
 * This method will try to rectify state with AppSync by applying the conflict handler
 * in the {@link DataStoreConfigurationProvider#getConfiguration()}.
 *
 * The ConflictResolver will return a {@link ModelWithMetadata} that is understood
 * to be the final, resolved version of the data, to which both the server and local
 * database should agree.
 *
 * After {@link ConflictResolver#resolve(PendingMutation, AppSyncConflictUnhandledError)}
 * is invoked by the MutationProcessor, the MutationProcessor must merge the returned
 * {@link ModelWithMetadata} into the local store, unconditionally.
 */
final class ConflictResolver {
    private final DataStoreConfigurationProvider configurationProvider;
    private final AppSync appSync;

    ConflictResolver(DataStoreConfigurationProvider configurationProvider, AppSync appSync) {
        this.configurationProvider = configurationProvider;
        this.appSync = appSync;
    }

    @NonNull
    <T extends Model> Single<ModelWithMetadata<T>> resolve(
            @NonNull PendingMutation<T> pendingMutation,
            @NonNull AppSyncConflictUnhandledError<T> conflictUnhandledError) {
        final DataStoreConflictHandler conflictHandler;
        try {
            conflictHandler = configurationProvider.getConfiguration().getConflictHandler();
        } catch (DataStoreException badConfigurationProvider) {
            return Single.error(badConfigurationProvider);
        }

        ModelWithMetadata<T> serverData = conflictUnhandledError.getServerVersion();
        ModelMetadata metadata = serverData.getSyncMetadata();
        T remote = serverData.getModel();
        T local = pendingMutation.getMutatedItem();
        ConflictData<T> conflictData = ConflictData.create(local, remote);

        return Single
            .<ConflictResolutionDecision<? extends Model>>create(emitter ->
                conflictHandler.onConflictDetected(conflictData, emitter::onSuccess)
            )
            .flatMap(decision -> {
                @SuppressWarnings("unchecked")
                ConflictResolutionDecision<T> typedDecision = (ConflictResolutionDecision<T>) decision;
                return resolveModelAndMetadata(conflictData, metadata, typedDecision);
            });
    }

    @NonNull
    private <T extends Model> Single<ModelWithMetadata<T>> resolveModelAndMetadata(
            @NonNull ConflictData<T> conflictData,
            @NonNull ModelMetadata metadata,
            @NonNull ConflictResolutionDecision<T> decision) {
        switch (decision.getResolutionStrategy()) {
            case RETRY_LOCAL:
                return publish(conflictData.getLocal(), metadata.getVersion());
            case APPLY_REMOTE:
                // No network operations to do. The resolution is just to return
                // the resolved data, so it can be applied locally.
                return Single.just(new ModelWithMetadata<>(conflictData.getRemote(), metadata));
            case RETRY:
                return publish(decision.getCustomModel(), metadata.getVersion());
            default:
                throw new IllegalStateException("Unknown resolution strategy = " + decision.getResolutionStrategy());
        }
    }

    @NonNull
    private <T extends Model> Single<ModelWithMetadata<T>> publish(@NonNull T model, int version) {
        return Single
            .<GraphQLResponse<ModelWithMetadata<T>>>create(emitter -> {
                final ModelSchema schema = ModelSchema.fromModelClass(model.getClass());
                appSync.update(model, schema, version, emitter::onSuccess, emitter::onError);
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
}
