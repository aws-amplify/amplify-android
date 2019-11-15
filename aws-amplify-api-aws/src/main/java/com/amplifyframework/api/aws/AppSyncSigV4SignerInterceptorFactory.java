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

import android.content.Context;

import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.aws.sigv4.ApiKeyAuthProvider;
import com.amplifyframework.api.aws.sigv4.AppSyncSigV4SignerInterceptor;
import com.amplifyframework.api.aws.sigv4.BasicCognitoUserPoolsAuthProvider;
import com.amplifyframework.api.aws.sigv4.CognitoUserPoolsAuthProvider;
import com.amplifyframework.api.aws.sigv4.OidcAuthProvider;
import com.amplifyframework.core.plugin.PluginException;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;

import java.util.concurrent.Semaphore;

/**
 * Implementation of {@link InterceptorFactory} that creates
 * {@link AppSyncSigV4SignerInterceptor} from provided configuration.
 * This factory should be constructed once in a plugin.
 */
final class AppSyncSigV4SignerInterceptorFactory implements InterceptorFactory {
    private final Context context;
    private final ApiAuthProviders apiAuthProviders;

    AppSyncSigV4SignerInterceptorFactory(
            final Context context,
            ApiAuthProviders apiAuthProviders) {
        this.context = context;
        this.apiAuthProviders = apiAuthProviders;
    }

    /**
     * Implementation of {@link InterceptorFactory#create(ApiConfiguration)} that
     * uses preconfigured instances of authorization providers to construct a new
     * instance of {@link AppSyncSigV4SignerInterceptor}. It reads the
     * {@link ApiConfiguration} to determine the type of authorization mode, from
     * which it determines the type of authorization provider to use.
     *
     * If the authorization mode is {@link AuthorizationType#API_KEY} and the
     * {@link ApiAuthProviders} was not overridden with a custom
     * {@link ApiKeyAuthProvider}, then the API key is read from
     * {@link ApiConfiguration} each time this method is called.
     *
     * For all other authorization modes, the factory will reuse the auth providers
     * that were generated (or overridden) during construction of this factory
     * instance.
     *
     * @param config API configuration
     * @return configured interceptor that signs requests using
     *         authorization mode specified in API configuration
     */
    @Override
    public AppSyncSigV4SignerInterceptor create(ApiConfiguration config) {
        switch (config.getAuthorizationType()) {
            case API_KEY:
                // API key provider is configured per API, not per plugin.
                // If a custom instance of API key provider was provided, the
                // factory will remember and reuse it.
                // Otherwise, a new lambda is made per interceptor generation.
                ApiKeyAuthProvider keyProvider = apiAuthProviders.getApiKeyAuthProvider();
                if (keyProvider == null) {
                    keyProvider = config::getApiKey;
                }
                return new AppSyncSigV4SignerInterceptor(keyProvider);
            case AWS_IAM:
                // Initializes mobile client once and remembers the instance.
                // This instance is reused by this factory.
                AWSCredentialsProvider credentialsProvider = apiAuthProviders.getAWSCredentialsProvider();
                if (credentialsProvider == null) {
                    credentialsProvider = getCredProvider(context);
                }
                return new AppSyncSigV4SignerInterceptor(credentialsProvider, config.getRegion());
            case AMAZON_COGNITO_USER_POOLS:

                // Initializes cognito user pool once and remembers the token
                // provider instance. This instance is reused by this factory.
                CognitoUserPoolsAuthProvider cognitoProvider = apiAuthProviders.getCognitoUserPoolsAuthProvider();
                if (cognitoProvider == null) {
                    CognitoUserPool userPool = new CognitoUserPool(context, new AWSConfiguration(context));
                    cognitoProvider = new BasicCognitoUserPoolsAuthProvider(userPool);
                }
                return new AppSyncSigV4SignerInterceptor(cognitoProvider);
            case OPENID_CONNECT:
                // This factory does not have a default implementation for
                // OpenID Connect token provider. User-provided implementation
                // is remembered and reused by this factory.
                OidcAuthProvider oidcProvider = apiAuthProviders.getOidcAuthProvider();
                if (oidcProvider == null) {
                    oidcProvider = () -> {
                        throw new ApiException.AuthorizationTypeNotConfiguredException(
                            "OidcAuthProvider interface is not implemented.");
                    };
                }
                return new AppSyncSigV4SignerInterceptor(oidcProvider);
            default:
                throw new PluginException.PluginConfigurationException(
                        "Unsupported authorization mode.");
        }
    }

    // Helper method to initialize AWS Mobile Client.
    private AWSCredentialsProvider getCredProvider(Context context) {
        final Semaphore semaphore = new Semaphore(0);
        AWSMobileClient.getInstance().initialize(context, new Callback<UserStateDetails>() {
            @Override
            public void onResult(UserStateDetails result) {
                semaphore.release();
            }

            @Override
            public void onError(Exception error) {
                throw new RuntimeException("Failed to initialize mobile client.", error);
            }
        });

        try {
            semaphore.acquire();
        } catch (InterruptedException exception) {
            throw new ApiException("Interrupted signing into mobile client.", exception);
        } catch (Exception error) {
            throw new ApiException(error.getLocalizedMessage(), error);
        }
        return AWSMobileClient.getInstance();
    }
}
