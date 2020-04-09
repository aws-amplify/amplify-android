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
import com.amplifyframework.auth.result.AuthSignInResult;
import com.amplifyframework.auth.result.AuthSignUpResult;
import com.amplifyframework.core.Consumer;

public interface AuthCategoryBehavior {

    /**
     * Creates a new user account with the specified username and password.
     * Can also pass in user attributes to associate with the user through
     * the options object.
     * @param username This can be a typical username, email, phone number, etc.
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
     * @param username The username of the account to confirm
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
     * @param username The username of the account to resend the code to
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
     * @param username This can be a normal username, email/phone if that's setup, or null if custom auth is enabled
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
     * @param username This can be a normal username or email/phone if that's setup
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
     * TODO: Replace this with a generic sign in with social method (or whatever naming lands on)
     *       which takes the provider as an enum.
     * @param token Token retrieved from the social provider's authentication code.
     * @param onSuccess Success callback
     * @param onError Error callback
     */
    void signInWithFacebook(
            @NonNull String token,
            @NonNull Consumer<String> onSuccess,
            @NonNull Consumer<AmplifyException> onError);

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
}
