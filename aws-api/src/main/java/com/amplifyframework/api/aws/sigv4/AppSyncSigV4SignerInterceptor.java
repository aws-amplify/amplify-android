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

import androidx.annotation.NonNull;

import com.amplifyframework.api.aws.AuthorizationType;
import com.amplifyframework.api.aws.EndpointType;

import com.amazonaws.DefaultRequest;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.http.HttpMethodName;
import com.amazonaws.util.VersionInfoUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;

/**
 * Interceptor to sign requests for AppSync from AppSync Android SDK.
 * See https://github.com/awslabs/aws-mobile-appsync-sdk-android
 */
public final class AppSyncSigV4SignerInterceptor implements Interceptor {

    private static final String TAG = AppSyncSigV4SignerInterceptor.class.getSimpleName();

    private static final String CONTENT_TYPE = "application/json";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse(CONTENT_TYPE);
    private static final String SERVICE_NAME = "appsync";
    private static final String HEADER_USER_AGENT = "User-Agent";
    private static final String X_API_KEY = "x-api-key";
    private static final String AUTHORIZATION = "authorization";

    private final AWSCredentialsProvider credentialsProvider;
    private final ApiKeyAuthProvider apiKeyProvider;

    private final CognitoUserPoolsAuthProvider cognitoUserPoolsAuthProvider;
    private final OidcAuthProvider oidcAuthProvider;
    private final String awsRegion;
    private final AuthorizationType authType;
    private final EndpointType endpointType;

    private AppSyncSigV4SignerInterceptor(AWSCredentialsProvider credentialsProvider,
                                          ApiKeyAuthProvider apiKeyProvider,
                                          CognitoUserPoolsAuthProvider cognitoUserPoolsAuthProvider,
                                          OidcAuthProvider oidcAuthProvider,
                                          String awsRegion,
                                          AuthorizationType authType,
                                          EndpointType endpointType) {
        this.credentialsProvider = credentialsProvider;
        this.apiKeyProvider = apiKeyProvider;
        this.cognitoUserPoolsAuthProvider = cognitoUserPoolsAuthProvider;
        this.oidcAuthProvider = oidcAuthProvider;
        this.awsRegion = awsRegion;
        this.authType = authType;
        this.endpointType = endpointType;
    }

    /**
     * Constructs an instance of AppSyncSigV4SignerInterceptor that
     * uses API key for authorization.
     * @param apiKeyProvider An instance of {@link ApiKeyAuthProvider}
     * @param endpointType Endpoint type for the api
     */
    public AppSyncSigV4SignerInterceptor(@NonNull ApiKeyAuthProvider apiKeyProvider,
                                         final EndpointType endpointType) {
        this(null,
                Objects.requireNonNull(apiKeyProvider),
                null,
                null,
                null,
                AuthorizationType.API_KEY,
                endpointType);
    }

    /**
     * Constructs an instance of AppSyncSigV4SignerInterceptor that
     * signs the request with AWS IAM credentials.
     * @param credentialsProvider An instance of {@link AWSCredentialsProvider}
     * @param awsRegion Associated AWS region
     * @param endpointType Endpoint type for the api
     */
    public AppSyncSigV4SignerInterceptor(@NonNull AWSCredentialsProvider credentialsProvider,
                                         final String awsRegion,
                                         final EndpointType endpointType) {
        this(Objects.requireNonNull(credentialsProvider),
                null,
                null,
                null,
                awsRegion,
                AuthorizationType.AWS_IAM,
                endpointType);
    }

    /**
     * Constructs an instance of AppSyncSigV4SignerInterceptor that
     * authorizes user with Cognito User Pool token.
     * @param cognitoUserPoolsAuthProvider An instance of {@link CognitoUserPoolsAuthProvider}
     * @param endpointType Endpoint type for the api
     */
    public AppSyncSigV4SignerInterceptor(@NonNull CognitoUserPoolsAuthProvider cognitoUserPoolsAuthProvider,
                                         final EndpointType endpointType) {
        this(null,
                null,
                Objects.requireNonNull(cognitoUserPoolsAuthProvider),
                null,
                null,
                AuthorizationType.AMAZON_COGNITO_USER_POOLS,
                endpointType);
    }

