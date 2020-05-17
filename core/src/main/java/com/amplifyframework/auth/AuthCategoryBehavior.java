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

package com.amplifyframework.auth;

import android.app.Activity;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.auth.options.AuthSignInOptions;
import com.amplifyframework.auth.options.AuthSignUpOptions;
import com.amplifyframework.auth.result.AuthResetPasswordResult;
import com.amplifyframework.auth.result.AuthSignInResult;
import com.amplifyframework.auth.result.AuthSignUpResult;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Consumer;

/**
 * Specifies the behavior for the Auth category.
 */
public interface AuthCategoryBehavior {

    /**
     * Creates a new user account with the specified username and password.
     * Can also pass in user attributes to associate with the user through
     * the options object.
     * @param username A login identifier e.g. `superdog22`; or an email/phone number, depending on configuration
     * @param password The user's password
     * @param options Advanced options such as additional attributes of the user or validation data
     * @param onSuccess Success callback
     * @param onError Error callback
     */
    void signUp(
            @NonNull String username,
            @NonNull String password,
            @NonNull AuthSignUpOptions options,
            @NonNull Consumer<AuthSignUpResult> onSuccess,
            @NonNull Consumer<AuthException> onError);

    /**
     * If you have attribute confirmation enabled, this will allow the user
     * to enter the confirmation code they received to activate their account.
     * @param username A login identifier e.g. `superdog22`; or an email/phone number, depending on configuration
     * @param confirmationCode The confirmation code the user received
     * @param onSuccess Success callback
     * @param onError Error callback
     */
    void confirmSignUp(
            @NonNull String username,
            @NonNull String confirmationCode,
            @NonNull Consumer<AuthSignUpResult> onSuccess,
            @NonNull Consumer<AuthException> onError);

    /**
     * If the user's code expires or they just missed it, this method can
     * be used to send them a new one.
     * @param username A login identifier e.g. `superdog22`; or an email/phone number, depending on configuration
     * @param onSuccess Success callback
     * @param onError Error callback
     */
    void resendSignUpCode(
            @NonNull String username,
            @NonNull Consumer<AuthSignUpResult> onSuccess,
            @NonNull Consumer<AuthException> onError);

    /**
     * Basic authentication to the app with a username and password or, if custom auth is setup,
     * you can send null for those and the necessary authentication details in the options object.
     * @param username A login identifier e.g. `superdog22`; or an email/phone number, depending on configuration
     * @param password User's password for normal signup, null if custom auth or passwordless configurations are setup
     * @param options Advanced options such as a map of auth information for custom auth
     * @param onSuccess Success callback
     * @param onError Error callback
     */
    void signIn(
            @Nullable String username,
            @Nullable String password,
            @NonNull AuthSignInOptions options,
            @NonNull Consumer<AuthSignInResult> onSuccess,
            @NonNull Consumer<AuthException> onError);

    /**
     * Basic authentication to the app with a username and password.
     * @param username A login identifier e.g. `superdog22`; or an email/phone number, depending on configuration
     * @param password User's password
     * @param onSuccess Success callback
     * @param onError Error callback
     */
    void signIn(
            @Nullable String username,
            @Nullable String password,
            @NonNull Consumer<AuthSignInResult> onSuccess,
            @NonNull Consumer<AuthException> onError);

    /**
     * Submit the confirmation code received as part of multi-factor Authentication during sign in.
     * @param confirmationCode The code received as part of the multi-factor authentication process
     * @param onSuccess Success callback
     * @param onError Error callback
     */
    void confirmSignIn(
            @NonNull String confirmationCode,
            @NonNull Consumer<AuthSignInResult> onSuccess,
            @NonNull Consumer<AuthException> onError);

    /**
     * Launch a pre-built sign in UI flow. You should also put the {@link #handleSignInWithUIResponse(Intent)} method in
     * your activity's onResume method to capture the response which comes back from the UI flow.
     * @param callingActivity The activity in your app you are calling this from
     * @param onSuccess Success callback
     * @param onError Error callback
     */
    void signInWithUI(
            @NonNull Activity callingActivity,
            @NonNull Consumer<String> onSuccess,
            @NonNull Consumer<AmplifyException> onError);

    /**
     * Handles the response which comes back from {@link #signInWithUI(Activity, Consumer, Consumer)}.
     * @param intent The app activity's intent
     */
    void handleSignInWithUIResponse(Intent intent);

    /**
     * Retrieve the user's current session information - by default just whether they are signed out or in.
     * Depending on how a plugin implements this, the resulting AuthSession can also be cast to a type specific
     * to that plugin which contains the various security tokens and other identifying information if you want to
     * manually use them outside the plugin. Within Amplify this should not be needed as the other categories will
     * automatically work as long as you are signed in.
     * @param onSuccess Success callback
     * @param onError Error callback
     */
    void fetchAuthSession(
            @NonNull Consumer<AuthSession> onSuccess,
            @NonNull Consumer<AuthException> onError);

    /**
     * Trigger password recovery for the given username.
     * @param username A login identifier e.g. `superdog22`; or an email/phone number, depending on configuration
     * @param onSuccess Success callback
     * @param onError Error callback
     */
    void resetPassword(
            @NonNull String username,
            @NonNull Consumer<AuthResetPasswordResult> onSuccess,
            @NonNull Consumer<AuthException> onError);

    /**
     * Complete password recovery process by inputting user's desired new password and confirmation code.
     * @param newPassword The user's desired new password
     * @param confirmationCode The confirmation code the user received after starting the forgotPassword process
     * @param onSuccess Success callback
     * @param onError Error callback
     */
    void confirmResetPassword(
            @NonNull String newPassword,
            @NonNull String confirmationCode,
            @NonNull Action onSuccess,
            @NonNull Consumer<AuthException> onError);

    /**
     * Update the password of an existing user - must be signed in to perform this action.
     * @param oldPassword The user's existing password
     * @param newPassword The new password desired on the user account
     * @param onSuccess Success callback
     * @param onError Error callback
     */
    void updatePassword(
            @NonNull String oldPassword,
            @NonNull String newPassword,
            @NonNull Action onSuccess,
            @NonNull Consumer<AuthException> onError
    );

    /**
     * Gets the currently logged in User.
     * @return the currently logged in user with basic info and methods for fetching/updating user attributes
     */
    AuthUser getCurrentUser();

    /**
     * Sign out of all devices. The current credentials cached on other devices will be valid but will not be able to
     * be refreshed without signing in again so will expire shortly.
     * @param onSuccess Success callback
     * @param onError Error callback
     */
    void signOut(
            @NonNull Action onSuccess,
            @NonNull Consumer<AuthException> onError);
}
