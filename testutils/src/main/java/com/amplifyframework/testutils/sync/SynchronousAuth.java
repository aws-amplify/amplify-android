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

package com.amplifyframework.testutils.sync;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.auth.AuthCategoryBehavior;
import com.amplifyframework.auth.AuthCodeDeliveryDetails;
import com.amplifyframework.auth.AuthDevice;
import com.amplifyframework.auth.AuthException;
import com.amplifyframework.auth.AuthPlugin;
import com.amplifyframework.auth.AuthProvider;
import com.amplifyframework.auth.AuthSession;
import com.amplifyframework.auth.AuthUser;
import com.amplifyframework.auth.AuthUserAttribute;
import com.amplifyframework.auth.AuthUserAttributeKey;
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
import com.amplifyframework.auth.result.AuthSignInResult;
import com.amplifyframework.auth.result.AuthSignUpResult;
import com.amplifyframework.auth.result.AuthUpdateAttributeResult;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.plugin.Plugin;
import com.amplifyframework.logging.AndroidLoggingPlugin;
import com.amplifyframework.logging.LogLevel;
import com.amplifyframework.testutils.Await;
import com.amplifyframework.testutils.VoidResult;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * A utility to perform synchronous calls to the {@link com.amplifyframework.auth.AuthCategory}.
 * This code is not well suited for production use, but is useful in test
 * code, where we want to make a series of sequential assertions after
 * performing various operations.
 */
public final class SynchronousAuth {
    private static final long AUTH_OPERATION_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(10);
    private final AuthCategoryBehavior asyncDelegate;

    private SynchronousAuth(AuthCategoryBehavior asyncDelegate) {
        this.asyncDelegate = asyncDelegate;
    }

    /**
     * Creates a synchronous auth wrapper which delegates calls to the provided auth
     * category behavior.
     *
     * @param asyncDelegate Performs the actual auth operations
     * @return A synchronous auth wrapper
     */
    @NonNull
    public static SynchronousAuth delegatingTo(@NonNull AuthCategoryBehavior asyncDelegate) {
        Objects.requireNonNull(asyncDelegate);
        return new SynchronousAuth(asyncDelegate);
    }

    /**
     * Creates a synchronous auth wrapper which delegates to the {@link Amplify#Auth} facade.
     *
     * @return A synchronous auth wrapper
     */
    @NonNull
    public static SynchronousAuth delegatingToAmplify() {
        return new SynchronousAuth(Amplify.Auth);
    }

    /**
     * Creates a synchronous auth wrapper which delegates to the AWSCognitoPlugin.
     *
     * @param context Application context
     * @param authPlugin to delegate auth
     * @return A synchronous auth wrapper
     * @throws AmplifyException exception during configuring Amplify
     * @throws InterruptedException exception during thread interruption
     */
    public static SynchronousAuth delegatingToCognito(Context context, Plugin<?> authPlugin)
        throws AmplifyException, InterruptedException {
        try {
            Amplify.Auth.addPlugin((AuthPlugin<?>) authPlugin);
            Amplify.Logging.addPlugin(new AndroidLoggingPlugin(LogLevel.DEBUG));
            Amplify.configure(context);
        } catch (Exception exception) {
            Log.i("SynchronousAuth", "Amplify already called", exception);
        }
        //TODO: make authCategory confiuration synchronous
        Thread.sleep(AUTH_OPERATION_TIMEOUT_MS);
        return SynchronousAuth.delegatingTo(Amplify.Auth);
    }

    /**
     * Creates a new user account synchronously.
     * @param username A login identifier e.g. `superdog22`; or an email/phone number, depending on configuration
     * @param password The user's password
     * @param options Advanced options such as additional attributes of the user or validation data
     * @return result object
     * @throws AuthException exception
     */
    @NonNull
    public AuthSignUpResult signUp(
            @NonNull String username,
            @NonNull String password,
            @NonNull AuthSignUpOptions options
    ) throws AuthException {
        return Await.<AuthSignUpResult, AuthException>result(AUTH_OPERATION_TIMEOUT_MS, (onResult, onError) ->
                asyncDelegate.signUp(username, password, options, onResult, onError)
        );
    }

