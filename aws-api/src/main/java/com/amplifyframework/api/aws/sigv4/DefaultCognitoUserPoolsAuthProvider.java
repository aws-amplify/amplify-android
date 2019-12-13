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

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiException;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.results.Tokens;

import java.util.concurrent.Semaphore;

/**
 * Basic retrieval of the Cognito Userpools token. The user must have already signed-in before using
 * this class as the retrieval mechanism.
 * This provider requires {@link AWSMobileClient} singleton instance to be already initialized
 * to successfully fetch the token.
 */
public final class DefaultCognitoUserPoolsAuthProvider implements CognitoUserPoolsAuthProvider {

    private static final String TAG = DefaultCognitoUserPoolsAuthProvider.class.getSimpleName();

    private String token;
    private String lastTokenRetrievalFailureMessage;

    // Fetches token from the mobile client.
    private synchronized void fetchToken() throws ApiException {
        final Semaphore semaphore = new Semaphore(0);
        lastTokenRetrievalFailureMessage = null;
        AWSMobileClient.getInstance().getTokens(new Callback<Tokens>() {
            @Override
            public void onResult(Tokens result) {
                token = result.getAccessToken().getTokenString();
                semaphore.release();
            }

            @Override
            public void onError(Exception error) {
                lastTokenRetrievalFailureMessage = error.getLocalizedMessage();
                semaphore.release();
            }
        });

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
            throw new ApiException(lastTokenRetrievalFailureMessage, AmplifyException.TODO_RECOVERY_SUGGESTION);
        }
    }

    @Override
    public String getLatestAuthToken() throws ApiException {
        fetchToken();
        return token;
    }

    @Override
    public String getUsername() {
        return AWSMobileClient.getInstance().getUsername();
    }
}
