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
import com.amplifyframework.auth.AuthChannelEventName;
import com.amplifyframework.auth.AuthCodeDeliveryDetails;
import com.amplifyframework.auth.AuthException;
import com.amplifyframework.auth.AuthPlugin;
import com.amplifyframework.auth.AuthProvider;
import com.amplifyframework.auth.AuthSession;
import com.amplifyframework.auth.AuthUser;
import com.amplifyframework.auth.AuthUserAttribute;
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthSignInOptions;
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthSignUpOptions;
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthWebUISignInOptions;
import com.amplifyframework.auth.cognito.util.AuthProviderConverter;
import com.amplifyframework.auth.cognito.util.SignInStateConverter;
import com.amplifyframework.auth.options.AuthSignInOptions;
import com.amplifyframework.auth.options.AuthSignOutOptions;
import com.amplifyframework.auth.options.AuthSignUpOptions;
import com.amplifyframework.auth.options.AuthWebUISignInOptions;
import com.amplifyframework.auth.result.AuthResetPasswordResult;
import com.amplifyframework.auth.result.AuthSessionResult;
import com.amplifyframework.auth.result.AuthSignInResult;
import com.amplifyframework.auth.result.AuthSignUpResult;
import com.amplifyframework.auth.result.step.AuthNextResetPasswordStep;
import com.amplifyframework.auth.result.step.AuthNextSignInStep;
import com.amplifyframework.auth.result.step.AuthNextSignUpStep;
import com.amplifyframework.auth.result.step.AuthResetPasswordStep;
import com.amplifyframework.auth.result.step.AuthSignInStep;
import com.amplifyframework.auth.result.step.AuthSignUpStep;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.util.UserAgent;

import com.amazonaws.AmazonClientException;
import com.amazonaws.logging.LogFactory;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.HostedUIOptions;
import com.amazonaws.mobile.client.SignInUIOptions;
import com.amazonaws.mobile.client.SignOutOptions;
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
import com.amazonaws.mobileconnectors.cognitoauth.AuthClient;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.util.CognitoJWTParser;
import com.amazonaws.services.cognitoidentityprovider.model.AliasExistsException;
import com.amazonaws.services.cognitoidentityprovider.model.CodeDeliveryFailureException;
import com.amazonaws.services.cognitoidentityprovider.model.CodeMismatchException;
import com.amazonaws.services.cognitoidentityprovider.model.ExpiredCodeException;
import com.amazonaws.services.cognitoidentityprovider.model.InvalidPasswordException;
import com.amazonaws.services.cognitoidentityprovider.model.NotAuthorizedException;
import com.amazonaws.services.cognitoidentityprovider.model.UserNotConfirmedException;
import com.amazonaws.services.cognitoidentityprovider.model.UserNotFoundException;
import com.amazonaws.services.cognitoidentityprovider.model.UsernameExistsException;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A Cognito implementation of the Auth Plugin.
 */
public final class AWSCognitoAuthPlugin extends AuthPlugin<AWSMobileClient> {
    /**
     * The code which the web UI activity is launched under and is needed to listen to the result.
     * See the documentation for more information: https://docs.amplify.aws/lib/auth/signin_web_ui/q/platform/android
     */
    public static final int WEB_UI_SIGN_IN_ACTIVITY_CODE = AuthClient.CUSTOM_TABS_ACTIVITY_CODE;

    private static final String AWS_COGNITO_AUTH_PLUGIN_KEY = "awsCognitoAuthPlugin";
    private static final long SECONDS_BEFORE_TIMEOUT = 10;
    private static final String COGNITO_USER_ID_ATTRIBUTE = "sub";
    private static final String MOBILE_CLIENT_TOKEN_KEY = "token";
    private String userId;
    private AWSMobileClient awsMobileClient;
    private AuthChannelEventName lastEvent;

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
        JSONObject mobileClientConfig;
        LogFactory.setLevel(LogFactory.Level.OFF);

        try {
            mobileClientConfig = new JSONObject(pluginConfiguration.toString());
            mobileClientConfig.put("UserAgentOverride", UserAgent.string());
        } catch (JSONException exception) {
            throw new AuthException("Failed to set user agent string",
                    exception,
                    AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION);
        }