    /**
     * Confirm sign up synchronously.
     * @param username A login identifier e.g. `superdog22`; or an email/phone number, depending on configuration
     * @param confirmationCode The confirmation code the user received
     * @param options Advanced options such as a map of auth information for custom auth
     * @return result object
     * @throws AuthException exception
     */
    @NonNull
    public AuthSignUpResult confirmSignUp(
            @NonNull String username,
            @NonNull String confirmationCode,
            @NonNull AuthConfirmSignUpOptions options
    ) throws AuthException {
        return Await.<AuthSignUpResult, AuthException>result(AUTH_OPERATION_TIMEOUT_MS, (onResult, onError) ->
                asyncDelegate.confirmSignUp(username, confirmationCode, options, onResult, onError)
        );
    }

    /**
     * Confirm sign up synchronously.
     * @param username A login identifier e.g. `superdog22`; or an email/phone number, depending on configuration
     * @param confirmationCode The confirmation code the user received
     * @return result object
     * @throws AuthException exception
     */
    @NonNull
    public AuthSignUpResult confirmSignUp(
            @NonNull String username,
            @NonNull String confirmationCode
    ) throws AuthException {
        return Await.<AuthSignUpResult, AuthException>result(AUTH_OPERATION_TIMEOUT_MS, (onResult, onError) ->
                asyncDelegate.confirmSignUp(username, confirmationCode, onResult, onError)
        );
    }

    /**
     * Resend sign up code synchronously.
     * @param username A login identifier e.g. `superdog22`; or an email/phone number, depending on configuration
     * @param options Advanced options such as a map of auth information for custom auth
     * @return result object
     * @throws AuthException exception
     */
    @NonNull
    public AuthCodeDeliveryDetails resendSignUpCode(
            @NonNull String username,
            @NonNull AuthResendSignUpCodeOptions options
    ) throws AuthException {
        return Await.<AuthCodeDeliveryDetails, AuthException>result(AUTH_OPERATION_TIMEOUT_MS, (onResult, onError) ->
                asyncDelegate.resendSignUpCode(username, options, onResult, onError)
        );
    }

    /**
     * Resend sign up code synchronously.
     * @param username A login identifier e.g. `superdog22`; or an email/phone number, depending on configuration
     * @return result object
     * @throws AuthException exception
     */
    @NonNull
    public AuthCodeDeliveryDetails resendSignUpCode(
            @NonNull String username
    ) throws AuthException {
        return Await.<AuthCodeDeliveryDetails, AuthException>result(AUTH_OPERATION_TIMEOUT_MS, (onResult, onError) ->
                asyncDelegate.resendSignUpCode(username, onResult, onError)
        );
    }

    /**
     * Sign in synchronously.
     * @param username A login identifier e.g. `superdog22`; or an email/phone number, depending on configuration
     * @param password User's password for normal signup, null if custom auth or passwordless configurations are setup
     * @param options Advanced options such as a map of auth information for custom auth
     * @return result object
     * @throws AuthException exception
     */
    @NonNull
    public AuthSignInResult signIn(
            @Nullable String username,
            @Nullable String password,
            @NonNull AuthSignInOptions options
    ) throws AuthException {
        return Await.<AuthSignInResult, AuthException>result(AUTH_OPERATION_TIMEOUT_MS, (onResult, onError) ->
                asyncDelegate.signIn(username, password, options, onResult, onError)
        );
    }

    /**
     * Sign in synchronously.
     * @param username A login identifier e.g. `superdog22`; or an email/phone number, depending on configuration
     * @param password User's password for normal signup, null if custom auth or passwordless configurations are setup
     * @return result object
     * @throws AuthException exception
     */
    @NonNull
    public AuthSignInResult signIn(
            @Nullable String username,
            @Nullable String password
    ) throws AuthException {
        return Await.<AuthSignInResult, AuthException>result(AUTH_OPERATION_TIMEOUT_MS, (onResult, onError) ->
                asyncDelegate.signIn(username, password, onResult, onError)
        );
    }

    /**
     * Confirm sign in synchronously.
     * @param challengeResponse The code received as part of the multi-factor authentication process
     * @param options Advanced options such as a map of auth information for custom auth
     * @return result object
     * @throws AuthException exception
     */
    @NonNull
    public AuthSignInResult confirmSignIn(
            @NonNull String challengeResponse,
            @NonNull AuthConfirmSignInOptions options
    ) throws AuthException {
        return Await.<AuthSignInResult, AuthException>result(AUTH_OPERATION_TIMEOUT_MS, (onResult, onError) ->
                asyncDelegate.confirmSignIn(challengeResponse, options, onResult, onError)
        );
    }

