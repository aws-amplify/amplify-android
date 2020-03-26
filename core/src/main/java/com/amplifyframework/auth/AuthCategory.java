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
import com.amplifyframework.core.category.Category;
import com.amplifyframework.core.category.CategoryType;

/**
 * Defines the Client API consumed by the application.
 * Internally routes the calls to the Auth Category
 * plugins registered.
 */
public final class AuthCategory extends Category<AuthPlugin<?>> implements AuthCategoryBehavior {

    @NonNull
    @Override
    public CategoryType getCategoryType() {
        return CategoryType.AUTH;
    }

    @Override
    public void signUp(
            @NonNull String username,
            @NonNull String password,
            @NonNull AuthSignUpOptions options,
            @NonNull Consumer<AuthSignUpResult> onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {
        getSelectedPlugin().signUp(username, password, options, onSuccess, onError);
    }

    @Override
    public void confirmSignUp(
            @NonNull String username,
            @NonNull String code,
            @NonNull Consumer<AuthSignUpResult> onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {
        getSelectedPlugin().confirmSignUp(username, code, onSuccess, onError);
    }

    @Override
    public void resendSignUpCode(
            @NonNull String username,
            @NonNull Consumer<AuthSignUpResult> onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {
        getSelectedPlugin().resendSignUpCode(username, onSuccess, onError);
    }

    @Override
    public void signIn(
            @Nullable String username,
            @Nullable String password,
            @Nullable AuthSignInOptions options,
            @NonNull Consumer<AuthSignInResult> onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {
        getSelectedPlugin().signIn(username, password, options, onSuccess, onError);
    }

    @Override
    public void signIn(
            @Nullable String username,
            @Nullable String password,
            @NonNull Consumer<AuthSignInResult> onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {
        getSelectedPlugin().signIn(username, password, onSuccess, onError);
    }

    @Override
    public void signInWithUI(
            @NonNull Activity callingActivity,
            @NonNull Consumer<String> onSuccess,
            @NonNull Consumer<AmplifyException> onError
    ) {
        getSelectedPlugin().signInWithUI(callingActivity, onSuccess, onError);
    }

    @Override
    public void handleSignInWithUIResponse(Intent intent) {
        getSelectedPlugin().handleSignInWithUIResponse(intent);
    }

    @Override
    public void signInWithFacebook(
            @NonNull String token,
            @NonNull Consumer<String> onSuccess,
            @NonNull Consumer<AmplifyException> onError
    ) {
        getSelectedPlugin().signInWithFacebook(token, onSuccess, onError);
    }

    @Override
    public void currentAuthState(
            @NonNull Consumer<AuthSession> onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {
        getSelectedPlugin().currentAuthState(onSuccess, onError);
    }
}

