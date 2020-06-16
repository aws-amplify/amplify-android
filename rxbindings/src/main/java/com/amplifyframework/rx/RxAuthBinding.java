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
import androidx.annotation.VisibleForTesting;

import com.amplifyframework.auth.AuthCategoryBehavior;
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
import com.amplifyframework.core.Amplify;

import java.util.Objects;

import io.reactivex.Completable;
import io.reactivex.Single;

final class RxAuthBinding implements RxAuthCategoryBehavior {
    private final AuthCategoryBehavior delegate;

    RxAuthBinding() {
        this(Amplify.Auth);
    }

    @VisibleForTesting
    RxAuthBinding(@NonNull AuthCategoryBehavior delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    public Single<AuthSignUpResult> signUp(
            @NonNull String username, @NonNull String password, @NonNull AuthSignUpOptions options) {
        return toSingle((onResult, onError) ->
            delegate.signUp(username, password, options, onResult, onError));
    }

    @Override
    public Single<AuthSignUpResult> confirmSignUp(@NonNull String username, @NonNull String confirmationCode) {
        return toSingle((onResult, onError) ->
            delegate.confirmSignUp(username, confirmationCode, onResult, onError));
    }

    @Override
    public Single<AuthSignUpResult> resendSignUpCode(@NonNull String username) {
        return toSingle((onResult, onError) ->
            delegate.resendSignUpCode(username, onResult, onError));
    }

    @Override
    public Single<AuthSignInResult> signIn(
            @Nullable String username, @Nullable String password, @NonNull AuthSignInOptions options) {
        return toSingle((onResult, onError) ->
            delegate.signIn(username, password, options, onResult, onError));
    }

    @Override
    public Single<AuthSignInResult> signIn(@Nullable String username, @Nullable String password) {
        return toSingle((onResult, onError) -> delegate.signIn(username, password, onResult, onError));
    }

    @Override
    public Single<AuthSignInResult> confirmSignIn(@NonNull String confirmationCode) {
        return toSingle((onResult, onError) -> delegate.confirmSignIn(confirmationCode, onResult, onError));
    }

    @Override
    public Single<AuthSignInResult> signInWithSocialWebUI(
            @NonNull AuthProvider provider, @NonNull Activity callingActivity) {
        return toSingle((onResult, onError) ->
            delegate.signInWithSocialWebUI(provider, callingActivity, onResult, onError));
    }

    @Override
    public Single<AuthSignInResult> signInWithSocialWebUI(
            @NonNull AuthProvider provider,
            @NonNull Activity callingActivity,
            @NonNull AuthWebUISignInOptions options) {
        return toSingle((onResult, onError) ->
            delegate.signInWithSocialWebUI(provider, callingActivity, options, onResult, onError));
    }

    @Override
    public Single<AuthSignInResult> signInWithWebUI(@NonNull Activity callingActivity) {
        return toSingle((onResult, onError) -> delegate.signInWithWebUI(callingActivity, onResult, onError));
    }

    @Override
    public Single<AuthSignInResult> signInWithWebUI(
            @NonNull Activity callingActivity, @NonNull AuthWebUISignInOptions options) {
        return toSingle((onResult, onError) ->
            delegate.signInWithWebUI(callingActivity, options, onResult, onError));
    }

    @Override
    public void handleWebUISignInResponse(Intent intent) {
        delegate.handleWebUISignInResponse(intent);
    }

    @Override
    public Single<AuthSession> fetchAuthSession() {
        return toSingle(delegate::fetchAuthSession);
    }

    @Override
    public Single<AuthResetPasswordResult> resetPassword(@NonNull String username) {
        return toSingle((onResult, onError) -> delegate.resetPassword(username, onResult, onError));
    }

    @Override
    public Completable confirmResetPassword(@NonNull String newPassword, @NonNull String confirmationCode) {
        return toCompletable((onComplete, onError) ->
            delegate.confirmResetPassword(newPassword, confirmationCode, onComplete, onError));
    }

    @Override
    public Completable updatePassword(@NonNull String oldPassword, @NonNull String newPassword) {
        return toCompletable((onComplete, onError) ->
            delegate.updatePassword(oldPassword, newPassword, onComplete, onError));
    }

    @Override
    public AuthUser getCurrentUser() {
        return delegate.getCurrentUser();
    }

    @Override
    public Completable signOut() {
        return toCompletable(delegate::signOut);
    }

    @Override
    public Completable signOut(@NonNull AuthSignOutOptions options) {
        return toCompletable((onComplete, onError) -> delegate.signOut(options, onComplete, onError));
    }

    private <T> Single<T> toSingle(RxAdapters.VoidResultEmitter<T, AuthException> resultEmitter) {
        return Single.defer(() ->
            Single.create(emitter -> resultEmitter.emitTo(emitter::onSuccess, emitter::onError))
        );
    }

    private Completable toCompletable(RxAdapters.VoidCompletionEmitter<AuthException> resultEmitter) {
        return Completable.defer(() -> Completable.create(emitter ->
            resultEmitter.emitTo(emitter::onComplete, emitter::onError)
        ));
    }
}
