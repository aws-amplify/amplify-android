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

import com.amplifyframework.api.graphql.GraphQLLocation;
import com.amplifyframework.api.graphql.GraphQLPathSegment;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.util.GsonFactory;
import com.amplifyframework.util.GsonObjectConverter;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A test utility to help construct AppSyncConflictUnhandledError objects.
 */
public final class AppSyncConflictUnhandledErrorFactory {
    private AppSyncConflictUnhandledErrorFactory() {}

    /**
     * Creates an {@link AppSyncConflictUnhandledError}, given a {@link ModelWithMetadata}
     * with representative server data.
     * @param serverData Server data to include in the app sync error
     * @param <T> Type of model that is experiencing conflict
     * @return A non-null AppSyncConflictUnhandledError
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public static <T extends Model> AppSyncConflictUnhandledError<T> createUnhandledConflictError(
            @NonNull ModelWithMetadata<T> serverData) {
        Objects.requireNonNull(serverData);

        // Get the MWM as an object map
        Gson gson = GsonFactory.instance();
        JsonObject jsonObject = gson.fromJson(gson.toJson(serverData), JsonObject.class);
        Map<String, Object> serverModelData = GsonObjectConverter.toMap(jsonObject);

        // The model class is needed to get the simple name, and to pass to the
        // findFirst() utility method.
        Class<T> modelClass = (Class<T>) serverData.getModel().getClass();

        // When AppSync receives that update, have it respond
        // with a ConflictUnhandledError.
        String message = "Conflict resolver rejects mutation.";

        String pathSegmentText = "update" + modelClass.getSimpleName();
        List<GraphQLPathSegment> paths = Collections.singletonList(new GraphQLPathSegment(pathSegmentText));
        List<GraphQLLocation> locations = Collections.singletonList(new GraphQLLocation(2, 3));

        Map<String, Object> extensions = new HashMap<>();
        extensions.put("errorType", "ConflictUnhandled");
        extensions.put("data", serverModelData);

        GraphQLResponse.Error error = new GraphQLResponse.Error(message, locations, paths, extensions);
        AppSyncConflictUnhandledError<T> conflictUnhandledError =
            AppSyncConflictUnhandledError.findFirst(modelClass, Collections.singletonList(error));
        return Objects.requireNonNull(conflictUnhandledError);
    }
}
