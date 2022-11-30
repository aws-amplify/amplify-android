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
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.SchemaRegistry;
import com.amplifyframework.core.model.SerializedModel;
import com.amplifyframework.datastore.DataStoreConfigurationProvider;
import com.amplifyframework.datastore.DataStoreConflictHandler;
import com.amplifyframework.datastore.DataStoreConflictHandler.ConflictData;
import com.amplifyframework.datastore.DataStoreConflictHandler.ConflictResolutionDecision;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.appsync.AppSync;
import com.amplifyframework.datastore.appsync.AppSyncConflictUnhandledError;
import com.amplifyframework.datastore.appsync.ModelMetadata;
import com.amplifyframework.datastore.appsync.ModelWithMetadata;
import com.amplifyframework.logging.Logger;
import com.amplifyframework.util.GsonFactory;

import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.Objects;

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
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-datastore");

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
        T local = getMutatedModelFromSerializedModel(pendingMutation);
        T remote = getServerModel(serverData, pendingMutation.getMutatedItem());
        ConflictData<T> conflictData = ConflictData.create(local, remote);

        return Single
            .<ConflictResolutionDecision<? extends Model>>create(emitter -> {
                LOG.debug("Invoking conflict handler");
                conflictHandler.onConflictDetected(conflictData, emitter::onSuccess);
            })
            .flatMap(decision -> {
                LOG.debug(String.format("Conflict handler decision: %s", decision));
                @SuppressWarnings("unchecked")
                ConflictResolutionDecision<T> typedDecision = (ConflictResolutionDecision<T>) decision;
                return resolveModelAndMetadata(conflictData, metadata, typedDecision);
            });
    }

    /**
     * The local data representation coming from android app comes here as serialized model of the type user defined
     * model. For appsync request this data has to be converted to the user defined model. The local data representation
     * coming for flutter is a serialized model of type serialized model which needs to be passed as is.
     * Also if the data
     * is coming as user defined model it doesn't need to be converted.
     * @param pendingMutation pending mutation coming from mutation outbox.
     * @param <T> Type of the Pending mutation.
     * @return Model
     */
    @SuppressWarnings("unchecked")
    private <T extends Model> T getMutatedModelFromSerializedModel(@NonNull PendingMutation<T> pendingMutation) {
        T local = pendingMutation.getMutatedItem();
        if (local instanceof SerializedModel) {
            SerializedModel serializedModel = (SerializedModel) local;
            Type modelType = Objects.requireNonNull(((SerializedModel) pendingMutation.getMutatedItem())
                                                                    .getModelSchema()).getModelClass();
            if (modelType != SerializedModel.class) {
                Gson gson = GsonFactory.instance();
                String jsonString = gson.toJson(serializedModel.getSerializedData());
                local = gson.fromJson(jsonString, modelType);
            }
        }
        return local;
    }

    /***
     * Server Model is deserialized from ConflictUnhandled error coming from appsync. For flutter it is a serialized
     * model. The deserialization process doesn't have the knowledge of flutter schema so a new serialized model
     * is created and schema is populated from local representation.
     * @param serverData data deserialized from server conflict unhandled error response.
     * @param local local representation of the conflicted model.
     * @param <T> Type of the model
     * @return Model
     */
    @SuppressWarnings("unchecked")
    private <T extends Model> T getServerModel(@NonNull ModelWithMetadata<T> serverData, T local) {
        T serverModel = serverData.getModel();
        if (serverModel instanceof SerializedModel) {
            SerializedModel serverSerializedModel = (SerializedModel) serverModel;
            return (T) SerializedModel.builder()
                    .modelSchema(((SerializedModel) local).getModelSchema())
                    .serializedData(serverSerializedModel.getSerializedData())
                    .build();
        } else {
            return serverModel;
        }
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
                //SchemaRegistry.instance().getModelSchemaForModelClass method supports schema generation for flutter
                //models.
                final ModelSchema schema = SchemaRegistry.instance().getModelSchemaForModelClass(model.getModelName());
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
