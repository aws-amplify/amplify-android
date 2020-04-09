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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.auth.AuthCodeDeliveryDetails;
import com.amplifyframework.auth.AuthException;
import com.amplifyframework.auth.AuthPlugin;
import com.amplifyframework.auth.AuthSession;
import com.amplifyframework.auth.AuthSignInState;
import com.amplifyframework.auth.options.AuthSignInOptions;
import com.amplifyframework.auth.options.AuthSignUpOptions;
import com.amplifyframework.auth.result.AuthSignInResult;
import com.amplifyframework.auth.result.AuthSignUpResult;
import com.amplifyframework.core.Consumer;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.HostedUIOptions;
import com.amazonaws.mobile.client.IdentityProvider;
import com.amazonaws.mobile.client.SignInUIOptions;
import com.amazonaws.mobile.client.UserState;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.mobile.client.results.SignInResult;
import com.amazonaws.mobile.client.results.SignUpResult;
import com.amazonaws.mobile.client.results.Tokens;
import com.amazonaws.mobile.client.results.UserCodeDeliveryDetails;
import com.amazonaws.mobile.config.AWSConfiguration;
import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class AWSCognitoAuthPlugin extends AuthPlugin<AWSMobileClient> {
    private static final String AWS_COGNITO_AUTH_PLUGIN_KEY = "awsCognitoAuthPlugin";
    private static final long SECONDS_BEFORE_TIMEOUT = 10;

    @NonNull
    @Override
    public String getPluginKey() {
        return AWS_COGNITO_AUTH_PLUGIN_KEY;
    }

    @Override
    public void configure(
            @NonNull JSONObject pluginConfiguration,
            @NonNull Context context
    ) throws AmplifyException {
        final CountDownLatch latch = new CountDownLatch(1);

        AWSMobileClient.getInstance().initialize(
            context,
            new AWSConfiguration(pluginConfiguration),
            new Callback<UserStateDetails>() {
                @Override
                public void onResult(UserStateDetails result) {
                    latch.countDown();
                }

                @Override
                public void onError(Exception error) {
                    throw new IllegalStateException("Failed to instantiate AWSMobileClient", error);
                }
            }
        );

        try {
            if (latch.await(SECONDS_BEFORE_TIMEOUT, TimeUnit.SECONDS)) {
                return;
            } else {
                throw new IllegalStateException(
                    "Failed to instantiate AWSMobileClient within " + SECONDS_BEFORE_TIMEOUT + " seconds"
                );
            }
        } catch (InterruptedException error) {
            throw new IllegalStateException(error);
        }
    }

    @Override
    public void signUp(
            @NonNull String username,
            @NonNull String password,
            @NonNull AuthSignUpOptions options,
            @NonNull final Consumer<AuthSignUpResult> onSuccess,
            @NonNull final Consumer<AuthException> onException
    ) {
        AWSMobileClient.getInstance().signUp(
            username,
            password,
            options.getUserAttributes(),
            options.getValidationData(),
            new Callback<SignUpResult>() {
                @Override
                public void onResult(SignUpResult result) {
                    UserCodeDeliveryDetails details = result.getUserCodeDeliveryDetails();

                    onSuccess.accept(new AuthSignUpResult(
                        result.getConfirmationState(),
                        details != null
                            ? new AuthCodeDeliveryDetails(
                                details.getDestination(),
                                AuthCodeDeliveryDetails.DeliveryMedium.getEnum(details.getDeliveryMedium()),
                                details.getAttributeName()
                            )
                            : null
                    ));
                }

                @Override
                public void onError(Exception error) {
                    onException.accept(
                        new AuthException("Sign up failed", error, "See attached exception for more details")
                    );
                }
            }
        );
    }

    @Override
    public void confirmSignUp(
        @NonNull String username,
        @NonNull String confirmationCode,
        @NonNull final Consumer<AuthSignUpResult> onSuccess,
        @NonNull final Consumer<AuthException> onException
    ) {
        AWSMobileClient.getInstance().confirmSignUp(username, confirmationCode, new Callback<SignUpResult>() {
            @Override
            public void onResult(SignUpResult result) {
                UserCodeDeliveryDetails details = result.getUserCodeDeliveryDetails();

                onSuccess.accept(new AuthSignUpResult(
                    result.getConfirmationState(),
                    details != null
                        ? new AuthCodeDeliveryDetails(
                            details.getDestination(),
                            AuthCodeDeliveryDetails.DeliveryMedium.getEnum(details.getDeliveryMedium()),
                            details.getAttributeName()
                        )
                        : null
                ));
            }

            @Override
            public void onError(Exception error) {
                onException.accept(
                    new AuthException("Confirm sign up failed", error, "See attached exception for more details")
                );
            }
        });
    }

    @Override
    public void resendSignUpCode(
            @NonNull String username,
            @NonNull Consumer<AuthSignUpResult> onSuccess,
            @NonNull Consumer<AuthException> onException
    ) {
        AWSMobileClient.getInstance().resendSignUp(username, new Callback<SignUpResult>() {
            @Override
            public void onResult(SignUpResult result) {
                UserCodeDeliveryDetails details = result.getUserCodeDeliveryDetails();

                onSuccess.accept(new AuthSignUpResult(
                    result.getConfirmationState(),
                    details != null
                        ? new AuthCodeDeliveryDetails(
                            details.getDestination(),
                            AuthCodeDeliveryDetails.DeliveryMedium.getEnum(details.getDeliveryMedium()),
                            details.getAttributeName()
                        )
                        : null
                ));
            }

            @Override
            public void onError(Exception error) {
                onException.accept(
                    new AuthException(
                        "Resend confirmation code failed",
                        error,
                        "See attached exception for more details"
                    )
                );
            }
        });
    }

    @Override
    public void signIn(
        @Nullable String username,
        @Nullable String password,
        @NonNull AuthSignInOptions options,
        @NonNull final Consumer<AuthSignInResult> onSuccess,
        @NonNull final Consumer<AuthException> onException
    ) {
        AWSMobileClient.getInstance().signIn(username, password, null, new Callback<SignInResult>() {
            @Override
            public void onResult(SignInResult result) {
                AuthSignInState state;
                UserCodeDeliveryDetails oldDetails = result.getCodeDetails();
                // Take information from Cognito specific object and wrap it in the new Amplify object
                AuthCodeDeliveryDetails newDetails =
                    oldDetails != null
                        ? new AuthCodeDeliveryDetails(
                            oldDetails.getDestination(),
                            AuthCodeDeliveryDetails.DeliveryMedium.getEnum(oldDetails.getDeliveryMedium()),
                            oldDetails.getAttributeName()
                        )
                        : null;

                onSuccess.accept(
                        new AuthSignInResult(AuthSignInState.fromString(result.getSignInState().toString()), newDetails)
                );
            }

            @Override
            public void onError(Exception error) {
                onException.accept(
                    new AuthException("Sign in failed", error, "See attached exception for more details")
                );
            }
        });
    }

    @Override
    public void signIn(
            @Nullable String username,
            @Nullable String password,
            @NonNull final Consumer<AuthSignInResult> onSuccess,
            @NonNull final Consumer<AuthException> onException
    ) {
        signIn(username, password, null, onSuccess, onException);
    }

    @Override
    public void signInWithUI(
            @NonNull Activity callingActivity,
            @NonNull final Consumer<String> onSuccess,
            @NonNull final Consumer<AmplifyException> onException
    ) {
        HostedUIOptions hostedUIOptions = HostedUIOptions.builder()
                .scopes("openid", "email")
                .build();
        SignInUIOptions signInUIOptions = SignInUIOptions.builder()
                .hostedUIOptions(hostedUIOptions)
                .build();

        AWSMobileClient.getInstance().showSignIn(callingActivity, signInUIOptions, new Callback<UserStateDetails>() {
            @Override
            public void onResult(UserStateDetails details) {
                onSuccess.accept(details.getUserState().toString());
            }

            @Override
            public void onError(Exception error) {
                onException.accept(
                    new AmplifyException("Sign in with UI failed", error, "See attached exception for more details")
                );
            }
        });
    }

    @Override
    public void handleSignInWithUIResponse(@NonNull Intent intent) {
        AWSMobileClient.getInstance().handleAuthResponse(intent);
    }

    @Override
    public void signInWithFacebook(
            @NonNull String token,
            @NonNull final Consumer<String> onSuccess,
            @NonNull final Consumer<AmplifyException> onException
    ) {
        AWSMobileClient.getInstance().federatedSignIn(
            IdentityProvider.FACEBOOK.toString(),
            token,
            new Callback<UserStateDetails>() {
                @Override
                public void onResult(UserStateDetails result) {
                    onSuccess.accept(result.getUserState().toString());
                }

                @Override
                public void onError(Exception error) {
                    onException.accept(
                        new AmplifyException(
                                "Sign in with Facebook failed",
                                error,
                                "See attached exception for more details"
                        )
                    );
                }
            }
        );
    }

    // The result of a success callback is an object of type AWSCognitoAuthSession so that when the result
    // is cast to that type, the following Cognito specific fields can be retrieved:
    //   - AWSCredentials
    //   - Cognito Identity ID
    //   - Cognito Access Token
    //   - Cognito ID Token
    //   - Cognito Refresh Token
    @Override
    public void fetchAuthSession(
            @NonNull Consumer<AuthSession> onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {
        try {
            AWSMobileClient.getInstance().currentUserState(new Callback<UserStateDetails>() {
                @Override
                public void onResult(UserStateDetails result) {
                    UserState state = result.getUserState();

                    switch (state) {
                        case SIGNED_OUT:
                        case SIGNED_OUT_USER_POOLS_TOKENS_INVALID:
                        case SIGNED_OUT_FEDERATED_TOKENS_INVALID:
                            onSuccess.accept(
                                AWSCognitoAuthSession.builder().isSignedIn(false).build()
                            );
                            break;
                        case SIGNED_IN:
                        case GUEST:
                            try {
                                onSuccess.accept(buildCognitoAuthSession(state));
                            } catch (AuthException exception) {
                                onError.accept(exception);
                            }
                            break;
                        default:
                            onError.accept(new AuthException(
                                    "User is in an unknown authorization state",
                                    "This is a bug. Please report it to the AWS team for resolution"
                            ));
                    }
                }

                @Override
                public void onError(Exception exception) {
                    onError.accept(new AuthException(
                            "An error occurred while attempting to retrieve your user details",
                            exception,
                            "See attached exception for more details"
                    ));
                }
            });
        } catch (Throwable exception) {
            onError.accept(new AuthException(
                    "An error occurred fetching authorization details for the current user",
                    exception,
                    "See attached exception for more details"
            ));
        }
    }

    @NonNull
    @Override
    public AWSMobileClient getEscapeHatch() {
        return AWSMobileClient.getInstance();
    }

    private AWSCognitoAuthSession buildCognitoAuthSession(UserState state) throws AuthException {
        final AWSCognitoAuthSession.Builder sessionBuilder;
        final CountDownLatch latch = new CountDownLatch(3);

        sessionBuilder = AWSCognitoAuthSession.builder()
            .isSignedIn(state.equals(UserState.SIGNED_IN))
            .identityId(AWSMobileClient.getInstance().getIdentityId());

        AWSMobileClient.getInstance().getTokens(new Callback<Tokens>() {
            @Override
            public void onResult(Tokens result) {
                sessionBuilder.accessToken(result.getAccessToken().getTokenString());
                sessionBuilder.idToken(result.getIdToken().getTokenString());
                sessionBuilder.refreshToken(result.getRefreshToken().getTokenString());
                latch.countDown();
            }

            @Override
            public void onError(Exception exception) {
                latch.countDown();
            }
        });

        AWSMobileClient.getInstance().getAWSCredentials(new Callback<AWSCredentials>() {
            @Override
            public void onResult(AWSCredentials result) {
                sessionBuilder.awsCredentials(result);
                latch.countDown();
            }

            @Override
            public void onError(Exception exception) {
                latch.countDown();
            }
        });

        AWSMobileClient.getInstance().getUserAttributes(new Callback<Map<String, String>>() {
            @Override
            public void onResult(Map<String, String> result) {
                sessionBuilder.userSub(result.get("sub"));
                latch.countDown();
            }

            @Override
            public void onError(Exception exception) {
                latch.countDown();
            }
        });

        try {
            if (latch.await(SECONDS_BEFORE_TIMEOUT, TimeUnit.SECONDS)) {
                return sessionBuilder.build();
            } else {
                throw new AuthException(
                    "Failed to retrieve auth details within " + SECONDS_BEFORE_TIMEOUT + " seconds",
                    "Check network connectivity"
                );
            }
        } catch (InterruptedException exception) {
            throw new AuthException(
                    "Failed to retrieve auth details",
                    exception,
                    "See attached exception for more details"
            );
        }
    }
}
