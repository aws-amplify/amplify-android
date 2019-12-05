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

package com.amplifyframework.api.aws.utils;

import com.amplifyframework.api.rest.HttpMethod;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * Util class to handle Rest url request.
 */
public final class RestOperationRequestUtils {

    private static final String CONTENT_TYPE = "application/json";

    /**
     * Private constructor since this is a utility class.
     */
    private RestOperationRequestUtils() {
        //not called
    }

    /**
     * Create an URL by appending the path and queries.
     * <p>
     * Throws exception if the url is malformed.
     *
     * @param endpoint        A valid endpoint
     * @param path            path to be appended.
     * @param queryParameters query parameters
     * @return Valid URL
     * @throws MalformedURLException Throw when the exception is not valid
     */
    public static URL constructURL(String endpoint,
                                   String path,
                                   Map<String, String> queryParameters)
            throws MalformedURLException {
        String urlPath = "";
        if (path != null) {
            urlPath = path;
        }
        URL url = new URL(endpoint);
        HttpUrl.Builder builder = new HttpUrl.Builder()
                .scheme(url.getProtocol())
                .host(url.getHost())
                .addPathSegment(url.getPath().replaceFirst("/", ""))
                .addPathSegment(urlPath);

        if (queryParameters != null) {
            for (Map.Entry<String, String> entry : queryParameters.entrySet()) {
                builder.addQueryParameter(entry.getKey(), entry.getValue());
            }
        }
        return builder.build().url();
    }

    /**
     * Constructs the ok http request.
     *
     * @param url         URL endpoint to make the request
     * @param requestData Data for the request
     * @param headers     Header map for th request
     * @param type        Rest operation type
     * @return Returns the request
     */
    public static Request constructOKHTTPRequest(final URL url,
                                                 final byte[] requestData,
                                                 final Map<String, String> headers,
                                                 final HttpMethod type) {
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
                requestBuilder.delete();
                break;
            default:
                break;
        }
        if (headers != null) {
            requestBuilder.headers(Headers.of(headers));
        }
        return requestBuilder.build();
    }

    private static void populateBody(final Request.Builder builder,
                                     final byte[] data,
                                     BodyCreationStrategy strategy) {
        if (data != null) {
            strategy.buildRequest(builder, data);
        }
    }

    /**
     * A strategy to add data to a request.
     */
    interface BodyCreationStrategy {
        void buildRequest(Request.Builder builder, byte[] data);
    }
}
