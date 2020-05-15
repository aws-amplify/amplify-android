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
import androidx.annotation.VisibleForTesting;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.auth.AuthCodeDeliveryDetails;
import com.amplifyframework.auth.AuthException;
import com.amplifyframework.auth.AuthPlugin;
import com.amplifyframework.auth.AuthSession;
import com.amplifyframework.auth.AuthUser;
import com.amplifyframework.auth.AuthUserAttribute;
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthSignInOptions;
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthSignUpOptions;
import com.amplifyframework.auth.cognito.util.SignInStateConverter;
import com.amplifyframework.auth.options.AuthSignInOptions;
import com.amplifyframework.auth.options.AuthSignUpOptions;
import com.amplifyframework.auth.options.AuthWebUISignInOptions;
import com.amplifyframework.auth.result.AuthResetPasswordResult;
import com.amplifyframework.auth.result.AuthSignInResult;
import com.amplifyframework.auth.result.AuthSignUpResult;
import com.amplifyframework.auth.result.step.AuthNextResetPasswordStep;
import com.amplifyframework.auth.result.step.AuthNextSignInStep;
import com.amplifyframework.auth.result.step.AuthNextSignUpStep;
import com.amplifyframework.auth.result.step.AuthResetPasswordStep;
import com.amplifyframework.auth.result.step.AuthSignUpStep;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Consumer;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.HostedUIOptions;
import com.amazonaws.mobile.client.SignInUIOptions;
import com.amazonaws.mobile.client.UserState;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.mobile.client.results.ForgotPasswordResult;
import com.amazonaws.mobile.client.results.ForgotPasswordState;
import com.amazonaws.mobile.client.results.SignInResult;
import com.amazonaws.mobile.client.results.SignInState;
import com.amazonaws.mobile.client.results.SignUpResult;
import com.amazonaws.mobile.client.results.Tokens;
import com.amazonaws.mobile.client.results.UserCodeDeliveryDetails;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.util.CognitoJWTParser;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A Cognito implementation of the Auth Plugin.
 */
public final class AWSCognitoAuthPlugin extends AuthPlugin<AWSMobileClient> {
    private static final String AWS_COGNITO_AUTH_PLUGIN_KEY = "awsCognitoAuthPlugin";
    private static final long SECONDS_BEFORE_TIMEOUT = 10;
    private static final String COGNITO_USER_ID_ATTRIBUTE = "sub";
    private static final String MOBILE_CLIENT_TOKEN_KEY = "token";
    private String userId;
    private AWSMobileClient awsMobileClient;

    /**
     * A Cognito implementation of the Auth Plugin.
     */
    public AWSCognitoAuthPlugin() {
        this.awsMobileClient = AWSMobileClient.getInstance();
    }
    
    @VisibleForTesting
    AWSCognitoAuthPlugin(AWSMobileClient instance, String userId) {
        this.awsMobileClient = instance;
        this.userId = userId;
    }

    @NonNull
    @Override
    public String getPluginKey() {
        return AWS_COGNITO_AUTH_PLUGIN_KEY;
    }

    @Override
    public void configure(
            @NonNull JSONObject pluginConfiguration,
            @NonNull Context context
    ) throws AuthException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Exception> asyncException = new AtomicReference<>();

        awsMobileClient.initialize(
            context,
            new AWSConfiguration(pluginConfiguration),
            new Callback<UserStateDetails>() {
                @Override
                public void onResult(UserStateDetails result) {
                    if (UserState.SIGNED_IN.equals(result.getUserState())) {
                        userId = getUserIdFromToken(result.getDetails().get(MOBILE_CLIENT_TOKEN_KEY));
                    } else {
                        userId = null;
                    }

                    // Set up a listener to asynchronously update the user id if the user state changes in the future
                    awsMobileClient.addUserStateListener(userStateDetails -> {
                        switch (userStateDetails.getUserState()) {
                            case SIGNED_IN:
                                fetchAndSetUserId(() -> { /* No response needed */ });
                                break;
                            default:
                                userId = null;
                        }
                    });

                    latch.countDown();
                }

                @Override
                public void onError(Exception error) {
                    asyncException.set(error);
                    latch.countDown();
                }
            }
        );

