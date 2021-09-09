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
import com.amplifyframework.auth.AuthDevice;
import com.amplifyframework.auth.AuthException;
import com.amplifyframework.auth.AuthPlugin;
import com.amplifyframework.auth.AuthProvider;
import com.amplifyframework.auth.AuthSession;
import com.amplifyframework.auth.AuthUser;
import com.amplifyframework.auth.AuthUserAttribute;
import com.amplifyframework.auth.AuthUserAttributeKey;
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthConfirmResetPasswordOptions;
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthConfirmSignInOptions;
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthConfirmSignUpOptions;
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthResendSignUpCodeOptions;
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthResendUserAttributeConfirmationCodeOptions;
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthResetPasswordOptions;
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthSignInOptions;
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthSignOutOptions;
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthSignUpOptions;
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthUpdateUserAttributeOptions;
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthUpdateUserAttributesOptions;
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthWebUISignInOptions;
import com.amplifyframework.auth.cognito.util.AuthProviderConverter;
import com.amplifyframework.auth.cognito.util.CognitoAuthExceptionConverter;
import com.amplifyframework.auth.cognito.util.SignInStateConverter;
import com.amplifyframework.auth.options.AuthConfirmResetPasswordOptions;
import com.amplifyframework.auth.options.AuthConfirmSignInOptions;
import com.amplifyframework.auth.options.AuthConfirmSignUpOptions;
import com.amplifyframework.auth.options.AuthResendSignUpCodeOptions;
import com.amplifyframework.auth.options.AuthResendUserAttributeConfirmationCodeOptions;
import com.amplifyframework.auth.options.AuthResetPasswordOptions;
import com.amplifyframework.auth.options.AuthSignInOptions;
import com.amplifyframework.auth.options.AuthSignOutOptions;
import com.amplifyframework.auth.options.AuthSignUpOptions;
import com.amplifyframework.auth.options.AuthUpdateUserAttributeOptions;
import com.amplifyframework.auth.options.AuthUpdateUserAttributesOptions;
import com.amplifyframework.auth.options.AuthWebUISignInOptions;
import com.amplifyframework.auth.result.AuthResetPasswordResult;
import com.amplifyframework.auth.result.AuthSessionResult;
import com.amplifyframework.auth.result.AuthSignInResult;
import com.amplifyframework.auth.result.AuthSignUpResult;
import com.amplifyframework.auth.result.AuthUpdateAttributeResult;
import com.amplifyframework.auth.result.step.AuthNextResetPasswordStep;
import com.amplifyframework.auth.result.step.AuthNextSignInStep;
import com.amplifyframework.auth.result.step.AuthNextSignUpStep;
import com.amplifyframework.auth.result.step.AuthNextUpdateAttributeStep;
import com.amplifyframework.auth.result.step.AuthResetPasswordStep;
import com.amplifyframework.auth.result.step.AuthSignInStep;
import com.amplifyframework.auth.result.step.AuthSignUpStep;
import com.amplifyframework.auth.result.step.AuthUpdateAttributeStep;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.util.UserAgent;

