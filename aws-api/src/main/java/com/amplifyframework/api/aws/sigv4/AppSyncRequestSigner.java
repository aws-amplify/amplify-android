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

package com.amplifyframework.api.aws.sigv4;

import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.aws.ApiAuthProviders;
import com.amplifyframework.api.aws.AuthorizationType;
import com.amplifyframework.util.Empty;

import com.amazonaws.DefaultRequest;
import com.amazonaws.auth.AWSCredentials;
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
 * HTTP request signer for AppSync that signs the request using the auth mode set in the constructor.
 */
public final class AppSyncRequestSigner implements AWSRequestSigner {
    private static final String CONTENT_TYPE = "application/json";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse(CONTENT_TYPE);
    private static final String APP_SYNC_SERVICE_NAME = "appsync";
    private static final String X_API_KEY = "x-api-key";
    private static final String AUTHORIZATION = "authorization";

    private final ApiAuthProviders providers;
    private final String awsRegion;
    private final String apiKey;

    /**
     * Constructor that allows consumers to pass the desired auth mode along with
     * other necessary parameters needed to sign HTTP requests to be sent to AppSync.
     * @param providers The provider types that produce the token to be added to the request.
     * @param apiKey The API key to be used when signing requests.
     * @param awsRegion The region where the AppSync API resides.
     */
    public AppSyncRequestSigner(ApiAuthProviders providers,
                                String apiKey, //HACK: Each API can have a different key
                                String awsRegion) {
        this.apiKey = apiKey;
        this.providers = providers;
        this.awsRegion = awsRegion;
    }

    @Override
    public Request sign(Request req, AuthorizationType authMode) throws ApiException {
        //Clone the request into a new DefaultRequest object and populate it with credentials
        final DefaultRequest<?> dr;
        dr = new DefaultRequest<>(APP_SYNC_SERVICE_NAME);
        //set the endpoint
        dr.setEndpoint(req.url().uri());
        //copy all the headers
        for (String headerName : req.headers().names()) {
            dr.addHeader(headerName, req.header(headerName));
        }
        //set the http method
        dr.setHttpMethod(HttpMethodName.valueOf(req.method()));

        //set the request body
        final byte[] bodyBytes = getBodyBytes(req);
        dr.setContent(new ByteArrayInputStream(bodyBytes));

        //set the query string parameters
        dr.setParameters(splitQuery(req.url().url()));

        //Sign or Decorate request with the required headers
        if (AuthorizationType.AWS_IAM.equals(authMode) && providers.getAWSCredentialsProvider() != null) {
            AWSCredentials credentials = providers.getAWSCredentialsProvider().getCredentials();
            new AppSyncV4Signer(this.awsRegion).sign(dr, credentials);
        } else if (AuthorizationType.API_KEY.equals(authMode) && apiKey != null) {
            dr.addHeader(X_API_KEY, apiKey);
        } else if (AuthorizationType.AMAZON_COGNITO_USER_POOLS.equals(authMode) &&
                    providers.getCognitoUserPoolsAuthProvider() != null) {
            dr.addHeader(AUTHORIZATION, providers.getCognitoUserPoolsAuthProvider().getLatestAuthToken());
        } else if (AuthorizationType.OPENID_CONNECT.equals(authMode) && providers.getOidcAuthProvider() != null) {
            dr.addHeader(AUTHORIZATION, providers.getOidcAuthProvider().getLatestAuthToken());
        } else {
            throw new ApiException("No provider for the requested auth mode:" + authMode.name(), "TODO");
        }
        //Copy the signed/credentialed request back into an OKHTTP Request object.
        Request.Builder okReqBuilder = new Request.Builder();
        //set the headers from default request, since it contains the signed headers as well.
        for (Map.Entry<String, String> e : dr.getHeaders().entrySet()) {
            okReqBuilder.addHeader(e.getKey(), e.getValue());
        }
        //Set the URL and Method
        okReqBuilder.url(req.url());
        final RequestBody requestBody;
        if (bodyBytes != null && bodyBytes.length > 0) {
            requestBody = RequestBody.create(bodyBytes, JSON_MEDIA_TYPE);
        } else {
            requestBody = null;
        }
        okReqBuilder.method(req.method(), requestBody);
        return okReqBuilder.build();
    }

    private static byte[] getBodyBytes(Request req) throws ApiException {
        final byte[] bodyBytes;
        RequestBody body = req.body();
        if (body != null) {
            //write the body to a byte array.
            final Buffer buffer = new Buffer();
            try {
                body.writeTo(buffer);
                bodyBytes = IOUtils.toByteArray(buffer.inputStream());
            } catch (IOException ioException) {
                //TODO: suggestions
                throw new ApiException("Unable to build HTTP request body.", ioException, "");
            }
        } else {
            bodyBytes = "".getBytes();
        }
        return bodyBytes;
    }

    private static Map<String, String> splitQuery(URL url) throws ApiException {
        Map<String, String> queryPairs = new LinkedHashMap<>();
        String query = url.getQuery();
        if (Empty.check(query)) {
            return Collections.emptyMap();
        }
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int index = pair.indexOf("=");
            if (index < 0) {
                throw new ApiException("Failed to construct the request URL.",
                                       new MalformedURLException("URL query parameters are malformed."),
                                       "TODO");
            }
            try {
                queryPairs.put(
                    URLDecoder.decode(pair.substring(0, index), "UTF-8"),
                    URLDecoder.decode(pair.substring(index + 1), "UTF-8")
                );
            } catch (IOException ioException) {
                //TODO: Suggestion
                throw new ApiException("Failed to construct the request URL.",
                                       ioException,
                                       "");
            }
        }
        return queryPairs;
    }
}
