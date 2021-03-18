/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.api.aws.auth;

import androidx.annotation.NonNull;

import com.amplifyframework.util.Empty;

import com.amazonaws.DefaultRequest;
import com.amazonaws.http.HttpMethodName;
import com.amazonaws.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okio.Buffer;

/**
 * Abstract class that defines some basic functionality with regards to signing AppSync requests.
 * Implementations of this class should implement the {@link ApiRequestSigner#addAuthHeader(com.amazonaws.Request)}
 * method.
 */
public abstract class ApiRequestSigner {
    private static final String CONTENT_TYPE = "application/json";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse(CONTENT_TYPE);
    private static final String APP_SYNC_SERVICE_NAME = "appsync";
    private static final String API_GATEWAY_SERVICE_NAME = "apigateway";

    abstract void addAuthHeader(com.amazonaws.Request<?> request);

    /**
     * Adds the appropriate headers to the provided HTTP request.
     * @param req The request to be signed.
     * @return A new instance of the request containing the signature headers.
     * @throws IOException If the signing process fails.
     */
    public final Request sign(Request req) throws IOException {
        //Clone the request into a new DefaultRequest object and populate it with credentials
        final DefaultRequest<?> dr = new DefaultRequest<>(APP_SYNC_SERVICE_NAME);
        //set the endpoint
        dr.setEndpoint(req.url().uri());
        //copy all the headers
        for (String headerName : req.headers().names()) {
            dr.addHeader(headerName, req.header(headerName));
        }
        //set the http method
        dr.setHttpMethod(HttpMethodName.valueOf(req.method()));

        //set the request body
        final byte[] bodyBytes;
        RequestBody body = req.body();
        if (body != null) {
            //write the body to a byte array.
            final Buffer buffer = new Buffer();
            body.writeTo(buffer);
            bodyBytes = IOUtils.toByteArray(buffer.inputStream());
        } else {
            bodyBytes = "".getBytes();
        }
        dr.setContent(new ByteArrayInputStream(bodyBytes));

        //set the query string parameters
        dr.setParameters(splitQuery(req.url().url()));

        addAuthHeader(dr);

        //Copy the signed/credentialed request back into an OKHTTP Request object.
        Request.Builder okReqBuilder = new Request.Builder();

        //set the headers from default request, since it contains the signed headers as well.
        for (Map.Entry<String, String> e : dr.getHeaders().entrySet()) {
            okReqBuilder.addHeader(e.getKey(), e.getValue());
        }

        //Set the URL and Method
        okReqBuilder.url(req.url());
        final RequestBody requestBody;
        if (req.body() != null && req.body().contentLength() > 0) {
            requestBody = RequestBody.create(bodyBytes, JSON_MEDIA_TYPE);
        } else {
            requestBody = null;
        }

        okReqBuilder.method(req.method(), requestBody);

        //continue with chain.
        return okReqBuilder.build();
    }

    // Extracts query string parameters from a URL.
    // Source: https://stackoverflow.com/questions/13592236/parse-a-uri-string-into-name-value-collection
    @NonNull
    private static Map<String, String> splitQuery(URL url) throws IOException {
        Map<String, String> queryPairs = new LinkedHashMap<>();
        String query = url.getQuery();
        if (Empty.check(query)) {
            return Collections.emptyMap();
        }
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int index = pair.indexOf("=");
            if (index < 0) {
                throw new MalformedURLException("URL query parameters are malformed.");
            }
            queryPairs.put(
                URLDecoder.decode(pair.substring(0, index), "UTF-8"),
                URLDecoder.decode(pair.substring(index + 1), "UTF-8")
            );
        }
        return queryPairs;
    }

}
