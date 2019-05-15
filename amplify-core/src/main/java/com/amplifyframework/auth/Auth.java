/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.
 * All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.amplifyframework.auth;

import android.content.Context;
import android.support.annotation.AnyThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.async.Callback;

public class Auth extends Amplify {

    private User user;
    private Context context;

    @AnyThread
    public Auth(@NonNull Context context) {
        this.context = context;
    }

    @AnyThread
    public User getCurrentUser() {
        return user;
    }

    @AnyThread
    public void signUp(@NonNull String username,
                       @NonNull String password,
                       @Nullable Callback<SignUpResult> callback,
                       @Nullable SignUpOptions signUpOptions) {

    }

    @AnyThread
    public void confirmSignUp(@NonNull String username,
                              @NonNull String userConfirmationCode,
                              @Nullable Callback<SignUpResult> callback,
                              @Nullable SignUpOptions signUpOptions) {

    }

    @AnyThread
    public void sendVerificationCode(@NonNull String username,
                                     @Nullable Callback<SignUpResult> callback,
                                     @Nullable SignUpOptions signUpOptions) {

    }

    @AnyThread
    public void signIn(@NonNull String username,
                       @NonNull String password,
                       @Nullable Callback<SignInResult> callback,
                       @Nullable SignInOptions signInOptions) {

    }

    @AnyThread
    public void signIn(@NonNull IdentityProvider identityProvider,
                       @NonNull String token,
                       @Nullable Callback<SignInResult> callback,
                       @Nullable SignInOptions signInOptions) {

    }

    @AnyThread
    public void confirmSignIn(@NonNull String signInConfirmation,
                              @Nullable Callback<SignInResult> callback,
                              @Nullable SignInOptions signInOptions) {

    }

    @AnyThread
    public void signOut(@Nullable Callback<Void> callback,
                        @Nullable SignOutOptions signOutOptions) {

    }

    @AnyThread
    public void forgotPassword(@NonNull String username,
                               @Nullable Callback<SignUpResult> callback,
                               @Nullable SignUpOptions signUpOptions) {

    }

    @AnyThread
    public void confirmForgotPassword(@NonNull String newPassword,
                                      @NonNull String userConfirmationCode,
                                      @Nullable Callback<SignUpResult> callback,
                                      @Nullable SignUpOptions signUpOptions) {

    }

    @AnyThread
    public boolean isSignedIn() {
        return UserState.SIGNED_IN.equals(user.getUserState());
    }

    @AnyThread
    public void showSignInUI(@NonNull Context context,
                             @Nullable Callback<User> callback,
                             @Nullable SignInUIOptions signInUiOptions ) {

    }
}
