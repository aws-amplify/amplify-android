/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.api.aws.utils;

import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.api.rest.HttpMethod;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Objects;

import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * Factory to create {@link URL}s and OkHttp {@link Request}s for REST-ful behaviors.
 */
public final class RestRequestFactory {
    private RestRequestFactory() {}

    /**
     * Create an URL by appending the path and queries.
     * Throws exception if the url is malformed.
     * @param endpoint        A valid endpoint
     * @param path            path to be appended.
     * @param queryParameters query parameters
     * @return Valid URL
     * @throws MalformedURLException Throw when the exception is not valid
     */
    @NonNull
    public static URL createURL(
            @NonNull String endpoint,
            @Nullable String path,
            @Nullable Map<String, String> queryParameters
    ) throws MalformedURLException {

        URL url = new URL(endpoint);
        HttpUrl.Builder builder = new HttpUrl.Builder()
            .scheme(url.getProtocol())
            .host(url.getHost())
            .addPathSegment(stripLeadingSlashes(url.getPath()));

        if (url.getPort() != -1) {
            builder.port(url.getPort());
        }

        if (path != null) {
            builder.addPathSegments(stripLeadingSlashes(path));
        }

        if (queryParameters != null) {
            for (Map.Entry<String, String> entry : queryParameters.entrySet()) {
                builder.addQueryParameter(entry.getKey(), entry.getValue());
            }
        }

        try {
            String encodedUrl = builder.build().url().toString();
            return new URL(Uri.decode(encodedUrl));
        } catch (MalformedURLException error) {
            throw new MalformedURLException(error.getMessage());
        }
    }

    /**
     * Constructs the ok http request.
     * @param url         URL endpoint to make the request
     * @param requestData Data for the request
     * @param headers     Header map for th request
     * @param type        Rest operation type
     * @return Returns the request
     */
    @NonNull
    public static Request createRequest(
            @NonNull URL url,
            @Nullable byte[] requestData,
            @Nullable Map<String, String> headers,
            @NonNull HttpMethod type) {
        Objects.requireNonNull(url);
        Objects.requireNonNull(type);
        Request.Builder requestBuilder = new Request.Builder()
                .url(url);
        switch (type) {
            case GET:
                requestBuilder.get();
                break;
            case PUT:
                populateBody(
                        requestBuilder,
                        requestData, (builder, data) -> builder.put(RequestBody.create(data))
                );
                break;
            case POST:
                populateBody(
                        requestBuilder,
                        requestData, (builder, data) -> builder.post(RequestBody.create(data))
                );
                break;
            case HEAD:
                requestBuilder.head();
                break;
            case PATCH:
                populateBody(
                        requestBuilder,
                        requestData, (builder, data) -> builder.patch(RequestBody.create(data))
                );
                break;
            case DELETE:
                if (requestData != null) {
                    populateBody(
                            requestBuilder,
                            requestData, (builder, data) -> builder.delete(RequestBody.create(data))
                    );
                } else {
                    requestBuilder.delete();
                }
                break;
            default:
                break;
        }
        if (headers != null) {
            requestBuilder.headers(Headers.of(headers));
        }
        return requestBuilder.build();
    }

    private static void populateBody(
            Request.Builder builder,
            byte[] data,
            BodyCreationStrategy strategy
    ) {
        if (data != null) {
            strategy.buildRequest(builder, data);
        }
    }

    // Segment separator can be either '/' or '\'.
    // HttpUrl.Builder assumes an empty URL if path segments
    // begin with either character. Strip them before appending.
    private static String stripLeadingSlashes(final String path) {
        return path.replaceAll("^[\\\\/]+", "");
    }

    /**
     * A strategy to add data to a request.
     */
    interface BodyCreationStrategy {
        void buildRequest(Request.Builder builder, byte[] data);
    }
}