        awsMobileClient.initialize(
            context,
            new AWSConfiguration(mobileClientConfig),
            new Callback<UserStateDetails>() {
                @Override
                public void onResult(UserStateDetails result) {
                    switch (result.getUserState()) {
                        case GUEST:
                        case SIGNED_OUT:
                            lastEvent = AuthChannelEventName.SIGNED_OUT;
                            userId = null;
                            break;
                        case SIGNED_IN:
                            lastEvent = AuthChannelEventName.SIGNED_IN;
                            userId = getUserIdFromToken(result.getDetails().get(MOBILE_CLIENT_TOKEN_KEY));
                            break;
                        case SIGNED_OUT_USER_POOLS_TOKENS_INVALID:
                        case SIGNED_OUT_FEDERATED_TOKENS_INVALID:
                            lastEvent = AuthChannelEventName.SESSION_EXPIRED;
                            userId = getUserIdFromToken(result.getDetails().get(MOBILE_CLIENT_TOKEN_KEY));
                            break;
                        default:
                            userId = null;
                            lastEvent = null;
                    }

                    // Set up a listener to asynchronously update the user id if the user state changes in the future
                    awsMobileClient.addUserStateListener(userStateDetails -> {
                        switch (userStateDetails.getUserState()) {
                            case SIGNED_OUT:
                            case GUEST:
                                userId = null;
                                if (lastEvent != AuthChannelEventName.SIGNED_OUT) {
                                    lastEvent = AuthChannelEventName.SIGNED_OUT;
                                    Amplify.Hub.publish(
                                            HubChannel.AUTH,
                                            HubEvent.create(AuthChannelEventName.SIGNED_OUT)
                                    );
                                }
                                break;
                            case SIGNED_IN:
                                fetchAndSetUserId(() -> { /* No response needed */ });
                                if (lastEvent != AuthChannelEventName.SIGNED_IN) {
                                    lastEvent = AuthChannelEventName.SIGNED_IN;
                                    Amplify.Hub.publish(
                                            HubChannel.AUTH,
                                            HubEvent.create(AuthChannelEventName.SIGNED_IN)
                                    );
                                }
                                break;
                            case SIGNED_OUT_FEDERATED_TOKENS_INVALID:
                            case SIGNED_OUT_USER_POOLS_TOKENS_INVALID:
                                fetchAndSetUserId(() -> { /* No response needed */ });
                                if (lastEvent != AuthChannelEventName.SESSION_EXPIRED) {
                                    lastEvent = AuthChannelEventName.SESSION_EXPIRED;
                                    Amplify.Hub.publish(
                                            HubChannel.AUTH,
                                            HubEvent.create(AuthChannelEventName.SESSION_EXPIRED)
                                    );
                                }
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
                    onSuccess.accept(convertSignUpResult(result, username));
                }

                @Override
                public void onError(Exception error) {
                    if (error instanceof UsernameExistsException) {
                        onException.accept(
                                new AuthException.AWSCognitoAuthException.UsernameExistsException(error.getCause())
                        );
                    } else if (error instanceof AliasExistsException) {
                        onException.accept(
                                new AuthException.AWSCognitoAuthException.AliasExistsException(error.getCause())
                        );
                    } else if (error instanceof AmazonClientException) {
                        onException.accept(
                                new AuthException.AWSCognitoAuthException.NetworkException(error.getCause())
                        );
                    } else {
                        onException.accept(
                                new AuthException("Sign up failed", error, "See attached exception for more details")
                        );
                    }

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
                onSuccess.accept(convertSignUpResult(result, username));
            }

            @Override
            public void onError(Exception error) {
                if (error instanceof UserNotFoundException) {
                    onException.accept(
                            new AuthException.AWSCognitoAuthException.UserNotFoundException(error.getCause())
                    );
                } else if (error instanceof CodeMismatchException) {
                    onException.accept(
                            new AuthException.AWSCognitoAuthException.CodeMismatchException(error.getCause())
                    );
                } else if (error instanceof ExpiredCodeException) {
                    onException.accept(
                            new AuthException.AWSCognitoAuthException.CodeExpiredException(error.getCause())
                    );
                } else if (error instanceof CodeDeliveryFailureException) {
                    onException.accept(
                            new AuthException.AWSCognitoAuthException.CodeDeliveryFailureException(error.getCause())
                    );
                } else if (error instanceof UserNotConfirmedException) {
                    onException.accept(
                            new AuthException.AWSCognitoAuthException.UserNotConfirmedException(error.getCause())
                    );
                } else if (error instanceof AmazonClientException) {
                    onException.accept(
                            new AuthException.AWSCognitoAuthException.NetworkException(error.getCause())
                    );
                } else {
                    onException.accept(
                            new AuthException("Confirm sign up failed", error,
                                            "See attached exception for more details")
                    );
                }
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
                onSuccess.accept(convertSignUpResult(result, username));
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
                if (error instanceof InvalidPasswordException) {
                    onException.accept(
                            new AuthException.AWSCognitoAuthException.InvalidPasswordException(error.getCause())
                    );
                } else if (error instanceof UserNotFoundException) {
                    onException.accept(
                            new AuthException.AWSCognitoAuthException.UserNotFoundException(error.getCause())
                    );
                } else if (error instanceof AmazonClientException) {
                    onException.accept(
                            new AuthException.AWSCognitoAuthException.NetworkException(error.getCause())
                    );
                } else {
                    onException.accept(
                            new AuthException("Sign in failed", error, "See attached exception for more details")
                    );
                }
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
    public void signInWithSocialWebUI(
            @NonNull AuthProvider provider,
            @NonNull Activity callingActivity,
            @NonNull Consumer<AuthSignInResult> onSuccess,
            @NonNull Consumer<AuthException> onException
    ) {
        signInWithSocialWebUI(
                Objects.requireNonNull(provider),
                Objects.requireNonNull(callingActivity),
                AuthWebUISignInOptions.builder().build(),
                Objects.requireNonNull(onSuccess),
                Objects.requireNonNull(onException)
        );
    }

    @Override
    public void signInWithSocialWebUI(
            @NonNull AuthProvider provider,
            @NonNull Activity callingActivity,
            @NonNull AuthWebUISignInOptions options,
            @NonNull Consumer<AuthSignInResult> onSuccess,
            @NonNull Consumer<AuthException> onException
    ) {
        signInWithWebUIHelper(
                Objects.requireNonNull(provider),
                Objects.requireNonNull(callingActivity),
                Objects.requireNonNull(options),
                Objects.requireNonNull(onSuccess),
                Objects.requireNonNull(onException)
        );
    }

    @Override
    public void signInWithWebUI(
            @NonNull Activity callingActivity,
            @NonNull final Consumer<AuthSignInResult> onSuccess,
            @NonNull final Consumer<AuthException> onException
    ) {
        signInWithWebUI(
                Objects.requireNonNull(callingActivity),
                AuthWebUISignInOptions.builder().build(),
                Objects.requireNonNull(onSuccess),
                Objects.requireNonNull(onException)
        );
    }

    @Override
    public void signInWithWebUI(
            @NonNull Activity callingActivity,
            @NonNull AuthWebUISignInOptions options,
            @NonNull Consumer<AuthSignInResult> onSuccess,
            @NonNull Consumer<AuthException> onException
    ) {
        // Note that passing a null value for AuthProvider to the private helper method here is intentional.
        // AuthProvider is a public enum used by customers, and I intentionally don't want to be confusing by adding
        // a value which would represent no specific AuthProvider since that would make the signWithWithSocialWebUI
        // method a duplicate of this method (and not make sense in the context of that method) if chosen.
        signInWithWebUIHelper(
                null,
                Objects.requireNonNull(callingActivity),
                Objects.requireNonNull(options),
                Objects.requireNonNull(onSuccess),
                Objects.requireNonNull(onException)
        );
    }

    @Override
    public void handleWebUISignInResponse(@NonNull Intent intent) {
        awsMobileClient.handleAuthResponse(intent);
    }

    @Override
    public void fetchAuthSession(
            @NonNull Consumer<AuthSession> onSuccess,
            @NonNull Consumer<AuthException> onException
    ) {
        try {
            awsMobileClient.currentUserState(new Callback<UserStateDetails>() {
                @Override
                public void onResult(UserStateDetails result) {
                    switch (result.getUserState()) {
                        case SIGNED_OUT:
                        case GUEST:
                            MobileClientSessionAdapter.fetchSignedOutSession(awsMobileClient, onSuccess);
                            break;
                        case SIGNED_OUT_FEDERATED_TOKENS_INVALID:
                        case SIGNED_OUT_USER_POOLS_TOKENS_INVALID:
                            onSuccess.accept(expiredSession());
                            break;
                        default:
                            MobileClientSessionAdapter.fetchSignedInSession(awsMobileClient, onSuccess);
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
                public void onError(Exception error) {
                    onException.accept(new AuthException(
                            "An error occurred confirming password recovery code",
                            error,
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
            @Nullable Consumer<AuthException> onException
    ) {
        awsMobileClient.changePassword(oldPassword, newPassword, new Callback<Void>() {
            @Override
            public void onResult(Void result) {
                onSuccess.call();
            }

            @Override
            public void onError(Exception error) {
                onException.accept(new AuthException(
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

    @Override
    public void signOut(@NonNull Action onSuccess, @NonNull Consumer<AuthException> onError) {
        signOut(AuthSignOutOptions.builder().globalSignOut(false).build(), onSuccess, onError);
    }

    @Override
    public void signOut(
            @NonNull AuthSignOutOptions options,
            @NonNull Action onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {
        if (options.isGlobalSignOut()) {
            awsMobileClient.signOut(
                    SignOutOptions.builder().signOutGlobally(true).build(),
                    new Callback<Void>() {
                        @Override
                        public void onResult(Void result) {
                            onSuccess.call();
                        }

                        @Override
                        public void onError(Exception error) {
                            // This exception is thrown if the user was signed out globally on another device
                            // already and tries to sign out globally here. In which case, we just complete the
                            // global sign out by locally signing them out here.
                            if (error instanceof NotAuthorizedException) {
                                signOutLocally(onSuccess, onError);
                            } else {
                                // Any other runtime exception means global sign out failed for another reason
                                // (e.g. device offline), in which case we pass that error back to the customer.
                                onError.accept(new AuthException(
                                        "Failed to sign out globally",
                                        error,
                                        "See attached exception for more details"
                                ));
                            }
                        }
                    }
            );
        } else {
            signOutLocally(onSuccess, onError);
        }
    }

    @NonNull
    @Override
    public AWSMobileClient getEscapeHatch() {
        return awsMobileClient;
    }

    private void signOutLocally(@NonNull Action onSuccess, @NonNull Consumer<AuthException> onError) {
        awsMobileClient.signOut(
                SignOutOptions.builder().signOutGlobally(false).invalidateTokens(true).build(),
                new Callback<Void>() {
                    @Override
                    public void onResult(Void result) {
                        onSuccess.call();
                    }

                    @Override
                    public void onError(Exception error) {
                        if (error != null && error.getMessage() != null && error.getMessage().contains("signed-out")) {
                            onError.accept(new AuthException(
                                    "Failed to sign out since Auth is already signed out",
                                    "No need to sign out - you already are!"
                            ));
                        } else {
                            onError.accept(new AuthException(
                                    "Failed to sign out",
                                    error,
                                    "See attached exception for more details"
                            ));
                        }
                    }
            }
        );
    }

    private void signInWithWebUIHelper(
        @Nullable AuthProvider authProvider,
        @NonNull Activity callingActivity,
        @NonNull AuthWebUISignInOptions options,
        @NonNull Consumer<AuthSignInResult> onSuccess,
        @NonNull Consumer<AuthException> onException
    ) {
        HostedUIOptions.Builder optionsBuilder = HostedUIOptions.builder();

        if (options != null) {
            if (options.getScopes() != null) {
                optionsBuilder.scopes(options.getScopes().toArray(new String[options.getScopes().size()]));
            }

            if (!options.getSignInQueryParameters().isEmpty()) {
                optionsBuilder.signInQueryParameters(options.getSignInQueryParameters());
            }

            if (!options.getSignOutQueryParameters().isEmpty()) {
                optionsBuilder.signOutQueryParameters(options.getSignOutQueryParameters());
            }

            if (!options.getTokenQueryParameters().isEmpty()) {
                optionsBuilder.tokenQueryParameters(options.getTokenQueryParameters());
            }

            if (options instanceof AWSCognitoAuthWebUISignInOptions) {
                AWSCognitoAuthWebUISignInOptions cognitoOptions = (AWSCognitoAuthWebUISignInOptions) options;
                optionsBuilder.idpIdentifier(cognitoOptions.getIdpIdentifier())
                        .federationProviderName(cognitoOptions.getFederationProviderName());
            }

            if (authProvider != null) {
                optionsBuilder.identityProvider(AuthProviderConverter.getIdentityProvider(authProvider));
            }
        }

        SignInUIOptions signInUIOptions = SignInUIOptions.builder()
                .hostedUIOptions(optionsBuilder.build())
                .build();

        awsMobileClient.showSignIn(callingActivity, signInUIOptions, new Callback<UserStateDetails>() {
            @Override
            public void onResult(UserStateDetails details) {
                fetchAndSetUserId(() -> onSuccess.accept(
                        new AuthSignInResult(
                                UserState.SIGNED_IN.equals(details.getUserState()),
                                new AuthNextSignInStep(
                                        AuthSignInStep.DONE,
                                        details.getDetails(),
                                        null
                                )
                        )
                ));
            }

            @Override
            public void onError(Exception error) {
                onException.accept(
                        new AuthException(
                                "Sign in with web UI failed",
                                error,
                                "See attached exception for more details"
                        )
                );
            }
        });
    }

    private AuthSession expiredSession() {
        return new AWSCognitoAuthSession(
                true,
                AuthSessionResult.failure(new AuthException.SessionExpiredException()),
                AuthSessionResult.failure(new AuthException.SessionExpiredException()),
                AuthSessionResult.failure(new AuthException.SessionExpiredException()),
                AuthSessionResult.failure(new AuthException.SessionExpiredException())
        );
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

    private AuthSignUpResult convertSignUpResult(@NonNull SignUpResult result, @NonNull String username) {
        UserCodeDeliveryDetails details = Objects.requireNonNull(result).getUserCodeDeliveryDetails();
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
                ),
                result.getUserSub() != null ? new AuthUser(result.getUserSub(), username) : null
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
}
