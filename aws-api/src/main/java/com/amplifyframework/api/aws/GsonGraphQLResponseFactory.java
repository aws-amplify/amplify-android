/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.api.aws;

import androidx.annotation.VisibleForTesting;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.graphql.GraphQLRequest;
import com.amplifyframework.api.graphql.GraphQLResponse;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.lang.reflect.Type;

/**
 * Converts JSON strings into models of a given type, using Gson.
 */
final class GsonGraphQLResponseFactory implements GraphQLResponse.Factory {
    private final Gson gson;

    GsonGraphQLResponseFactory() {
        this(GsonFactory.create());
    }

    @VisibleForTesting
    GsonGraphQLResponseFactory(Gson gson) {
        this.gson = gson;
    }

    @Override
    public <R> GraphQLResponse<R> buildResponse(GraphQLRequest<R> request, String responseJson, Type typeOfR
    ) throws ApiException {
        Type responseType = TypeMaker.getParameterizedType(GraphQLResponse.class, typeOfR);
        try {
            Gson responseGson = gson.newBuilder()
                    .registerTypeHierarchyAdapter(Iterable.class, new IterableDeserializer<>(request))
                    .create();
            return responseGson.fromJson(responseJson, responseType);
        } catch (JsonSyntaxException jsonSyntaxException) {
            throw new ApiException(
                    "Amplify encountered an error while deserializing an object.",
                    jsonSyntaxException,
                    AmplifyException.TODO_RECOVERY_SUGGESTION
            );
        }
    }
}