        try {
            if (latch.await(SECONDS_BEFORE_TIMEOUT, TimeUnit.SECONDS)) {
                if (asyncException.get() != null) {
                    throw new AuthException(
                        "Failed to instantiate AWSMobileClient",
                        asyncException.get(),
                        "See attached exception for more details"
                    );
                }
                return;
            } else {
                throw new AuthException(
                    "Failed to instantiate AWSMobileClient within " + SECONDS_BEFORE_TIMEOUT + " seconds",
                    "Check network connectivity"
                );
            }
        } catch (InterruptedException error) {
            throw new AuthException(
                    "Failed to instantiate AWSMobileClient",
                    error,
                    "See attached exception for more details"
            );
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
        Map<String, String> userAttributes = new HashMap<>();
        Map<String, String> validationData = new HashMap<>();

        if (options.getUserAttributes() != null) {
            for (AuthUserAttribute attribute : options.getUserAttributes()) {
                userAttributes.put(attribute.getKey().getKeyString(), attribute.getValue());
            }
        }

        if (options instanceof AWSCognitoAuthSignUpOptions) {
            validationData = ((AWSCognitoAuthSignUpOptions) options).getValidationData();
        }

        awsMobileClient.signUp(
            username,
            password,
            userAttributes,
            validationData,
            new Callback<SignUpResult>() {
                @Override
                public void onResult(SignUpResult result) {
                    onSuccess.accept(convertSignUpResult(result));
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
        awsMobileClient.confirmSignUp(username, confirmationCode, new Callback<SignUpResult>() {
            @Override
            public void onResult(SignUpResult result) {
                onSuccess.accept(convertSignUpResult(result));
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
        awsMobileClient.resendSignUp(username, new Callback<SignUpResult>() {
            @Override
            public void onResult(SignUpResult result) {
                onSuccess.accept(convertSignUpResult(result));
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
        Map<String, String> metadata = null;

        if (options != null && options instanceof AWSCognitoAuthSignInOptions) {
            metadata = ((AWSCognitoAuthSignInOptions) options).getMetadata();
        }

        awsMobileClient.signIn(username, password, metadata, new Callback<SignInResult>() {
            @Override
            public void onResult(SignInResult result) {
                try {
                    AuthSignInResult newResult = convertSignInResult(result);
                    fetchAndSetUserId(() -> onSuccess.accept(newResult));
                } catch (AuthException exception) {
                    onException.accept(exception);
                }
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
        signIn(username, password, AWSCognitoAuthSignInOptions.builder().build(), onSuccess, onException);
    }

    @Override
    public void confirmSignIn(
            @NonNull String confirmationCode,
            @NonNull Consumer<AuthSignInResult> onSuccess,
            @NonNull Consumer<AuthException> onException
    ) {
        awsMobileClient.confirmSignIn(confirmationCode, new Callback<SignInResult>() {
            @Override
            public void onResult(SignInResult result) {
                try {
                    AuthSignInResult newResult = convertSignInResult(result);
                    fetchAndSetUserId(() -> onSuccess.accept(newResult));
                } catch (AuthException exception) {
                    onException.accept(exception);
                }
            }

            @Override
            public void onError(Exception error) {
                onException.accept(
                        new AuthException("Confirm sign in failed", error, "See attached exception for more details")
                );
            }
        });
    }

    @Override
    public void signInWithWebUI(
            @NonNull Activity callingActivity,
            @NonNull final Consumer<AuthSignInResult> onSuccess,
            @NonNull final Consumer<AmplifyException> onException
    ) {
        signInWithWebUI(callingActivity, AuthWebUISignInOptions.builder().build(), onSuccess, onException);
    }

    @Override
    public void signInWithWebUI(
            @NonNull Activity callingActivity,
            @NonNull AuthWebUISignInOptions options,
            @NonNull Consumer<AuthSignInResult> onSuccess,
            @NonNull Consumer<AmplifyException> onException
    ) {
        HostedUIOptions hostedUIOptions = HostedUIOptions.builder()
                .scopes(options.getScopes().toArray(new String[options.getScopes().size()]))
                .signInQueryParameters(options.getSignInQueryParameters())
                .signOutQueryParameters(options.getSignOutQueryParameters())
                .tokenQueryParameters(options.getTokenQueryParameters())
                .build();

        SignInUIOptions signInUIOptions = SignInUIOptions.builder()
                .hostedUIOptions(hostedUIOptions)
                .build();

        awsMobileClient.showSignIn(callingActivity, signInUIOptions, new Callback<UserStateDetails>() {
            @Override
            public void onResult(UserStateDetails details) {
                fetchAndSetUserId(() -> onSuccess.accept(null));
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
    public void handleWebUISignInResponse(@NonNull Intent intent) {
        awsMobileClient.handleAuthResponse(intent);
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
            @NonNull Consumer<AuthException> onException
    ) {
        try {
            awsMobileClient.currentUserState(new Callback<UserStateDetails>() {
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
                                onException.accept(exception);
                            }
                            break;
                        default:
                            onException.accept(new AuthException(
                                    "User is in an unknown authorization state",
                                    "This is a bug. Please report it to the AWS team for resolution"
                            ));
                    }
                }

                @Override
                public void onError(Exception exception) {
                    onException.accept(new AuthException(
                            "An error occurred while attempting to retrieve your user details",
                            exception,
                            "See attached exception for more details"
                    ));
                }
            });
        } catch (Throwable exception) {
            onException.accept(new AuthException(
                    "An error occurred fetching authorization details for the current user",
                    exception,
                    "See attached exception for more details"
            ));
        }
    }

    @Override
    public void resetPassword(
            @NonNull String username,
            @NonNull Consumer<AuthResetPasswordResult> onSuccess,
            @NonNull Consumer<AuthException> onException
    ) {
        awsMobileClient.forgotPassword(username, new Callback<ForgotPasswordResult>() {
            @Override
            public void onResult(ForgotPasswordResult result) {
                if (result.getState().equals(ForgotPasswordState.CONFIRMATION_CODE)) {
                    onSuccess.accept(new AuthResetPasswordResult(
                            false,
                            new AuthNextResetPasswordStep(
                                    AuthResetPasswordStep.CONFIRM_RESET_PASSWORD_WITH_CODE,
                                    Collections.emptyMap(),
                                    convertCodeDeliveryDetails(result.getParameters())
                            )
                    ));
                } else {
                    onException.accept(new AuthException(
                            "Received an unsupported response after triggering password recovery: " + result.getState(),
                            "This is almost certainly a bug. Please report it as an issue in our GitHub repo."
                    ));
                }
            }

            @Override
            public void onError(Exception exception) {
                onException.accept(new AuthException(
                        "An error occurred triggering password recovery",
                        exception,
                        "See attached exception for more details"
                ));
            }
        });
    }

    @Override
    public void confirmResetPassword(
            @NonNull String newPassword,
            @NonNull String confirmationCode,
            @NonNull Action onSuccess,
            @NonNull Consumer<AuthException> onException
    ) {
        awsMobileClient.confirmForgotPassword(
            newPassword,
            confirmationCode,
            new Callback<ForgotPasswordResult>() {
                @Override
                public void onResult(ForgotPasswordResult result) {
                    if (result.getState().equals(ForgotPasswordState.DONE)) {
                        onSuccess.call();
                    } else {
                        onException.accept(new AuthException(
                                "Received an unsupported response while confirming password recovery code: "
                                        + result.getState(),
                                "This is almost certainly a bug. Please report it as an issue in our GitHub repo."
                        ));
                    }
                }

                @Override
                public void onError(Exception exception) {
                    onException.accept(new AuthException(
                            "An error occurred confirming password recovery code",
                            exception,
                            "See attached exception for more details"
                    ));
                }
            }
        );
    }

    @Override
    public void updatePassword(
            @NonNull String oldPassword,
            @NonNull String newPassword,
            @Nullable Action onSuccess,
            @Nullable Consumer<AuthException> onError
    ) {
        awsMobileClient.changePassword(oldPassword, newPassword, new Callback<Void>() {
            @Override
            public void onResult(Void result) {
                onSuccess.call();
            }

            @Override
            public void onError(Exception error) {
                onError.accept(new AuthException(
                        "Failed to change password",
                        error,
                        "See attached exception for more details"
                ));
            }
        });
    }

    @Override
    public AuthUser getCurrentUser() {
        if (userId != null) {
            return new AuthUser(userId, awsMobileClient.getUsername());
        } else {
            return null;
        }
    }

    @NonNull
    @Override
    public AWSMobileClient getEscapeHatch() {
        return awsMobileClient;
    }

    private void fetchAndSetUserId(Action onComplete) {
        awsMobileClient.getTokens(new Callback<Tokens>() {
            @Override
            public void onResult(Tokens result) {
                userId = getUserIdFromToken(result.getAccessToken().getTokenString());
                onComplete.call();
            }

            @Override
            public void onError(Exception error) {
                // If AWSMobileClient currently does not have the access token locally cached and/or otherwise
                // available we want our userId cache to match that state.
                userId = null;
                onComplete.call();
            }
        });
    }

    private String getUserIdFromToken(String token) {
        try {
            return CognitoJWTParser
                    .getPayload(token)
                    .getString(COGNITO_USER_ID_ATTRIBUTE);
        } catch (JSONException error) {
            return null;
        }
    }

    private AuthSignUpResult convertSignUpResult(SignUpResult result) {
        UserCodeDeliveryDetails details = result.getUserCodeDeliveryDetails();
        AuthCodeDeliveryDetails newDetails = details != null
                ? new AuthCodeDeliveryDetails(
                    details.getDestination(),
                    AuthCodeDeliveryDetails.DeliveryMedium.fromString(details.getDeliveryMedium()),
                    details.getAttributeName()
                )
                : null;

        return new AuthSignUpResult(
                true,
                new AuthNextSignUpStep(
                        result.getConfirmationState()
                                ? AuthSignUpStep.DONE
                                : AuthSignUpStep.CONFIRM_SIGN_UP_STEP,
                        Collections.emptyMap(),
                        newDetails
                )
        );
    }

    // Take information from the Cognito specific object and wrap it in the new Amplify object.
    // Throws an AuthException if the AWSMobileClient result is in an unsupported state which should not happen.
    private AuthSignInResult convertSignInResult(SignInResult result) throws AuthException {
        return new AuthSignInResult(
            SignInState.DONE.equals(result.getSignInState()),
            new AuthNextSignInStep(
                    SignInStateConverter.getAuthSignInStep(result.getSignInState()),
                    result.getParameters() == null ? Collections.emptyMap() : result.getParameters(),
                    convertCodeDeliveryDetails(result.getCodeDetails())
            )
        );
    }

    private AuthCodeDeliveryDetails convertCodeDeliveryDetails(UserCodeDeliveryDetails details) {
        return details != null
            ? new AuthCodeDeliveryDetails(
                    details.getDestination(),
                    AuthCodeDeliveryDetails.DeliveryMedium.fromString(details.getDeliveryMedium()),
                    details.getAttributeName())
            : null;
    }

    private AWSCognitoAuthSession buildCognitoAuthSession(UserState state) throws AuthException {
        final AWSCognitoAuthSession.Builder sessionBuilder;
        final CountDownLatch latch = new CountDownLatch(3);

        sessionBuilder = AWSCognitoAuthSession.builder()
            .isSignedIn(state.equals(UserState.SIGNED_IN))
            .identityId(awsMobileClient.getIdentityId());

        awsMobileClient.getTokens(new Callback<Tokens>() {
            @Override
            public void onResult(Tokens result) {
                try {
                    userId = CognitoJWTParser
                            .getPayload(result.getAccessToken().getTokenString())
                            .getString(COGNITO_USER_ID_ATTRIBUTE);
                } catch (JSONException error) {
                    userId = null;
                }

                sessionBuilder.accessToken(result.getAccessToken().getTokenString());
                sessionBuilder.idToken(result.getIdToken().getTokenString());
                sessionBuilder.refreshToken(result.getRefreshToken().getTokenString());
                latch.countDown();
            }

            @Override
            public void onError(Exception exception) {
                userId = null;
                latch.countDown();
            }
        });

        awsMobileClient.getAWSCredentials(new Callback<AWSCredentials>() {
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

        awsMobileClient.getUserAttributes(new Callback<Map<String, String>>() {
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
