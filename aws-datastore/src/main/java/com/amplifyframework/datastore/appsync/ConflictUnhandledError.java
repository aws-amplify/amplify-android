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

package com.amplifyframework.datastore.appsync;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.util.Empty;
import com.amplifyframework.util.GsonFactory;
import com.amplifyframework.util.TypeMaker;

import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;

/**
 * It is possible for the client and server to have two copies of one logical entity,
 * each containing different contents, but each believing that it is the latest
 * copy of the data. When the client tries to mutate the server's data, AppSync's response
 * may contain a GraphQLError with errorType as ConflictUnhandled.
 *
 * This ConflictUnhandledError class models that response data.
 *
 * @param <T> Type of model for which a conflict occurred between client & server versions.
 * @see <a href="https://docs.aws.amazon.com/appsync/latest/devguide/conflict-detection-and-sync.html#errors">
 *     AppSync Conflict Detection & Sync Errors
 *     </a>
 */
public final class ConflictUnhandledError<T extends Model> {
    private final ModelWithMetadata<T> serverVersion;

    private ConflictUnhandledError(ModelWithMetadata<T> serverVersion) {
        this.serverVersion = serverVersion;
    }

    /**
     * Iterates over a list of GraphQL.Error, checking to see if any of them
     * contain 'ConflictUnhandled' as the errorType.
     * @param typeOfConflictingData The class of the model experiencing conflict
     * @param errors A list of GraphQL errors, as received in response to a mutation
     * @param <T> The type of model experiencing conflict, if applicable
     * @return A model of the first ConflictUnhandled error found in the GraphQL error list.
     *         If there is no ConflictUnhandledError in the list, returns null.
     */
    @Nullable
    public static <T extends Model> ConflictUnhandledError<T> findFirst(
            @NonNull Class<T> typeOfConflictingData,
            @Nullable List<GraphQLResponse.Error> errors) {
        if (Empty.check(errors)) {
            return null;
        }

        Gson gson = GsonFactory.instance();

        for (GraphQLResponse.Error error : errors) {
            if (Empty.check(error.getExtensions())) {
                continue;
            }

            AppSyncExtensions appSyncExtensions = new AppSyncExtensions(error.getExtensions());
            AppSyncErrorType errorType = AppSyncErrorType.fromErrorType(appSyncExtensions.getErrorType());
            if (!AppSyncErrorType.CONFLICT_UNHANDLED.equals(errorType)) {
                continue;
            }

            Type type = TypeMaker.getParameterizedType(ModelWithMetadata.class, typeOfConflictingData);
            String serverVersionJson = gson.toJson(appSyncExtensions.getData());
            ModelWithMetadata<T> modelWithMetadata = gson.fromJson(serverVersionJson, type);
            return new ConflictUnhandledError<>(Objects.requireNonNull(modelWithMetadata));
        }

        return null;
    }

    /**
     * Access the version of the data that is currently living on the server.
     * @return Server's version of the data
     */
    public ModelWithMetadata<T> getServerVersion() {
        return serverVersion;
    }

    @Override
    public boolean equals(@Nullable Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        ConflictUnhandledError<?> that = (ConflictUnhandledError<?>) thatObject;

        return getServerVersion().equals(that.getServerVersion());
    }

    @Override
    public int hashCode() {
        return getServerVersion().hashCode();
    }

    @Override
    public String toString() {
        return "ConflictUnhandledError{" +
            "serverVersion=" + serverVersion +
            '}';
    }
}
