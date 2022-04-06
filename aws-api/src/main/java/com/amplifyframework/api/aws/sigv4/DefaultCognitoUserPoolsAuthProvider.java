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

package com.amplifyframework.api.aws.sigv4;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiException;
import com.amplifyframework.api.ApiException.ApiAuthException;
import com.amplifyframework.api.aws.auth.CognitoCredentialsProvider;
import com.amplifyframework.auth.AuthUser;
import com.amplifyframework.core.Amplify;

import java.util.concurrent.Semaphore;

/**
 * Basic retrieval of the Cognito Userpools token. The user must have already signed-in before using
 * this class as the retrieval mechanism.
 * to successfully fetch the token.
 */
public final class DefaultCognitoUserPoolsAuthProvider implements CognitoUserPoolsAuthProvider {

    private String token;
    private final CognitoCredentialsProvider credentialsProvider;
    private String lastTokenRetrievalFailureMessage;

    /**
     * Creates the object with the instance of AWSMobileClient pulled over from the Auth plugin.
     * TODO: Once we come up with a proper interface, we will remove the use of AWSMobileClient directly in favor
     * of using the AuthSession from Auth.
     *
     * @throws ApiAuthException Thrown if the AWSCognitoAuth plugin is not added.
     */
    public DefaultCognitoUserPoolsAuthProvider() throws ApiAuthException {
        try {
            this.credentialsProvider = new CognitoCredentialsProvider();
        } catch (IllegalStateException exception) {
            throw new ApiAuthException(
                    "AWSApiPlugin depends on AWSCognitoAuthPlugin but it is currently missing",
                    exception,
                    "Before configuring Amplify, be sure to add AWSCognitoAuthPlugin same as you added AWSApiPlugin."
            );
        }
    }

    // Fetches token from the mobile client.
    private synchronized void fetchToken() throws ApiException {
        final Semaphore semaphore = new Semaphore(0);
        lastTokenRetrievalFailureMessage = null;
        credentialsProvider.getAccessToken(value -> {
                token = value;
                semaphore.release();
            }, error -> {
                lastTokenRetrievalFailureMessage = error.getLocalizedMessage();
                semaphore.release();
            }
        );

        try {
            semaphore.acquire();
        } catch (InterruptedException exception) {
            throw new ApiException(
                "Interrupted waiting for Cognito Userpools token.",
                exception,
                AmplifyException.TODO_RECOVERY_SUGGESTION
            );
        }

        if (lastTokenRetrievalFailureMessage != null) {
            throw new ApiAuthException(lastTokenRetrievalFailureMessage, AmplifyException.TODO_RECOVERY_SUGGESTION);
        }
    }

    @Override
    public String getLatestAuthToken() throws ApiException {
        fetchToken();
        return token;
    }

    @Override
    public String getUsername() {
        AuthUser currentUser = Amplify.Auth.getCurrentUser();

        return currentUser == null ? null : currentUser.getUsername();
    }
}
