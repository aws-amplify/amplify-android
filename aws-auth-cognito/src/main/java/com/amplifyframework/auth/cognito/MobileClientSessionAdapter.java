/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.auth.cognito;

import androidx.annotation.NonNull;

import com.amplifyframework.auth.AuthException;
import com.amplifyframework.auth.AuthSession;
import com.amplifyframework.auth.result.AuthSessionResult;
import com.amplifyframework.core.Consumer;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.results.Tokens;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.util.CognitoJWTParser;
import org.json.JSONException;

import java.util.Arrays;
import java.util.List;

/**
 * Internal helper class for building session objects from AWSMobileClient.
 */
final class MobileClientSessionAdapter {
    // Since AWSMobileClient does not categorize its errors in any way, we resort to categorizing its
    // messages here as a temporary hack until we replace it with a fresh implementation of Auth.
    private static final List<String> MOBILE_CLIENT_INVALID_ACCOUNT_MESSAGES = Arrays.asList(
            "getTokens does not support retrieving tokens for federated sign-in",
            "You must be signed-in with Cognito Userpools to be able to use getTokens",
            "Tokens are not supported for OAuth2",
            "Cognito Identity not configured"
    );

    private static final List<String> MOBILE_CLIENT_SIGNED_OUT_MESSAGES = Arrays.asList(
            "getTokens does not support retrieving tokens while signed-out"
    );

    private MobileClientSessionAdapter() { }

    static void fetchSignedOutSession(
            @NonNull AWSMobileClient awsMobileClient,
            @NonNull Consumer<AuthSession> onComplete) {

        // Try to get identity ID - if the account doesn't support identity pools, Android AWSMobileClient throws an
        // exception and we return an invalid account exception for identity ID and AWS credentials. If it's just null,
        // it could be guest access is not setup or it just hasn't been online to retrieve them yet so for now we leave
        // it at a signed out exception.
        try {
            String identityId = awsMobileClient.getIdentityId();

            if (identityId == null) {
                onComplete.accept(signedOutSessionWithIdentityPool());
                return;
            } else {
                fetchSignedOutSessionWithIdentityId(identityId, awsMobileClient, onComplete);
            }
        } catch (Throwable exception) {
            onComplete.accept(signedOutSessionWithoutIdentityPool());
            return;
        }
    }

    static void fetchSignedInSession(
            @NonNull AWSMobileClient awsMobileClient,
            @NonNull Consumer<AuthSession> onComplete) {
        awsMobileClient.getTokens(new Callback<Tokens>() {
            @Override
            public void onResult(Tokens result) {

                AuthSessionResult<String> userSubResult;

                try {
                    userSubResult = AuthSessionResult.success(
                            CognitoJWTParser
                                .getPayload(result.getAccessToken().getTokenString())
                                .getString("sub")
                    );
                } catch (JSONException error) {
                    userSubResult = AuthSessionResult.failure(new AuthException.UnknownException(error));
                }

                AuthSessionResult<AWSCognitoUserPoolTokens> tokensResult =
                    AuthSessionResult.success(
                        new AWSCognitoUserPoolTokens(
                            result.getAccessToken().getTokenString(),
                            result.getIdToken().getTokenString(),
                            result.getRefreshToken().getTokenString()
                        )
                    );

                fetchSignedInSessionWithUserPoolResults(
                        userSubResult,
                        tokensResult,
                        awsMobileClient,
                        onComplete
                );
            }

            @Override
            public void onError(Exception error) {
                if (MOBILE_CLIENT_INVALID_ACCOUNT_MESSAGES.contains(error.getMessage())) {
                    fetchIdentityPoolOnlySignedInSession(awsMobileClient, onComplete);
                } else if (MOBILE_CLIENT_SIGNED_OUT_MESSAGES.contains(error.getMessage())) {
                    fetchSignedOutSession(awsMobileClient, onComplete);
                } else {
                    fetchSignedInSessionWithUserPoolResults(
                        AuthSessionResult.failure(new AuthException.UnknownException(error)),
                        AuthSessionResult.failure(new AuthException.UnknownException(error)),
                        awsMobileClient,
                        onComplete
                    );
                }
            }
        });
    }

    private static void fetchIdentityPoolOnlySignedInSession(
            AWSMobileClient awsMobileClient,
            Consumer<AuthSession> onComplete) {
        AuthSessionResult<String> userSubResult =
                AuthSessionResult.failure(new AuthException.InvalidAccountTypeException());
        AuthSessionResult<AWSCognitoUserPoolTokens> tokensResult =
                AuthSessionResult.failure(new AuthException.InvalidAccountTypeException());

        fetchSignedInSessionWithUserPoolResults(
                userSubResult,
                tokensResult,
                awsMobileClient,
                onComplete
        );
    }