    /**
     * Constructs an instance of AppSyncSigV4SignerInterceptor that
     * authorizes user with OpenID Connect token.
     * @param oidcAuthProvider An instance of {@link OidcAuthProvider}
     * @param endpointType Endpoint type for the api
     */
    public AppSyncSigV4SignerInterceptor(@NonNull OidcAuthProvider oidcAuthProvider,
                                         final EndpointType endpointType) {
        this(null,
                null,
                null,
                Objects.requireNonNull(oidcAuthProvider),
                null,
                AuthorizationType.OPENID_CONNECT,
                endpointType);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request req = chain.request();

        //Clone the request into a new DefaultRequest object and populate it with credentials
        DefaultRequest<?> dr = new DefaultRequest<>(SERVICE_NAME);
        //set the endpoint
        dr.setEndpoint(req.url().uri());
        //copy all the headers
        for (String headerName : req.headers().names()) {
            dr.addHeader(headerName, req.header(headerName));
        }
        //set the http method
        dr.setHttpMethod(HttpMethodName.valueOf(req.method()));

        //Add User Agent
        String userAgent = toHumanReadableAscii(VersionInfoUtils.getUserAgent());
        dr.addHeader(HEADER_USER_AGENT, userAgent);

        Buffer body = null;

        if (req.body() != null) {
            //write the body to a byte array stream.
            final Buffer buffer = new Buffer();
            req.body().writeTo(buffer);
            dr.setContent(buffer.inputStream());
            body = buffer.clone();
        } else {
            dr.setContent(new ByteArrayInputStream("".getBytes()));
        }

        //Sign or Decorate request with the required headers
        if (AuthorizationType.AWS_IAM.equals(authType)) {
            //get the aws credentials from provider.
            try {
                //Get credentials - This will refresh the credentials if necessary
                AWSCredentials credentials = this.credentialsProvider.getCredentials();
                //sign the request
                if (endpointType == EndpointType.GRAPHQL) {
                    new AppSyncV4Signer(this.awsRegion).sign(dr, credentials);
                } else {
                    new ApiGatewayIamSigner(this.awsRegion).sign(dr, credentials);
                }
            } catch (Exception error) {
                throw new IOException("Failed to read credentials to sign the request.", error);
            }
        } else if (AuthorizationType.API_KEY.equals(authType)) {
            dr.addHeader(X_API_KEY, apiKeyProvider.getAPIKey());
        } else if (AuthorizationType.AMAZON_COGNITO_USER_POOLS.equals(authType)) {
            try {
                dr.addHeader(AUTHORIZATION, cognitoUserPoolsAuthProvider.getLatestAuthToken());
            } catch (Exception error) {
                throw new IOException("Failed to retrieve Cognito User Pools token.", error);
            }
        } else if (AuthorizationType.OPENID_CONNECT.equals(authType)) {
            try {
                dr.addHeader(AUTHORIZATION, oidcAuthProvider.getLatestAuthToken());
            } catch (Exception error) {
                throw new IOException("Failed to retrieve OIDC token.", error);
            }
        }

        //Copy the signed/credentialed request back into an OKHTTP Request object.
        Request.Builder okReqBuilder = new Request.Builder();

        //set the headers from default request, since it contains the signed headers as well.
        for (Map.Entry<String, String> e : dr.getHeaders().entrySet()) {
            okReqBuilder.addHeader(e.getKey(), e.getValue());
        }

        //Set the URL and Method
        okReqBuilder.url(req.url());
        final RequestBody requestBody = body != null ?
                RequestBody.create(body.readByteArray(), JSON_MEDIA_TYPE) : null;
        okReqBuilder.method(req.method(), requestBody);

        //continue with chain.
        return chain.proceed(okReqBuilder.build());
    }

    // Utility method to convert string to human-readable format
    private String toHumanReadableAscii(String str) {
        for (int i = 0, length = str.length(), c; i < length; i += Character.charCount(c)) {
            c = str.codePointAt(i);
            if (c > '\u001f' && c < '\u007f') {
                continue;
            }
            Buffer buffer = new Buffer();
            buffer.writeUtf8(str, 0, i);
            for (int j = i; j < length; j += Character.charCount(c)) {
                c = str.codePointAt(j);
                if (c > '\u001f' && c < '\u007f') {
                    buffer.writeUtf8CodePoint(c);
                }
            }
            return buffer.readUtf8();
        }
        return str;
    }
}
