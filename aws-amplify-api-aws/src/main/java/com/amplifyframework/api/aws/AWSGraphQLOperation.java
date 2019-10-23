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

import android.util.Log;
import androidx.annotation.NonNull;

import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.ApiOperation;
import com.amplifyframework.api.graphql.GraphQLQuery;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.core.async.Listener;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * An operation to enqueue a GraphQL query to OkHttp client.
 * @param <T> Casted type of GraphQL query result
 */
public final class AWSGraphQLOperation<T> extends ApiOperation<T, GraphQLResponse<T>> {
    private final String endpoint;
    private final OkHttpClient client;
    private final GraphQLQuery query;

    private Call ongoingCall;

    /**
     * Constructs a new AWSGraphQLOperation.
     * @param endpoint API endpoint being hit
     * @param client OkHttp client being used to hit the endpoint
     * @param query GraphQL query being queried
     * @param responseFactory an implementation of GsonResponseFactory
     * @param classToCast class to cast the response to
     * @param callback local callback listener to be invoked
     */
    AWSGraphQLOperation(String endpoint,
                        OkHttpClient client,
                        GraphQLQuery query,
                        GsonResponseFactory responseFactory,
                        Class<T> classToCast,
                        Listener<GraphQLResponse<T>> callback) {
        super(responseFactory, classToCast, callback);
        this.endpoint = endpoint;
        this.client = client;
        this.query = query;
    }

    @Override
    public void start() {
        // No-op if start() is called post-execution
        if (ongoingCall != null && ongoingCall.isExecuted()) {
            return;
        }

        try {
            Log.d("graphql", query.getContent());
            ongoingCall = client.newCall(new Request.Builder()
                    .url(endpoint)
                    .addHeader("accept", "application/json")
                    .addHeader("content-type", "application/json")
                    .post(RequestBody.create(query.getContent(), MediaType.parse("application/json")))
                    .build());
            ongoingCall.enqueue(new OkHttpCallback());
        } catch (Exception error) {
            // Cancel if possible
            ongoingCall.cancel();

            // Let locally registered callback deal with this error
            // and throw runtime exception otherwise.
            ApiException wrappedError =
                    new ApiException("OkHttp client failed to make a successful request.", error);
            if (hasCallback()) {
                callback().onError(wrappedError);
            } else {
                throw wrappedError;
            }
        }
    }

    @Override
    public void cancel() {
        ongoingCall.cancel();
    }

    class OkHttpCallback implements Callback {
        @Override
        public void onResponse(@NonNull Call call,
                               @NonNull Response response) throws IOException {
            final ResponseBody responseBody = response.body();
            String jsonResponse = null;
            if (responseBody != null) {
                jsonResponse = responseBody.string();
            }

            GraphQLResponse<T> wrappedResponse = wrapResponse(jsonResponse);

            Log.d("graphql", jsonResponse);

            if (hasCallback()) {
                callback().onResult(wrappedResponse);
            }
            //TODO: Dispatch to hub
        }

        @Override
        public void onFailure(@NonNull Call call,
                              @NonNull IOException ioe) {
            if (hasCallback()) {
                callback().onError(ioe);
            }
            //TODO: Dispatch to hub
        }
    }
}
