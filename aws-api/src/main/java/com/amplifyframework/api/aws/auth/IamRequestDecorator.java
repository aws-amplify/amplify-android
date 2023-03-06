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

package com.amplifyframework.api.aws.auth;

import com.amplifyframework.api.ApiException.ApiAuthException;
import com.amplifyframework.api.aws.sigv4.AWS4Signer;

import aws.smithy.kotlin.runtime.net.Url;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider;
import aws.smithy.kotlin.runtime.http.DeferredHeaders;
import aws.smithy.kotlin.runtime.http.Headers;
import aws.smithy.kotlin.runtime.http.HttpMethod;
import aws.smithy.kotlin.runtime.http.content.ByteArrayContent;
import aws.smithy.kotlin.runtime.http.request.HttpRequest;
import aws.smithy.kotlin.runtime.http.request.HttpRequestKt;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;

/**
 * Request decorator implementatioon that uses AWS SigV4 signing.
 */
public class IamRequestDecorator implements RequestDecorator {
    private static final String CONTENT_TYPE = "application/json";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse(CONTENT_TYPE);
    private final CredentialsProvider credentialsProvider;
    private final AWS4Signer v4Signer;
    private final String serviceName;

    /**
     * Constructor that takes in the necessary dependencies used to sign the requests.
     *
     * @param signer              Signer used to sign the request.
     * @param credentialsProvider The AWS credentials provider to use when retrieving AWS credentials.
     * @param serviceName         the name of the AWS service for which the request is being decorated.
     */
    public IamRequestDecorator(AWS4Signer signer, CredentialsProvider credentialsProvider, String serviceName) {
        this.v4Signer = signer;
        this.credentialsProvider = credentialsProvider;
        this.serviceName = serviceName;
    }

    /**
     * Adds the appropriate header to the provided HTTP request.
     *
     * @param req The request to be signed.
     * @return A new instance of the request containing the signature headers.
     * @throws ApiAuthException If the signing process fails.
     */
    public final okhttp3.Request decorate(okhttp3.Request req) throws ApiAuthException {
        //set the request body
        final byte[] bodyBytes = getBytes(req.body());
        ByteArrayContent body2 = new ByteArrayContent(bodyBytes);

        HttpMethod method = HttpMethod.Companion.parse(req.method());
        Url url = Url.Companion.parse(req.url().uri().toString());
        Headers headers = Headers.Companion.invoke((builder) -> {
            for (String headerName : req.headers().names()) {
                builder.set(headerName, req.header(headerName));
            }

            builder.set("Host", req.url().url().getHost());
            return null;
        });

        HttpRequest req2 = HttpRequestKt.HttpRequest(method, url, headers, body2, DeferredHeaders.Companion.getEmpty());

        HttpRequest request = v4Signer.signBlocking(req2, credentialsProvider, serviceName).getOutput();

        //Copy the signed/credentialed request back into an OKHTTP Request object.
        okhttp3.Request.Builder okReqBuilder = new okhttp3.Request.Builder();

        for (Map.Entry<String, List<String>> e : request.getHeaders().entries()) {
            okReqBuilder.addHeader(e.getKey(), e.getValue().get(0));
        }

        //Set the URL and Method
        okReqBuilder.url(req.url());
        final RequestBody requestBody;
        if (req.body() == null) {
            requestBody = null;
        } else {
            requestBody = RequestBody.create(bodyBytes, JSON_MEDIA_TYPE);
        }

        okReqBuilder.method(req.method(), requestBody);

        //continue with chain.
        return okReqBuilder.build();
    }

    private byte[] getBytes(RequestBody body) throws ApiAuthException {
        if (body == null) {
            return "".getBytes();
        }

        final int BUFFER_SIZE = 1024 * 4;
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            //write the body to a byte array.
            final Buffer buffer = new Buffer();
            body.writeTo(buffer);

            final byte[] bytes = new byte[BUFFER_SIZE];
            int n = 0;
            while ((n = buffer.inputStream().read(bytes)) != -1) {
                output.write(bytes, 0, n);
            }
            return output.toByteArray();
        } catch (IOException exception) {
            throw new ApiAuthException("Unable to calculate SigV4 signature for the request",
                exception,
                "Check your application logs for details.");
        }
    }
}