    /**
     * Confirm sign in synchronously.
     * @param challengeResponse The code received as part of the multi-factor authentication process
     * @return result object
     * @throws AuthException exception
     */
    @NonNull
    public AuthSignInResult confirmSignIn(
            @NonNull String challengeResponse
    ) throws AuthException {
        return Await.<AuthSignInResult, AuthException>result(AUTH_OPERATION_TIMEOUT_MS, (onResult, onError) ->
                asyncDelegate.confirmSignIn(challengeResponse, onResult, onError)
        );
    }

    /**
     * Social web UI sign in synchronously.
     * @param provider The auth provider you want to launch the web ui for (e.g. Facebook, Google, etc.)
     * @param callingActivity The activity in your app you are calling this from
     * @return result object
     * @throws AuthException exception
     */
    @NonNull
    public AuthSignInResult signInWithSocialWebUI(
            @NonNull AuthProvider provider,
            @NonNull Activity callingActivity
    ) throws AuthException {
        return Await.<AuthSignInResult, AuthException>result(AUTH_OPERATION_TIMEOUT_MS, (onResult, onError) ->
                asyncDelegate.signInWithSocialWebUI(provider, callingActivity, onResult, onError)
        );
    }

    /**
     * Web UI sign in synchronously.
     * @param callingActivity The activity in your app you are calling this from
     * @param options Advanced options for signing in with an auth provider's hosted web ui.
     * @return result object
     * @throws AuthException exception
     */
    @NonNull
    public AuthSignInResult signInWithWebUI(
            @NonNull Activity callingActivity,
            @NonNull AuthWebUISignInOptions options
    ) throws AuthException {
        return Await.<AuthSignInResult, AuthException>result(AUTH_OPERATION_TIMEOUT_MS, (onResult, onError) ->
                asyncDelegate.signInWithWebUI(callingActivity, options, onResult, onError)
        );
    }

    /**
     * Reset password synchronously.
     * @param username A login identifier e.g. `superdog22`; or an email/phone number, depending on configuration
     * @param options Advanced options such as a map of auth information for custom auth
     * @return result object
     * @throws AuthException exception
     */
    @NonNull
    public AuthResetPasswordResult resetPassword(
            @NonNull String username,
            @NonNull AuthResetPasswordOptions options
    ) throws AuthException {
        return Await.<AuthResetPasswordResult, AuthException>result(AUTH_OPERATION_TIMEOUT_MS, (onResult, onError) ->
                asyncDelegate.resetPassword(username, options, onResult, onError)
        );
    }

    /**
     * Reset password synchronously.
     * @param username A login identifier e.g. `superdog22`; or an email/phone number, depending on configuration
     * @return result object
     * @throws AuthException exception
     */
    @NonNull
    public AuthResetPasswordResult resetPassword(
            @NonNull String username
    ) throws AuthException {
        return Await.<AuthResetPasswordResult, AuthException>result(AUTH_OPERATION_TIMEOUT_MS, (onResult, onError) ->
                asyncDelegate.resetPassword(username, onResult, onError)
        );
    }

    /**
     * Complete password recovery process by inputting user's desired new password and confirmation code.
     * @param username A login identifier e.g. `superdog22`; or an email/phone number, depending on configuration
     * @param newPassword The user's desired new password
     * @param confirmationCode The confirmation code the user received after starting the forgotPassword process
     * @param options Advanced options such as a map of auth information for custom auth
     * @throws AuthException exception
     */
    public void confirmResetPassword(
            @NonNull String username,
            @NonNull String newPassword,
            @NonNull String confirmationCode,
            @NonNull AuthConfirmResetPasswordOptions options
    ) throws AuthException {
        Await.<Object, AuthException>result(AUTH_OPERATION_TIMEOUT_MS, (onResult, onError) ->
            asyncDelegate.confirmResetPassword(
                username,
                newPassword,
                confirmationCode,
                options,
                () -> onResult.accept(VoidResult.instance()),
                onError
            )
        );
    }

    /**
     * Complete password recovery process by inputting user's desired new password and confirmation code.
     * @param username A login identifier e.g. `superdog22`; or an email/phone number, depending on configuration
     * @param newPassword The user's desired new password
     * @param confirmationCode The confirmation code the user received after starting the forgotPassword process
     * @throws AuthException exception
     */
    public void confirmResetPassword(
            @NonNull String username,
            @NonNull String newPassword,
            @NonNull String confirmationCode
    ) throws AuthException {
        Await.<Object, AuthException>result(AUTH_OPERATION_TIMEOUT_MS, (onResult, onError) ->
            asyncDelegate.confirmResetPassword(
                username,
                newPassword,
                confirmationCode,
                () -> onResult.accept(VoidResult.instance()),
                onError
            )
        );
    }