import com.amazonaws.logging.LogFactory;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.HostedUIOptions;
import com.amazonaws.mobile.client.SignInUIOptions;
import com.amazonaws.mobile.client.SignOutOptions;
import com.amazonaws.mobile.client.UserState;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.mobile.client.results.Device;
import com.amazonaws.mobile.client.results.ForgotPasswordResult;
import com.amazonaws.mobile.client.results.ForgotPasswordState;
import com.amazonaws.mobile.client.results.ListDevicesResult;
import com.amazonaws.mobile.client.results.SignInResult;
import com.amazonaws.mobile.client.results.SignInState;
import com.amazonaws.mobile.client.results.SignUpResult;
import com.amazonaws.mobile.client.results.Tokens;
import com.amazonaws.mobile.client.results.UserCodeDeliveryDetails;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.cognitoauth.AuthClient;
import com.amazonaws.mobileconnectors.cognitoauth.exceptions.AuthNavigationException;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.util.CognitoJWTParser;
import com.amazonaws.services.cognitoidentityprovider.model.NotAuthorizedException;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
                                fetchAndSetUserId(() -> {
                                    if (lastEvent != AuthChannelEventName.SIGNED_IN) {
                                        lastEvent = AuthChannelEventName.SIGNED_IN;
                                        Amplify.Hub.publish(
                                                HubChannel.AUTH,
                                                HubEvent.create(AuthChannelEventName.SIGNED_IN)
                                        );
                                    }
                                });
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
        Map<String, String> clientMetadata = new HashMap<>();

        if (options.getUserAttributes() != null) {
            for (AuthUserAttribute attribute : options.getUserAttributes()) {
                userAttributes.put(attribute.getKey().getKeyString(), attribute.getValue());
            }
        }

        if (options instanceof AWSCognitoAuthSignUpOptions) {
            validationData = ((AWSCognitoAuthSignUpOptions) options).getValidationData();
            clientMetadata = ((AWSCognitoAuthSignUpOptions) options).getClientMetadata();
        }

        awsMobileClient.signUp(
            username,
            password,
            userAttributes,
            validationData,
            clientMetadata,
            new Callback<SignUpResult>() {
                @Override
                public void onResult(SignUpResult result) {
                    onSuccess.accept(convertSignUpResult(result, username));
                }

                @Override
                public void onError(Exception error) {
                    onException.accept(
                           CognitoAuthExceptionConverter.lookup(error, "Sign up failed")
                    );
                }
            }
        );
    }

    @Override
    public void confirmSignUp(
            @NonNull String username,
            @NonNull String confirmationCode,
            @NonNull AuthConfirmSignUpOptions options,
            @NonNull final Consumer<AuthSignUpResult> onSuccess,
            @NonNull final Consumer<AuthException> onException
    ) {
        final Map<String, String> clientMetadata = new HashMap<>();

        if (options instanceof AWSCognitoAuthConfirmSignUpOptions) {
            AWSCognitoAuthConfirmSignUpOptions cognitoOptions = (AWSCognitoAuthConfirmSignUpOptions) options;
            clientMetadata.putAll(cognitoOptions.getClientMetadata());
        }

        awsMobileClient.confirmSignUp(username, confirmationCode, clientMetadata, new Callback<SignUpResult>() {
            @Override
            public void onResult(SignUpResult result) {
                onSuccess.accept(convertSignUpResult(result, username));
            }

            @Override
            public void onError(Exception error) {
                onException.accept(
                        CognitoAuthExceptionConverter.lookup(error, "Confirm sign up failed")
                );
            }
        });
    }

    @Override
    public void confirmSignUp(
            @NonNull String username,
            @NonNull String confirmationCode,
            @NonNull final Consumer<AuthSignUpResult> onSuccess,
            @NonNull final Consumer<AuthException> onException
    ) {
        confirmSignUp(username, confirmationCode, AuthConfirmSignUpOptions.defaults(), onSuccess, onException);
    }

    @Override
    public void resendSignUpCode(
            @NonNull String username,
            @NonNull AuthResendSignUpCodeOptions options,
            @NonNull Consumer<AuthSignUpResult> onSuccess,
            @NonNull Consumer<AuthException> onException
    ) {
        final Map<String, String> clientMetadata = new HashMap<>();

        if (options instanceof AWSCognitoAuthResendSignUpCodeOptions) {
            AWSCognitoAuthResendSignUpCodeOptions cognitoOptions = (AWSCognitoAuthResendSignUpCodeOptions) options;
            clientMetadata.putAll(cognitoOptions.getMetadata());
        }
        awsMobileClient.resendSignUp(username, clientMetadata, new Callback<SignUpResult>() {
            @Override
            public void onResult(SignUpResult result) {
                onSuccess.accept(convertSignUpResult(result, username));
            }

            @Override
            public void onError(Exception error) {
                onException.accept(
                        CognitoAuthExceptionConverter.lookup(
                                error, "Resend confirmation code failed")
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
        resendSignUpCode(username, AuthResendSignUpCodeOptions.defaults(), onSuccess, onException);
    }

    @Override
    public void signIn(
        @Nullable String username,
        @Nullable String password,
        @NonNull AuthSignInOptions options,
        @NonNull final Consumer<AuthSignInResult> onSuccess,
        @NonNull final Consumer<AuthException> onException
    ) {
        final Map<String, String> metadata = new HashMap<>();
        if (options instanceof AWSCognitoAuthSignInOptions) {
            metadata.putAll(((AWSCognitoAuthSignInOptions) options).getMetadata());
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
                        CognitoAuthExceptionConverter.lookup(error, "Sign in failed")
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
        signIn(username, password, AuthSignInOptions.defaults(), onSuccess, onException);
    }

    @Override
    public void confirmSignIn(
            @NonNull String confirmationCode,
            @NonNull AuthConfirmSignInOptions options,
            @NonNull Consumer<AuthSignInResult> onSuccess,
            @NonNull Consumer<AuthException> onException
    ) {
        final Map<String, String> metadata = new HashMap<>();
        final Map<String, String> userAttributes = new HashMap<>();
        if (options instanceof AWSCognitoAuthConfirmSignInOptions) {
            metadata.putAll(((AWSCognitoAuthConfirmSignInOptions) options).getMetadata());
            for (AuthUserAttribute attribute : ((AWSCognitoAuthConfirmSignInOptions) options).getUserAttributes()) {
                userAttributes.put(attribute.getKey().getKeyString(), attribute.getValue());
            }

        }

        awsMobileClient.confirmSignIn(confirmationCode, metadata, userAttributes, new Callback<SignInResult>() {
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
                        CognitoAuthExceptionConverter.lookup(
                                error, "Confirm sign in failed")
                );
            }
        });
    }

    @Override
    public void confirmSignIn(
            @NonNull String confirmationCode,
            @NonNull Consumer<AuthSignInResult> onSuccess,
            @NonNull Consumer<AuthException> onException
    ) {
        confirmSignIn(confirmationCode, AuthConfirmSignInOptions.defaults(), onSuccess, onException);
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
                    onException.accept(CognitoAuthExceptionConverter.lookup(
                            exception, "Fetching authorization session failed."));
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
    public void rememberDevice(
            @NonNull Action onSuccess,
            @NonNull Consumer<AuthException> onException
    ) {
        awsMobileClient.getDeviceOperations().updateStatus(true, new Callback<Void>() {
            @Override
            public void onResult(Void result) {
                onSuccess.call();
            }

            @Override
            public void onError(Exception exception) {
                onException.accept(CognitoAuthExceptionConverter.lookup(
                        exception, "Remember device failed."));
            }
        });
    }

    @Override
    public void forgetDevice(
            @NonNull Action onSuccess,
            @NonNull Consumer<AuthException> onException
    ) {
        awsMobileClient.getDeviceOperations().forget(new Callback<Void>() {
            @Override
            public void onResult(Void result) {
                onSuccess.call();
            }

            @Override
            public void onError(Exception exception) {
                onException.accept(CognitoAuthExceptionConverter.lookup(
                        exception, "Forget device failed."));
            }
        });
    }

    @Override
    public void forgetDevice(
            @NonNull AuthDevice device,
            @NonNull Action onSuccess,
            @NonNull Consumer<AuthException> onException
    ) {
        awsMobileClient.getDeviceOperations().forget(device.getDeviceId(), new Callback<Void>() {
            @Override
            public void onResult(Void result) {
                onSuccess.call();
            }

            @Override
            public void onError(Exception exception) {
                onException.accept(CognitoAuthExceptionConverter.lookup(
                        exception, "Forget device failed."));
            }
        });
    }

    @Override
    public void fetchDevices(
            @NonNull Consumer<List<AuthDevice>> onSuccess,
            @NonNull Consumer<AuthException> onException
    ) {
        awsMobileClient.getDeviceOperations().list(new Callback<ListDevicesResult>() {
            @Override
            public void onResult(ListDevicesResult result) {
                List<AuthDevice> devices = new ArrayList<>();
                for (Device device : result.getDevices()) {
                    devices.add(AuthDevice.fromId(device.getDeviceKey()));
                }
                onSuccess.accept(devices);
            }

            @Override
            public void onError(Exception exception) {
                onException.accept(CognitoAuthExceptionConverter.lookup(
                        exception, "Fetching devices failed."));
            }
        });
    }

    @Override
    public void resetPassword(
            @NonNull String username,
            @NonNull AuthResetPasswordOptions options,
            @NonNull Consumer<AuthResetPasswordResult> onSuccess,
            @NonNull Consumer<AuthException> onException
    ) {
        final Map<String, String> clientMetadata = new HashMap<>();

        if (options instanceof AWSCognitoAuthResetPasswordOptions) {
            AWSCognitoAuthResetPasswordOptions cognitoOptions = (AWSCognitoAuthResetPasswordOptions) options;
            clientMetadata.putAll(cognitoOptions.getMetadata());
        }
        awsMobileClient.forgotPassword(username, clientMetadata, new Callback<ForgotPasswordResult>() {
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
                onException.accept(CognitoAuthExceptionConverter.lookup(
                        exception, "Reset password failed."));
            }
        });
    }

    @Override
    public void resetPassword(
            @NonNull String username,
            @NonNull Consumer<AuthResetPasswordResult> onSuccess,
            @NonNull Consumer<AuthException> onException
    ) {
        resetPassword(username, AuthResetPasswordOptions.defaults(), onSuccess, onException);
    }

    @Override
    public void confirmResetPassword(
            @NonNull String newPassword,
            @NonNull String confirmationCode,
            @NonNull AuthConfirmResetPasswordOptions options,
            @NonNull Action onSuccess,
            @NonNull Consumer<AuthException> onException
    ) {
        final Map<String, String> clientMetadata = new HashMap<>();

        if (options instanceof AWSCognitoAuthConfirmResetPasswordOptions) {
            AWSCognitoAuthConfirmResetPasswordOptions cognitoOptions =
                    (AWSCognitoAuthConfirmResetPasswordOptions) options;
            clientMetadata.putAll(cognitoOptions.getMetadata());
        }
        awsMobileClient.confirmForgotPassword(
            newPassword,
            confirmationCode,
            clientMetadata,
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
                    onException.accept(CognitoAuthExceptionConverter.lookup(
                            error, "Confirm reset password failed."));
                }
            }
        );
    }

    @Override
    public void confirmResetPassword(
            @NonNull String newPassword,
            @NonNull String confirmationCode,
            @NonNull Action onSuccess,
            @NonNull Consumer<AuthException> onException
    ) {
        confirmResetPassword(
                newPassword,
                confirmationCode,
                AuthConfirmResetPasswordOptions.defaults(),
                onSuccess,
                onException
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
            public void onError(Exception exception) {
                onException.accept(CognitoAuthExceptionConverter.lookup(
                        exception, "Update password failed."));
            }
        });
    }

    @Override
    public void fetchUserAttributes(
            @NonNull Consumer<List<AuthUserAttribute>> onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {
        awsMobileClient.getUserAttributes(new Callback<Map<String, String>>() {
            @Override
            public void onResult(Map<String, String> result) {
                List<AuthUserAttribute> userAttributes = new ArrayList<>();
                for (Map.Entry<String, String> entry : result.entrySet()) {
                    userAttributes.add(new AuthUserAttribute(
                            AuthUserAttributeKey.custom(entry.getKey()), entry.getValue()));
                }
                onSuccess.accept(userAttributes);
            }

            @Override
            public void onError(Exception error) {
                onError.accept(CognitoAuthExceptionConverter.lookup(
                        error, "Fetching user attributes failed."));
            }
        });
    }

    @Override
    public void updateUserAttribute(
            @NonNull AuthUserAttribute attribute,
            @NonNull AuthUpdateUserAttributeOptions options,
            @NonNull Consumer<AuthUpdateAttributeResult> onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {

        final Map<String, String> clientMetadata = new HashMap<>();
        if (options instanceof AWSCognitoAuthUpdateUserAttributeOptions) {
            AWSCognitoAuthUpdateUserAttributeOptions cognitoOptions =
                    (AWSCognitoAuthUpdateUserAttributeOptions) options;
            clientMetadata.putAll(cognitoOptions.getMetadata());
        }

        awsMobileClient.updateUserAttributes(
                Collections.singletonMap(attribute.getKey().getKeyString(), attribute.getValue()),
                clientMetadata,
                new Callback<List<UserCodeDeliveryDetails>>() {
                    @Override
                    public void onResult(List<UserCodeDeliveryDetails> result) {
                        if (result.size() == 0) {
                            onSuccess.accept(new AuthUpdateAttributeResult(
                                    true,
                                    new AuthNextUpdateAttributeStep(
                                            AuthUpdateAttributeStep.DONE,
                                            Collections.emptyMap(),
                                            null)
                            ));
                        } else {
                            onSuccess.accept(new AuthUpdateAttributeResult(
                                    true,
                                    new AuthNextUpdateAttributeStep(
                                            AuthUpdateAttributeStep.CONFIRM_ATTRIBUTE_WITH_CODE,
                                            Collections.emptyMap(),
                                            convertCodeDeliveryDetails(result.get(0)))
                            ));
                        }
                    }

                    @Override
                    public void onError(Exception error) {
                        onError.accept(new AuthException(
                                "Failed to update user attributes",
                                error,
                                "See attached exception for more details"
                        ));
                    }
                }
        );
    }

    @Override
    public void updateUserAttribute(
            @NonNull AuthUserAttribute attribute,
            @NonNull Consumer<AuthUpdateAttributeResult> onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {
        updateUserAttribute(attribute, AuthUpdateUserAttributeOptions.defaults(), onSuccess, onError);
    }

    @Override
    public void updateUserAttributes(
            @NonNull List<AuthUserAttribute> attributes,
            @NonNull AuthUpdateUserAttributesOptions options,
            @NonNull Consumer<Map<AuthUserAttributeKey, AuthUpdateAttributeResult>> onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {

        final Map<String, String> clientMetadata = new HashMap<>();
        if (options instanceof AWSCognitoAuthUpdateUserAttributesOptions) {
            AWSCognitoAuthUpdateUserAttributesOptions cognitoOptions =
                    (AWSCognitoAuthUpdateUserAttributesOptions) options;
            clientMetadata.putAll(cognitoOptions.getMetadata());
        }

        Map<String, String> attributesMap = new HashMap<>();
        for (AuthUserAttribute attribute : attributes) {
            attributesMap.put(attribute.getKey().getKeyString(), attribute.getValue());
        }

        awsMobileClient.updateUserAttributes(
                attributesMap,
                clientMetadata,
                new Callback<List<UserCodeDeliveryDetails>>() {
                    @Override
                    public void onResult(List<UserCodeDeliveryDetails> result) {
                        Map<String, UserCodeDeliveryDetails> codeDetailsMap = new HashMap<>();
                        Map<AuthUserAttributeKey, AuthUpdateAttributeResult> resultMap = new HashMap<>();

                        for (UserCodeDeliveryDetails details : result) {
                            codeDetailsMap.put(details.getAttributeName(), details);
                        }

                        for (String attributeKey : attributesMap.keySet()) {
                            if (codeDetailsMap.containsKey(attributeKey)) {
                                resultMap.put(AuthUserAttributeKey.custom(attributeKey),
                                        new AuthUpdateAttributeResult(
                                                true,
                                                new AuthNextUpdateAttributeStep(
                                                        AuthUpdateAttributeStep.
                                                                CONFIRM_ATTRIBUTE_WITH_CODE,
                                                        Collections.emptyMap(),
                                                        convertCodeDeliveryDetails(codeDetailsMap.get(attributeKey)))
                                        ));
                            } else {
                                resultMap.put(AuthUserAttributeKey.custom(attributeKey),
                                        new AuthUpdateAttributeResult(
                                                true,
                                                new AuthNextUpdateAttributeStep(
                                                        AuthUpdateAttributeStep.
                                                                DONE,
                                                        Collections.emptyMap(),
                                                        null)
                                        ));
                            }
                        }
                        onSuccess.accept(resultMap);
                    }

                    @Override
                    public void onError(Exception error) {
                        onError.accept(new AuthException(
                                "Failed to update user attributes",
                                error,
                                "See attached exception for more details"
                        ));
                    }
                });
    }

    @Override
    public void updateUserAttributes(
            @NonNull List<AuthUserAttribute> attributes,
            @NonNull Consumer<Map<AuthUserAttributeKey, AuthUpdateAttributeResult>> onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {
        updateUserAttributes(
                attributes,
                AuthUpdateUserAttributesOptions.defaults(),
                onSuccess,
                onError
        );
    }

    @Override
    public void resendUserAttributeConfirmationCode(
            @NonNull AuthUserAttributeKey attributeKey,
            @NonNull AuthResendUserAttributeConfirmationCodeOptions options,
            @NonNull Consumer<AuthCodeDeliveryDetails> onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {

        final Map<String, String> clientMetadata = new HashMap<>();
        if (options instanceof AWSCognitoAuthResendUserAttributeConfirmationCodeOptions) {
            AWSCognitoAuthResendUserAttributeConfirmationCodeOptions cognitoOptions =
                    (AWSCognitoAuthResendUserAttributeConfirmationCodeOptions) options;
            clientMetadata.putAll(cognitoOptions.getMetadata());
        }

        String attributeName = attributeKey.getKeyString();
        awsMobileClient.verifyUserAttribute(
                attributeName,
                clientMetadata,
                new Callback<UserCodeDeliveryDetails>() {
                    @Override
                    public void onResult(UserCodeDeliveryDetails result) {
                        onSuccess.accept(convertCodeDeliveryDetails(result));
                    }

                    @Override
                    public void onError(Exception error) {
                        onError.accept(new AuthException(
                                "Failed to resend user attribute confirmation code",
                                error,
                                "See attached exception for more details"
                        ));
                    }
                });
    }

    @Override
    public void resendUserAttributeConfirmationCode(
            @NonNull AuthUserAttributeKey attributeKey,
            @NonNull Consumer<AuthCodeDeliveryDetails> onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {
        resendUserAttributeConfirmationCode(
                attributeKey,
                AuthResendUserAttributeConfirmationCodeOptions.defaults(),
                onSuccess,
                onError
        );
    }

    @Override
    public void confirmUserAttribute(
            @NonNull AuthUserAttributeKey attributeKey,
            @NonNull String confirmationCode,
            @NonNull Action onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {

        awsMobileClient.confirmUpdateUserAttribute(
                attributeKey.getKeyString(),
                confirmationCode,
                new Callback<Void>() {
                    @Override
                    public void onResult(Void result) {
                        onSuccess.call();
                    }

                    @Override
                    public void onError(Exception error) {
                        onError.accept(CognitoAuthExceptionConverter.lookup(
                                error, "Confirming user attributes failed."));
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
                                signOutLocally(options, onSuccess, onError);
                            } else if (error instanceof AuthNavigationException) {
                                // User cancelled the sign-out screen.
                                onError.accept(new AuthException.UserCancelledException(
                                    "The user cancelled the sign-out attempt.", error,
                                    "To recover, catch this error, and retry sign-out."
                                ));
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
            signOutLocally(options, onSuccess, onError);
        }
    }

    @NonNull
    @Override
    public AWSMobileClient getEscapeHatch() {
        return awsMobileClient;
    }

    @NonNull
    @Override
    public String getVersion() {
        return BuildConfig.VERSION_NAME;
    }

    private void signOutLocally(
            @NonNull AuthSignOutOptions options,
            @NonNull Action onSuccess,
            @NonNull Consumer<AuthException> onError) {
        SignOutOptions.Builder signOutOptionsBuilder =
                SignOutOptions.builder().signOutGlobally(false).invalidateTokens(true);

        if (options instanceof AWSCognitoAuthSignOutOptions) {
            signOutOptionsBuilder.browserPackage(((AWSCognitoAuthSignOutOptions) options).getBrowserPackage());
        }

        awsMobileClient.signOut(
                signOutOptionsBuilder.build(),
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
                        } else if (error instanceof AuthNavigationException) {
                            // User cancelled the sign-out screen.
                            onError.accept(new AuthException.UserCancelledException(
                                "The user cancelled the sign-out attempt.", error,
                                "To recover, catch this error, and retry sign-out."
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
        SignInUIOptions.Builder signInUIOptionsBuilder = SignInUIOptions.builder();

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
                signInUIOptionsBuilder.browserPackage(cognitoOptions.getBrowserPackage());
            }

            if (authProvider != null) {
                optionsBuilder.identityProvider(AuthProviderConverter.getIdentityProvider(authProvider));
            }
        }

        SignInUIOptions signInUIOptions = signInUIOptionsBuilder
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
                if (error instanceof AuthNavigationException) {
                    onException.accept(new AuthException.UserCancelledException(
                        "The user cancelled the sign-in attempt, so it did not complete.",
                        error, "To recover: catch this error, and show the sign-in screen again."
                    ));
                } else {
                    onException.accept(new AuthException(
                        "Sign-in with web UI failed", error,
                        "See attached exception for more details"
                    ));
                }
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
