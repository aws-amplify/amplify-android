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

package com.amplifyframework.rx;

import android.app.Activity;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.auth.AuthCategoryBehavior;
import com.amplifyframework.auth.AuthDevice;
import com.amplifyframework.auth.AuthException;
import com.amplifyframework.auth.AuthProvider;
import com.amplifyframework.auth.AuthSession;
import com.amplifyframework.auth.AuthUser;
import com.amplifyframework.auth.options.AuthSignInOptions;
import com.amplifyframework.auth.options.AuthSignOutOptions;
import com.amplifyframework.auth.options.AuthSignUpOptions;
import com.amplifyframework.auth.options.AuthWebUISignInOptions;
import com.amplifyframework.auth.result.AuthResetPasswordResult;
import com.amplifyframework.auth.result.AuthSignInResult;
import com.amplifyframework.auth.result.AuthSignUpResult;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;

/**
 * An Rx-idiomatic expression of the {@link AuthCategoryBehavior}.
 */
public interface RxAuthCategoryBehavior {

    /**
     * Creates a new user account with the specified username and password.
     * Can also pass in user attributes to associate with the user through
     * the options object.
     * @param username A login identifier e.g. `superdog22`; or an email/phone number, depending on configuration
     * @param password The user's password
     * @param options Advanced options such as additional attributes of the user or validation data
     * @return An Rx {@link Single} which emits an {@link AuthSignUpResult} on success, or an
     *         {@link AuthException} on failure
     */
    Single<AuthSignUpResult> signUp(
            @NonNull String username,
            @NonNull String password,
            @NonNull AuthSignUpOptions options
    );

    /**
     * If you have attribute confirmation enabled, this will allow the user
     * to enter the confirmation code they received to activate their account.
     * @param username A login identifier e.g. `superdog22`; or an email/phone number, depending on configuration
     * @param confirmationCode The confirmation code the user received
     * @return An Rx {@link Single} which emits an {@link AuthSignUpResult} on successful confirmation,
     *         or an {@link AuthException} on failure
     */
    Single<AuthSignUpResult> confirmSignUp(@NonNull String username, @NonNull String confirmationCode);

    /**
     * If the user's code expires or they just missed it, this method can
     * be used to send them a new one.
     * @param username A login identifier e.g. `superdog22`; or an email/phone number, depending on configuration
     * @return An Rx {@link Single} which emits an {@link AuthSignUpResult} on successful confirmation,
     *         or an {@link AuthException} on failure
     */
    Single<AuthSignUpResult> resendSignUpCode(@NonNull String username);

    /**
     * Basic authentication to the app with a username and password or, if custom auth is setup,
     * you can send null for those and the necessary authentication details in the options object.
     * @param username A login identifier e.g. `superdog22`; or an email/phone number, depending on configuration
     * @param password User's password for normal signup, null if custom auth or passwordless configurations are setup
     * @param options Advanced options such as a map of auth information for custom auth
     * @return An Rx {@link Single} which emits {@link AuthSignInResult} on success,
     *         {@link AuthException} on failure
     */
    Single<AuthSignInResult> signIn(
            @Nullable String username,
            @Nullable String password,
            @NonNull AuthSignInOptions options
    );

    /**
     * Basic authentication to the app with a username and password.
     * @param username A login identifier e.g. `superdog22`; or an email/phone number, depending on configuration
     * @param password User's password
     * @return An Rx {@link Single} which emits {@link AuthSignInResult} on success,
     *         {@link AuthException} on failure
     */
    Single<AuthSignInResult> signIn(@Nullable String username, @Nullable String password);

    /**
     * Submit the confirmation code received as part of multi-factor Authentication during sign in.
     * @param confirmationCode The code received as part of the multi-factor authentication process
     * @return An Rx {@link Single} which emits {@link AuthSignInResult} on success,
     *         {@link AuthException} on failure
     */
    Single<AuthSignInResult> confirmSignIn(@NonNull String confirmationCode);

    /**
     * Launch the specified auth provider's web UI sign in experience. You should also put the
     * {@link #handleWebUISignInResponse(Intent)} method in your activity's onNewIntent method to
     * capture the response which comes back from the UI flow.
     * @param provider The auth provider you want to launch the web ui for (e.g. Facebook, Google, etc.)
     * @param callingActivity The activity in your app you are calling this from
     * @return An Rx {@link Single} which emits {@link AuthSignInResult} on success,
     *         {@link AuthException} on failure
     */
    Single<AuthSignInResult> signInWithSocialWebUI(
            @NonNull AuthProvider provider,
            @NonNull Activity callingActivity
    );

    /**
     * Launch the specified auth provider's web UI sign in experience. You should also put the
     * {@link #handleWebUISignInResponse(Intent)} method in your activity's onNewIntent method to
     * capture the response which comes back from the UI flow.
     * @param provider The auth provider you want to launch the web ui for (e.g. Facebook, Google, etc.)
     * @param callingActivity The activity in your app you are calling this from
     * @param options Advanced options for signing in with an auth provider's hosted web ui.
     * @return An Rx {@link Single} which emits {@link AuthSignInResult} on success,
     *         {@link AuthException} on failure
     */
    Single<AuthSignInResult> signInWithSocialWebUI(
            @NonNull AuthProvider provider,
            @NonNull Activity callingActivity,
            @NonNull AuthWebUISignInOptions options
    );

