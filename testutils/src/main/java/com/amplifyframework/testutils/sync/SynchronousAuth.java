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

package com.amplifyframework.testutils.sync;

import android.app.Activity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.auth.AuthCategoryBehavior;
import com.amplifyframework.auth.AuthDevice;
import com.amplifyframework.auth.AuthException;
import com.amplifyframework.auth.AuthProvider;
import com.amplifyframework.auth.AuthSession;
import com.amplifyframework.auth.options.AuthSignInOptions;
import com.amplifyframework.auth.options.AuthSignUpOptions;
import com.amplifyframework.auth.options.AuthWebUISignInOptions;
import com.amplifyframework.auth.result.AuthResetPasswordResult;
import com.amplifyframework.auth.result.AuthSignInResult;
import com.amplifyframework.auth.result.AuthSignUpResult;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.testutils.Await;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * A utility to perform synchronous calls to the {@link com.amplifyframework.auth.AuthCategory}.
 * This code is not well suited for production use, but is useful in test
 * code, where we want to make a series of sequential assertions after
 * performing various operations.
 */
public final class SynchronousAuth {
    private static final long AUTH_OPERATION_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(5);
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
     * Resend signup code synchronously.
     * @param username A login identifier e.g. `superdog22`; or an email/phone number, depending on configuration
     * @return result object
     * @throws AuthException exception
     */
    @NonNull
    public AuthSignUpResult resendSignUpCode(
            @NonNull String username
    ) throws AuthException {
        return Await.<AuthSignUpResult, AuthException>result(AUTH_OPERATION_TIMEOUT_MS, (onResult, onError) ->
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
     * @param confirmationCode The code received as part of the multi-factor authentication process
     * @return result object
     * @throws AuthException exception
     */
    @NonNull
    public AuthSignInResult confirmSignIn(
            @NonNull String confirmationCode
    ) throws AuthException {
        return Await.<AuthSignInResult, AuthException>result(AUTH_OPERATION_TIMEOUT_MS, (onResult, onError) ->
                asyncDelegate.confirmSignIn(confirmationCode, onResult, onError)
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
     * @param newPassword The user's desired new password
     * @param confirmationCode The confirmation code the user received after starting the forgotPassword process
     * @return Dummy object - just indicates it completed successfully
     * @throws AuthException exception
     */
    @NonNull
    public Object confirmResetPassword(
            @NonNull String newPassword,
            @NonNull String confirmationCode
    ) throws AuthException {
        return Await.<Object, AuthException>result(AUTH_OPERATION_TIMEOUT_MS, (onResult, onError) ->
                asyncDelegate.confirmResetPassword(
                    newPassword,
                    confirmationCode,
                    () -> onResult.accept(new Object()),
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
        return Await.result(AUTH_OPERATION_TIMEOUT_MS, asyncDelegate::fetchAuthSession);
    }

    /**
     * Remembers current device synchronously.
     * @return Dummy object - just indicates it completed successfully
     * @throws AuthException exception
     */
    public Object rememberDevice() throws AuthException {
        return Await.<Object, AuthException>result(AUTH_OPERATION_TIMEOUT_MS, (onResult, onError) ->
                asyncDelegate.rememberDevice(() -> onResult.accept(new Object()), onError)
        );
    }

    /**
     * Forgets the current device synchronously.
     * @return Dummy object - just indicates it completed successfully
     * @throws AuthException exception
     */
    public Object forgetDevice() throws AuthException {
        return Await.<Object, AuthException>result(AUTH_OPERATION_TIMEOUT_MS, (onResult, onError) ->
                asyncDelegate.forgetDevice(() -> onResult.accept(new Object()), onError)
        );
    }

    /**
     * Forgets the current device synchronously.
     * @param device Auth device to forget
     * @return Dummy object - just indicates it completed successfully
     * @throws AuthException exception
     */
    public Object forgetDevice(@NonNull AuthDevice device) throws AuthException {
        return Await.<Object, AuthException>result(AUTH_OPERATION_TIMEOUT_MS, (onResult, onError) ->
                asyncDelegate.forgetDevice(device, () -> onResult.accept(new Object()), onError)
        );
    }

    /**
     * Fetches a list of remembered devices synchronously.
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
     * @return Dummy object - just indicates it completed successfully
     * @throws AuthException exception
     */
    @NonNull
    public Object updatePassword(
            @NonNull String oldPassword,
            @NonNull String newPassword
    ) throws AuthException {
        return Await.<Object, AuthException>result(AUTH_OPERATION_TIMEOUT_MS, (onResult, onError) ->
                asyncDelegate.updatePassword(oldPassword, newPassword, () -> onResult.accept(new Object()), onError)
        );
    }

    /**
     * Sign out synchronously.
     * @return Dummy object - just indicates it completed successfully
     * @throws AuthException exception
     */
    @NonNull
    public Object signOut() throws AuthException {
        return Await.<Object, AuthException>result(AUTH_OPERATION_TIMEOUT_MS, (onResult, onError) ->
                asyncDelegate.signOut(
                    () -> onResult.accept(new Object()),
                    onError
                )
        );
    }
}
