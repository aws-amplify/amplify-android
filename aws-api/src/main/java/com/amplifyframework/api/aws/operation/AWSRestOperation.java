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

package com.amplifyframework.api.aws.operation;

import androidx.annotation.NonNull;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.aws.utils.RestOperationRequestUtils;
import com.amplifyframework.api.rest.RestOperation;
import com.amplifyframework.api.rest.RestOperationRequest;
import com.amplifyframework.api.rest.RestResponse;
import com.amplifyframework.core.ResultListener;

import java.io.IOException;
import java.net.URL;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * An operation to enqueue a REST HTTP request to OkHttp client.
 */
public final class AWSRestOperation extends RestOperation {

    private final String endpoint;
    private final OkHttpClient client;
    private final ResultListener<RestResponse> responseListener;

    private Call ongoingCall;

    /**
     * Constructs a REST operation.
     * @param request REST request that contains the query and data.
     * @param endpoint Endpoint against which the request to be made.
     * @param client OKHTTPClient to be used for the request.
     * @param responseListener Callback listener.
     */
    public AWSRestOperation(
            RestOperationRequest request,
            String endpoint,
            OkHttpClient client,
            ResultListener<RestResponse> responseListener) {
        super(request);
        this.endpoint = endpoint;
        this.client = client;
        this.responseListener = responseListener;
    }

    @Override
    public void start() {
        // No-op if start() is called post-execution
        if (ongoingCall != null && ongoingCall.isExecuted()) {
            return;
        }
        try {
            URL url = RestOperationRequestUtils.constructURL(endpoint,
                    getRequest().getPath(),
                    getRequest().getQueryParameters());
            Request request = RestOperationRequestUtils.constructOKHTTPRequest(url,
                    getRequest().getData(),
                    getRequest().getHeaders(),
                    getRequest().getHttpMethod());
            ongoingCall = client.newCall(request);
            ongoingCall.enqueue(new AWSRestOperation.OkHttpCallback());
        } catch (Exception error) {
            // Cancel if possible
            if (ongoingCall != null) {
                ongoingCall.cancel();
            }

            ApiException wrappedError =
                    new ApiException("OkHttp client failed to make a successful request.",
                            error,
                            AmplifyException.TODO_RECOVERY_SUGGESTION);
            if (responseListener != null) {
                responseListener.onError(wrappedError);
            }
        }
    }

    @Override
    public synchronized void cancel() {
        if (ongoingCall != null) {
            ongoingCall.cancel();
        }
    }

    class OkHttpCallback implements Callback {
        @Override
        public void onResponse(@NonNull Call call,
                               @NonNull Response response) throws IOException {
            final ResponseBody responseBody = response.body();
            final int statusCode = response.code();

            RestResponse restResponse;
            if (responseBody != null) {
                final byte[] data = responseBody.bytes();
                restResponse = new RestResponse(statusCode, data);
            } else {
                restResponse = new RestResponse(statusCode);
            }

            if (responseListener != null) {
                responseListener.onResult(restResponse);
            }
            //TODO: Dispatch to hub
        }

        @Override
        public void onFailure(@NonNull Call call,
                              @NonNull IOException ioe) {
            ApiException wrappedError =
                    new ApiException("Received an IO exception while making the request.",
                            ioe,
                            "Retry the request.");
            if (responseListener != null) {
                responseListener.onError(wrappedError);
            }
            //TODO: Dispatch to hub
        }
    }

}