    /**
     * Launch a hosted web sign in UI flow. You should also put the {@link #handleWebUISignInResponse(Intent)} method in
     * your activity's onNewIntent method to capture the response which comes back from the UI flow.
     * @param callingActivity The activity in your app you are calling this from
     * @return An Rx {@link Single} which emits {@link AuthSignInResult} on success,
     *         {@link AuthException} on failure
     */
    Single<AuthSignInResult> signInWithWebUI(@NonNull Activity callingActivity);

    /**
     * Launch a hosted web sign in UI flow. You should also put the {@link #handleWebUISignInResponse(Intent)}
     * method in your activity's onNewIntent method to capture the response which comes back from the UI flow.
     * @param callingActivity The activity in your app you are calling this from
     * @param options Advanced options for signing in with a hosted web ui.
     * @return An Rx {@link Single} which emits {@link AuthSignInResult} on success,
     *         {@link AuthException} on failure
     */
    Single<AuthSignInResult> signInWithWebUI(
            @NonNull Activity callingActivity,
            @NonNull AuthWebUISignInOptions options
    );

    /**
     * Handles the response which comes back from {@link #signInWithWebUI(Activity)}.
     * @param intent The app activity's intent
     */
    void handleWebUISignInResponse(Intent intent);

    /**
     * Retrieve the user's current session information - by default just whether they are signed out or in.
     * Depending on how a plugin implements this, the resulting AuthSession can also be cast to a type specific
     * to that plugin which contains the various security tokens and other identifying information if you want to
     * manually use them outside the plugin. Within Amplify this should not be needed as the other categories will
     * automatically work as long as you are signed in.
     * @return An Rx {@link Single} which emits {@link AuthSession} on success,
     *         {@link AuthException} on failure
     */
    Single<AuthSession> fetchAuthSession();

    /**
     * Remember the user device that is currently being used.
     * @return An Rx {@link Completable} which completes successfully if device is remembered,
     *         emits an {@link AuthException} otherwise
     */
    Completable rememberDevice();

    /**
     * Forget the user device that is currently being used from the list
     * of remembered devices.
     * @return An Rx {@link Completable} which completes successfully if device is forgotten,
     *         emits an {@link AuthException} otherwise
     */
    Completable forgetDevice();

    /**
     * Forget a specific user device from the list of remembered devices.
     * @param device Auth device to forget
     * @return An Rx {@link Completable} which completes successfully if device is forgotten,
     *         emits an {@link AuthException} otherwise
     */
    Completable forgetDevice(@NonNull AuthDevice device);

    /**
     * Obtain a list of devices that are being tracked by the category.
     * @return An Rx {@link Single} which emits {@link List} of {@link AuthDevice} on success,
     *          {@link AuthException} on failure
     */
    Single<List<AuthDevice>> fetchDevices();

    /**
     * Trigger password recovery for the given username.
     * @param username A login identifier e.g. `superdog22`; or an email/phone number, depending on configuration
     * @return An Rx {@link Single} which emits {@link AuthResetPasswordResult} on success,
     *         {@link AuthException} on failure
     */
    Single<AuthResetPasswordResult> resetPassword(@NonNull String username);

    /**
     * Complete password recovery process by inputting user's desired new password and confirmation code.
     * @param newPassword The user's desired new password
     * @param confirmationCode The confirmation code the user received after starting the forgotPassword process
     * @return An Rx {@link Completable} which completes successfully if password reset is confirmed,
     *         emits an {@link AuthException} otherwise
     */
    Completable confirmResetPassword(@NonNull String newPassword, @NonNull String confirmationCode);

    /**
     * Update the password of an existing user - must be signed in to perform this action.
     * @param oldPassword The user's existing password
     * @param newPassword The new password desired on the user account
     * @return An Rx {@link Completable} which completes successfully if the
     *         user's password is updated successfully; emits an {@link AuthException},
     *         otherwise.
     */
    Completable updatePassword(@NonNull String oldPassword, @NonNull String newPassword);

    /**
     * Gets the currently logged in User.
     * @return the currently logged in user with basic info and methods for fetching/updating user attributes
     */
    AuthUser getCurrentUser();

    /**
     * Sign out of the current device.
     * @return An Rx {@link Completable} which completes upon successful sign-out; emits an
     *         {@link AuthException} otherwise
     */
    Completable signOut();

    /**
     * Sign out with advanced options.
     * @param options Advanced options for sign out (e.g. whether to sign out of all devices globally)
     * @return An Rx {@link Completable} which completes upon successful sign-out;
     *         emits an {@link AuthException} otherwise
     */
    Completable signOut(@NonNull AuthSignOutOptions options);
}
