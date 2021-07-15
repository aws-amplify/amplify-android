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

import android.net.Uri;
import androidx.annotation.NonNull;

import com.amplifyframework.api.ApiException.ApiAuthException;
import com.amplifyframework.api.aws.sigv4.AppSyncV4Signer;

import com.amazonaws.DefaultRequest;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.http.HttpMethodName;
import com.amazonaws.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;

/**
 * Request decorator implementatioon that uses AWS SigV4 signing.
 */
public class IamRequestDecorator implements RequestDecorator {
    private static final String CONTENT_TYPE = "application/json";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse(CONTENT_TYPE);
    private static final String APP_SYNC_SERVICE_NAME = "appsync";
    private final AWSCredentialsProvider credentialsProvider;
    private final AppSyncV4Signer v4Signer;

    /**
     * Constructor that takes in the necessary dependencies used to sign the requests.
     * @param v4Signer An instance of the {@link AppSyncV4Signer}.
     * @param credentialsProvider The AWS credentials provider to use when retrieving AWS credentials.
     */
    public IamRequestDecorator(AppSyncV4Signer v4Signer, AWSCredentialsProvider credentialsProvider) {
        this.v4Signer = v4Signer;
        this.credentialsProvider = credentialsProvider;
    }

    /**
     * Adds the appropriate header to the provided HTTP request.
     * @param req The request to be signed.
     * @return A new instance of the request containing the signature headers.
     * @throws ApiAuthException If the signing process fails.
     */
    public final okhttp3.Request decorate(okhttp3.Request req) throws ApiAuthException {
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
        boolean isEmptyRequestBody = false;
        try {
            if (body != null) {
                //write the body to a byte array.
                final Buffer buffer = new Buffer();
                body.writeTo(buffer);
                bodyBytes = IOUtils.toByteArray(buffer.inputStream());
            } else {
                isEmptyRequestBody = true;
                bodyBytes = "".getBytes();
            }
            dr.setParameters(splitQuery(req.url().url()));
        } catch (IOException exception) {
            throw new ApiAuthException("Unable to calculate SigV4 signature for the request",
                                                    exception,
                                                    "Check your application logs for details.");
        }
        dr.setContent(new ByteArrayInputStream(bodyBytes));

        //set the query string parameters
        v4Signer.sign(dr, credentialsProvider.getCredentials());

        //Copy the signed/credentialed request back into an OKHTTP Request object.
        okhttp3.Request.Builder okReqBuilder = new okhttp3.Request.Builder();

        //set the headers from default request, since it contains the signed headers as well.
        for (Map.Entry<String, String> e : dr.getHeaders().entrySet()) {
            okReqBuilder.addHeader(e.getKey(), e.getValue());
        }

        //Set the URL and Method
        okReqBuilder.url(req.url());
        final RequestBody requestBody;
        if (!isEmptyRequestBody) {
            requestBody = RequestBody.create(bodyBytes, JSON_MEDIA_TYPE);
        } else {
            requestBody = null;
        }

        okReqBuilder.method(req.method(), requestBody);

        //continue with chain.
        return okReqBuilder.build();
    }

    // Extracts query string parameters from a URL.
    @NonNull
    private static Map<String, String> splitQuery(URL url) throws IOException {
        Map<String, String> queryPairs = new LinkedHashMap<>();
        Uri uri = Uri.parse(url.toString());
        for (String paramName : uri.getQueryParameterNames()) {
            queryPairs.put(paramName, URLDecoder.decode(uri.getQueryParameter(paramName), "UTF8"));
        }
        return queryPairs;
    }
}