    /**
     * Fetch auth session synchronously.
     * @return result object
     * @throws AuthException exception
     */
    @NonNull
    public AuthSession fetchAuthSession() throws AuthException {
        return Await.<AuthSession, AuthException>result(AUTH_OPERATION_TIMEOUT_MS, asyncDelegate::fetchAuthSession);
    }

    /**
     * Remember current device synchronously.
     * @throws AuthException exception
     */
    public void rememberDevice() throws AuthException {
        Await.<Object, AuthException>result(AUTH_OPERATION_TIMEOUT_MS, (onResult, onError) ->
                asyncDelegate.rememberDevice(() -> onResult.accept(VoidResult.instance()), onError)
        );
    }

    /**
     * Forget the current device synchronously.
     * @throws AuthException exception
     */
    public void forgetDevice() throws AuthException {
        Await.<Object, AuthException>result(AUTH_OPERATION_TIMEOUT_MS, (onResult, onError) ->
                asyncDelegate.forgetDevice(() -> onResult.accept(VoidResult.instance()), onError)
        );
    }

    /**
     * Forget the current device synchronously.
     * @param device Auth device to forget
     * @throws AuthException exception
     */
    public void forgetDevice(@NonNull AuthDevice device) throws AuthException {
        Await.<Object, AuthException>result(AUTH_OPERATION_TIMEOUT_MS, (onResult, onError) ->
                asyncDelegate.forgetDevice(device, () -> onResult.accept(VoidResult.instance()), onError)
        );
    }

    /**
     * Fetch a list of remembered devices synchronously.
     * @return List of remembered Auth devices upon successful fetch
     * @throws AuthException exception
     */
    public List<AuthDevice> fetchDevices() throws AuthException {
        return Await.result(AUTH_OPERATION_TIMEOUT_MS, asyncDelegate::fetchDevices);
    }

    /**
     * Update the password of an existing user - must be signed in to perform this action.
     * @param oldPassword The user's existing password
     * @param newPassword The new password desired on the user account
     * @throws AuthException exception
     */
    public void updatePassword(
            @NonNull String oldPassword,
            @NonNull String newPassword
    ) throws AuthException {
        Await.<Object, AuthException>result(AUTH_OPERATION_TIMEOUT_MS, (onResult, onError) ->
                asyncDelegate.updatePassword(oldPassword, newPassword,
                    () -> onResult.accept(VoidResult.instance()), onError)
        );
    }

    /**
     * Fetch a list of user attributes synchronously.
     * @return List of user attributes upon successful fetch
     * @throws AuthException exception
     */
    public List<AuthUserAttribute> fetchUserAttribute() throws AuthException {
        return Await.<List<AuthUserAttribute>, AuthException>result((onResult, onError) -> {
            asyncDelegate.fetchUserAttributes(onResult, onError);
        });
    }

    /**
     * Update user attribute synchronously.
     * @param attribute The user attribute to be updated
     * @param options Advanced options such as a map of auth information for custom auth
     * @return result object
     * @throws AuthException exception
     */
    public AuthUpdateAttributeResult updateUserAttribute(
            @NonNull AuthUserAttribute attribute,
            @NonNull AuthUpdateUserAttributeOptions options
    ) throws AuthException {
        return Await.<AuthUpdateAttributeResult, AuthException>result((onResult, onError) -> {
            asyncDelegate.updateUserAttribute(attribute, options, onResult, onError);
        });
    }

    /**
     * Update user attribute synchronously.
     * @param attribute The user attribute to be updated
     * @return result object
     * @throws AuthException exception
     */
    public AuthUpdateAttributeResult updateUserAttribute(@NonNull AuthUserAttribute attribute) throws AuthException {
        return Await.<AuthUpdateAttributeResult, AuthException>result((onResult, onError) -> {
            asyncDelegate.updateUserAttribute(attribute, onResult, onError);
        });
    }