    private static void fetchSignedInSessionWithUserPoolResults(
            AuthSessionResult<String> userSubResult,
            AuthSessionResult<AWSCognitoUserPoolTokens> tokensResult,
            AWSMobileClient awsMobileClient,
            Consumer<AuthSession> onComplete) {
        awsMobileClient.getAWSCredentials(new Callback<AWSCredentials>() {
            @Override
            public void onResult(AWSCredentials result) {
                if (result != null) {
                    fetchSignedInSessionWithUserPoolAndAWSCredentialResults(
                            AuthSessionResult.success(result),
                            userSubResult,
                            tokensResult,
                            awsMobileClient,
                            onComplete
                    );
                } else {
                    AuthException error = new AuthException(
                            "Could not fetch AWS Cognito credentials, but there was no error reported back from " +
                            "AWSMobileClient.getAWSCredentials call.",
                            "This is a bug with the underlying AWSMobileClient");

                    onComplete.accept(
                        new AWSCognitoAuthSession(
                            true,
                            AuthSessionResult.failure(error),
                            AuthSessionResult.failure(error),
                            userSubResult,
                            tokensResult
                        )
                    );
                }
            }

            @Override
            public void onError(Exception error) {
                AuthException wrappedError;

                if (MOBILE_CLIENT_INVALID_ACCOUNT_MESSAGES.contains(error.getMessage())) {
                    wrappedError = new AuthException.InvalidAccountTypeException(error);
                } else {
                    wrappedError = new AuthException.UnknownException(error);
                }

                onComplete.accept(
                        new AWSCognitoAuthSession(
                                true,
                                AuthSessionResult.failure(wrappedError),
                                AuthSessionResult.failure(wrappedError),
                                userSubResult,
                                tokensResult
                        )
                );
            }
        });
    }

    private static void fetchSignedInSessionWithUserPoolAndAWSCredentialResults(
            AuthSessionResult<AWSCredentials> awsCredentialsResult,
            AuthSessionResult<String> userSubResult,
            AuthSessionResult<AWSCognitoUserPoolTokens> tokensResult,
            AWSMobileClient awsMobileClient, Consumer<AuthSession> onComplete
    ) {
        try {
            String identityId = awsMobileClient.getIdentityId();
            AuthSessionResult<String> identityIdResult;

            if (identityId != null) {
                identityIdResult = AuthSessionResult.success(identityId);
            } else {
                identityIdResult = AuthSessionResult.failure(new AuthException(
                        "AWSMobileClient returned awsCredentials but no identity id and no error",
                        "This should never happen and is a bug with AWSMobileClient."
                ));
            }

            onComplete.accept(
                    new AWSCognitoAuthSession(
                            true,
                            identityIdResult,
                            awsCredentialsResult,
                            userSubResult,
                            tokensResult
                    )
            );
        } catch (Throwable identityIdError) {
            onComplete.accept(
                    new AWSCognitoAuthSession(
                            true,
                            AuthSessionResult.failure(new AuthException.UnknownException(identityIdError)),
                            awsCredentialsResult,
                            userSubResult,
                            tokensResult
                    )
            );
        }
    }

    private static void fetchSignedOutSessionWithIdentityId(
            String identityId,
            AWSMobileClient awsMobileClient,
            Consumer<AuthSession> onComplete) {
        awsMobileClient.getAWSCredentials(new Callback<AWSCredentials>() {
            @Override
            public void onResult(AWSCredentials result) {
                if (result != null) {
                    onComplete.accept(
                            new AWSCognitoAuthSession(
                                    false,
                                    AuthSessionResult.success(identityId),
                                    AuthSessionResult.success(result),
                                    AuthSessionResult.failure(new AuthException.SignedOutException()),
                                    AuthSessionResult.failure(new AuthException.SignedOutException())
                            )
                    );
                } else {
                    onComplete.accept(signedOutSessionWithIdentityPool());
                }
            }

            // Currently the errors returned by AWSMobileClient don't specify why credentials were unavailable.
            // So we go for the more generic signed out exception for now.
            @Override
            public void onError(Exception error) {
                onComplete.accept(signedOutSessionWithIdentityPool());
            }
        });
    }

    private static AuthSession signedOutSessionWithoutIdentityPool() {
        return new AWSCognitoAuthSession(
                false,
                AuthSessionResult.failure(new AuthException.InvalidAccountTypeException()),
                AuthSessionResult.failure(new AuthException.InvalidAccountTypeException()),
                AuthSessionResult.failure(new AuthException.SignedOutException()),
                AuthSessionResult.failure(new AuthException.SignedOutException())
        );
    }

    private static AuthSession signedOutSessionWithIdentityPool() {
        return new AWSCognitoAuthSession(
                false,
                AuthSessionResult.failure(new AuthException.SignedOutException()),
                AuthSessionResult.failure(new AuthException.SignedOutException()),
                AuthSessionResult.failure(new AuthException.SignedOutException()),
                AuthSessionResult.failure(new AuthException.SignedOutException())
        );
    }
}
