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
     * @param code The confirmation code the user received
     * @param onSuccess Success callback
     * @param onError Error callback
     */
    void confirmSignUp(
            @NonNull String username,
            @NonNull String code,
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

    void signIn(
            @Nullable String username,
            @Nullable String password,
            @NonNull AuthSignInOptions options,
            @NonNull Consumer<AuthSignInResult> onSuccess,
            @NonNull Consumer<AuthException> onError);

    void signIn(
            @Nullable String username,
            @Nullable String password,
            @NonNull Consumer<AuthSignInResult> onSuccess,
            @NonNull Consumer<AuthException> onError);

    void signInWithUI(
            @NonNull Activity callingActivity,
            @NonNull Consumer<String> onSuccess,
            @NonNull Consumer<AmplifyException> onError);

    void handleSignInWithUIResponse(Intent intent);

    void signInWithFacebook(
            @NonNull String token,
            @NonNull Consumer<String> onSuccess,
            @NonNull Consumer<AmplifyException> onError);

    void currentAuthorizationState(
            @NonNull Consumer<AuthSession> onSuccess,
            @NonNull Consumer<AuthException> onError);
}