    /**
     * Update user attributes synchronously.
     * @param attributes The user attributes to be updated
     * @param options Advanced options such as a map of auth information for custom auth
     * @return result object
     * @throws AuthException exception
     */
    public Map<AuthUserAttributeKey, AuthUpdateAttributeResult> updateUserAttributes(
            @NonNull List<AuthUserAttribute> attributes,
            @NonNull AuthUpdateUserAttributesOptions options
    ) throws AuthException {
        return Await.<Map<AuthUserAttributeKey, AuthUpdateAttributeResult>, AuthException>result((
            (onResult, onError) -> {
                asyncDelegate.updateUserAttributes(attributes, options, onResult, onError);
            }));
    }

    /**
     * Update user attributes synchronously.
     * @param attributes The user attributes to be updated
     * @return result object
     * @throws AuthException exception
     */
    public Map<AuthUserAttributeKey, AuthUpdateAttributeResult> updateUserAttributes(
            @NonNull List<AuthUserAttribute> attributes) throws AuthException {
        return Await.<Map<AuthUserAttributeKey, AuthUpdateAttributeResult>, AuthException>result((
            (onResult, onError) -> {
                asyncDelegate.updateUserAttributes(attributes, onResult, onError);
            }));
    }

    /**
     * Resend user attribute confirmation code to verify user attribute synchronously.
     * @param attributeKey The user attribute key
     * @param options Advanced options such as a map of auth information for custom auth
     * @return result object
     * @throws AuthException exception
     */
    public AuthCodeDeliveryDetails resendUserAttributeConfirmationCode(
            @NonNull AuthUserAttributeKey attributeKey,
            @NonNull AuthResendUserAttributeConfirmationCodeOptions options
    ) throws AuthException {
        return Await.<AuthCodeDeliveryDetails, AuthException>result((onResult, onError) -> {
            asyncDelegate.resendUserAttributeConfirmationCode(attributeKey, options, onResult, onError);
        });
    }

    /**
     * Resend user attribute confirmation code to verify user attribute synchronously.
     * @param attributeKey The user attribute key
     * @return result object
     * @throws AuthException exception
     */
    public AuthCodeDeliveryDetails resendUserAttributeConfirmationCode(
            @NonNull AuthUserAttributeKey attributeKey) throws AuthException {
        return Await.<AuthCodeDeliveryDetails, AuthException>result((onResult, onError) -> {
            asyncDelegate.resendUserAttributeConfirmationCode(attributeKey, onResult, onError);
        });
    }

    /**
     * Confirm user attribute synchronously.
     * @param attributeKey The user attribute key
     * @param confirmationCode The confirmation code the user received after starting the confirmUserAttribute process
     * @throws AuthException exception
     */
    public void confirmUserAttribute(@NonNull AuthUserAttributeKey attributeKey,
                                     @NonNull String confirmationCode) throws AuthException {
        Await.<Object, AuthException>result(AUTH_OPERATION_TIMEOUT_MS, (onResult, onError) -> {
            asyncDelegate.confirmUserAttribute(
                    attributeKey, confirmationCode, () -> onResult.accept(VoidResult.instance()), onError
            );
        });
    }

    /**
     * Sign out synchronously.
     * @throws AuthException exception
     */
    public void signOut() throws AuthException {
        Await.<Object, AuthException>result(AUTH_OPERATION_TIMEOUT_MS, (onResult, onError) ->
                asyncDelegate.signOut(onResult::accept)
        );
    }

    /**
     * Sign out synchronously with options.
     * @param options Advanced options for signing out
     * @throws AuthException exception
     */
    public void signOut(AuthSignOutOptions options) throws AuthException {
        Await.<Object, AuthException>result(AUTH_OPERATION_TIMEOUT_MS, (onResult, onError) ->
                asyncDelegate.signOut(options, onResult::accept)
        );
    }

    /**
     * Delete the account of the currently signed in user.
     * @throws AuthException exception
     */
    public void deleteUser() throws AuthException {
        Await.<Object, AuthException>result(AUTH_OPERATION_TIMEOUT_MS, (onResult, onError) ->
                asyncDelegate.deleteUser(() -> onResult.accept(VoidResult.instance()), onError)
        );
    }

    /**
     * Get the current signed in user.
     * @return current authenticated user
     * @throws AuthException exception
     */
    public AuthUser getCurrentUser() throws AuthException {
        return Await.<AuthUser, AuthException>result(AUTH_OPERATION_TIMEOUT_MS, (onResult, onError) ->
            asyncDelegate.getCurrentUser(onResult, onError)
        );
    }
}
