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

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.datastore.DataStoreConfigurationProvider;
import com.amplifyframework.datastore.DataStoreErrorHandler;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.appsync.AppSyncExtensions;
import com.amplifyframework.datastore.appsync.ModelWithMetadata;
import com.amplifyframework.logging.Logger;
import com.amplifyframework.util.Empty;
import com.amplifyframework.util.GsonFactory;
import com.amplifyframework.util.TypeMaker;

import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

final class MutationErrorHandler {
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-datastore");
    private final DataStoreConfigurationProvider configurationProvider;
    private final MutationOutbox mutationOutbox;

    MutationErrorHandler(DataStoreConfigurationProvider configurationProvider, MutationOutbox mutationOutbox) {
        this.configurationProvider = configurationProvider;
        this.mutationOutbox = mutationOutbox;
    }

    @NonNull
    <T extends Model> Single<ModelWithMetadata<T>> onError(
            @NonNull PendingMutation<T> pendingMutation,
            @NonNull List<GraphQLResponse.Error> errors
    ) {
        final DataStoreErrorHandler errorHandler;
        try {
            errorHandler = configurationProvider.getConfiguration().getErrorHandler();
        } catch (DataStoreException badConfigurationProvider) {
            return Single.error(badConfigurationProvider);
        }

        return Completable.fromAction(() -> errorHandler.accept(firstError(pendingMutation, errors)))
                .doOnError(error -> LOG.warn("Failed to execute errorHandler. ", error))
                .andThen(mutationOutbox.remove(pendingMutation.getMutationId()))
                .andThen(Single.error(new DataStoreException(
                        "Mutation failed. Failed mutation = " + pendingMutation + ". " +
                                "AppSync response contained errors = " + errors,
                        "Verify that your AppSync endpoint is able to store " +
                                pendingMutation.getMutatedItem() + " models."
                )));
    }

    private <T extends Model> DataStoreErrorHandler.SyncError<T> firstError(
            PendingMutation<T> pendingMutation,
            List<GraphQLResponse.Error> errors
    ) throws DataStoreException {
        for (GraphQLResponse.Error error : errors) {
            T local = pendingMutation.getMutatedItem();
            T remote = parseRemote(error, pendingMutation.getClassOfMutatedItem());
            return new DataStoreErrorHandler.SyncError<>(error, local, remote);
        }
        throw new DataStoreException("Server response did not contain any error.",
                AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION);
    }

    private <T extends Model> T parseRemote(
            GraphQLResponse.Error error,
            Class<T> modelClazz
    ) throws DataStoreException {
        if (Empty.check(error.getExtensions())) {
            return null;
        }
        AppSyncExtensions appSyncExtensions = new AppSyncExtensions(error.getExtensions());
        if (appSyncExtensions.getData() == null) {
            return null;
        }

        try {
            Gson gson = GsonFactory.instance();
            Type type = TypeMaker.getParameterizedType(ModelWithMetadata.class, modelClazz);
            String serverVersionJson = gson.toJson(appSyncExtensions.getData());
            ModelWithMetadata<T> modelWithMetadata = gson.fromJson(serverVersionJson, type);
            return modelWithMetadata.getModel();
        } catch (Exception parsingError) {
            throw new DataStoreException(
                    "Failed to parse remote model from error.", parsingError,
                    AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION
            );
        }
    }
}
