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

package com.amplifyframework.auth;

import android.app.Activity;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.auth.options.AuthAssociateWebAuthnCredentialsOptions;
import com.amplifyframework.auth.options.AuthConfirmResetPasswordOptions;
import com.amplifyframework.auth.options.AuthConfirmSignInOptions;
import com.amplifyframework.auth.options.AuthConfirmSignUpOptions;
import com.amplifyframework.auth.options.AuthDeleteWebAuthnCredentialOptions;
import com.amplifyframework.auth.options.AuthFetchSessionOptions;
import com.amplifyframework.auth.options.AuthListWebAuthnCredentialsOptions;
import com.amplifyframework.auth.options.AuthResendSignUpCodeOptions;
import com.amplifyframework.auth.options.AuthResendUserAttributeConfirmationCodeOptions;
import com.amplifyframework.auth.options.AuthResetPasswordOptions;
import com.amplifyframework.auth.options.AuthSignInOptions;
import com.amplifyframework.auth.options.AuthSignOutOptions;
import com.amplifyframework.auth.options.AuthSignUpOptions;
import com.amplifyframework.auth.options.AuthUpdateUserAttributeOptions;
import com.amplifyframework.auth.options.AuthUpdateUserAttributesOptions;
import com.amplifyframework.auth.options.AuthVerifyTOTPSetupOptions;
import com.amplifyframework.auth.options.AuthWebUISignInOptions;
import com.amplifyframework.auth.result.AuthListWebAuthnCredentialsResult;
import com.amplifyframework.auth.result.AuthResetPasswordResult;
import com.amplifyframework.auth.result.AuthSignInResult;
import com.amplifyframework.auth.result.AuthSignOutResult;
import com.amplifyframework.auth.result.AuthSignUpResult;
import com.amplifyframework.auth.result.AuthUpdateAttributeResult;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.category.Category;
import com.amplifyframework.core.category.CategoryType;

import java.util.List;
import java.util.Map;

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
            @Nullable String password,
            @NonNull AuthSignUpOptions options,
            @NonNull Consumer<AuthSignUpResult> onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {
        getSelectedPlugin().signUp(username, password, options, onSuccess, onError);
    }

    @Override
    public void confirmSignUp(
            @NonNull String username,
            @NonNull String confirmationCode,
            @NonNull AuthConfirmSignUpOptions options,
            @NonNull Consumer<AuthSignUpResult> onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {
        getSelectedPlugin().confirmSignUp(username, confirmationCode, options, onSuccess, onError);
    }

    @Override
    public void confirmSignUp(
            @NonNull String username,
            @NonNull String confirmationCode,
            @NonNull Consumer<AuthSignUpResult> onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {
        getSelectedPlugin().confirmSignUp(username, confirmationCode, onSuccess, onError);
    }

    @Override
    public void resendSignUpCode(
            @NonNull String username,
            @NonNull AuthResendSignUpCodeOptions options,
            @NonNull Consumer<AuthCodeDeliveryDetails> onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {
        getSelectedPlugin().resendSignUpCode(username, options, onSuccess, onError);
    }

    @Override
    public void resendSignUpCode(
            @NonNull String username,
            @NonNull Consumer<AuthCodeDeliveryDetails> onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {
        getSelectedPlugin().resendSignUpCode(username, onSuccess, onError);
    }

    @Override
    public void signIn(
            @Nullable String username,
            @Nullable String password,
            @NonNull AuthSignInOptions options,
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
    public void confirmSignIn(
            @NonNull String challengeResponse,
            @NonNull AuthConfirmSignInOptions options,
            @NonNull Consumer<AuthSignInResult> onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {
        getSelectedPlugin().confirmSignIn(challengeResponse, options, onSuccess, onError);
    }

    @Override
    public void confirmSignIn(
            @NonNull String challengeResponse,
            @NonNull Consumer<AuthSignInResult> onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {
        getSelectedPlugin().confirmSignIn(challengeResponse, onSuccess, onError);
    }

    @Override
    public void signInWithSocialWebUI(
            @NonNull AuthProvider provider,
            @NonNull Activity callingActivity,
            @NonNull Consumer<AuthSignInResult> onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {
        getSelectedPlugin().signInWithSocialWebUI(provider, callingActivity, onSuccess, onError);
    }

    @Override
    public void signInWithSocialWebUI(
            @NonNull AuthProvider provider,
            @NonNull Activity callingActivity,
            @NonNull AuthWebUISignInOptions options,
            @NonNull Consumer<AuthSignInResult> onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {
        getSelectedPlugin().signInWithSocialWebUI(provider, callingActivity, options, onSuccess, onError);
    }

    @Override
    public void signInWithWebUI(
            @NonNull Activity callingActivity,
            @NonNull Consumer<AuthSignInResult> onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {
        getSelectedPlugin().signInWithWebUI(callingActivity, onSuccess, onError);
    }

    @Override
    public void signInWithWebUI(
            @NonNull Activity callingActivity,
            @NonNull AuthWebUISignInOptions options,
            @NonNull Consumer<AuthSignInResult> onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {
        getSelectedPlugin().signInWithWebUI(callingActivity, options, onSuccess, onError);
    }

    @Override
    public void handleWebUISignInResponse(Intent intent) {
        getSelectedPlugin().handleWebUISignInResponse(intent);
    }

    @Override
    public void fetchAuthSession(
            @NonNull AuthFetchSessionOptions options,
            @NonNull Consumer<AuthSession> onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {
        getSelectedPlugin().fetchAuthSession(options, onSuccess, onError);
    }

    @Override
    public void fetchAuthSession(
            @NonNull Consumer<AuthSession> onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {
        getSelectedPlugin().fetchAuthSession(onSuccess, onError);
    }

    @Override
    public void rememberDevice(
            @NonNull Action onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {
        getSelectedPlugin().rememberDevice(onSuccess, onError);
    }

    @Override
    public void forgetDevice(
            @NonNull Action onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {
        getSelectedPlugin().forgetDevice(onSuccess, onError);
    }

    @Override
    public void forgetDevice(
            @NonNull AuthDevice device,
            @NonNull Action onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {
        getSelectedPlugin().forgetDevice(device, onSuccess, onError);
    }

    @Override
    public void fetchDevices(
            @NonNull Consumer<List<AuthDevice>> onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {
        getSelectedPlugin().fetchDevices(onSuccess, onError);
    }

    @Override
    public void resetPassword(
            @NonNull String username,
            @NonNull AuthResetPasswordOptions options,
            @NonNull Consumer<AuthResetPasswordResult> onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {
        getSelectedPlugin().resetPassword(username, options, onSuccess, onError);
    }

    @Override
    public void resetPassword(
            @NonNull String username,
            @NonNull Consumer<AuthResetPasswordResult> onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {
        getSelectedPlugin().resetPassword(username, onSuccess, onError);
    }

    @Override
    public void confirmResetPassword(
            @NonNull String username,
            @NonNull String newPassword,
            @NonNull String confirmationCode,
            @NonNull AuthConfirmResetPasswordOptions options,
            @NonNull Action onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {
        getSelectedPlugin().confirmResetPassword(username, newPassword, confirmationCode, options, onSuccess, onError);
    }

    @Override
    public void confirmResetPassword(
            String username, @NonNull String newPassword,
            @NonNull String confirmationCode,
            @NonNull Action onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {
        getSelectedPlugin().confirmResetPassword(username, newPassword, confirmationCode, onSuccess, onError);
    }

    @Override
    public void updatePassword(
            @NonNull String oldPassword,
            @NonNull String newPassword,
            @NonNull Action onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {
        getSelectedPlugin().updatePassword(oldPassword, newPassword, onSuccess, onError);
    }

    @Override
    public void fetchUserAttributes(
            @NonNull Consumer<List<AuthUserAttribute>> onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {
        getSelectedPlugin().fetchUserAttributes(onSuccess, onError);
    }

    @Override
    public void updateUserAttribute(
            @NonNull AuthUserAttribute attribute,
            @NonNull AuthUpdateUserAttributeOptions options,
            @NonNull Consumer<AuthUpdateAttributeResult> onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {
        getSelectedPlugin().updateUserAttribute(attribute, options, onSuccess, onError);
    }

    @Override
    public void updateUserAttribute(
            @NonNull AuthUserAttribute attribute,
            @NonNull Consumer<AuthUpdateAttributeResult> onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {
        getSelectedPlugin().updateUserAttribute(attribute, onSuccess, onError);
    }

    @Override
    public void updateUserAttributes(
            @NonNull List<AuthUserAttribute> attributes,
            @NonNull AuthUpdateUserAttributesOptions options,
            @NonNull Consumer<Map<AuthUserAttributeKey, AuthUpdateAttributeResult>> onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {
        getSelectedPlugin().updateUserAttributes(attributes, options, onSuccess, onError);
    }

    @Override
    public void updateUserAttributes(
            @NonNull List<AuthUserAttribute> attributes,
            @NonNull Consumer<Map<AuthUserAttributeKey, AuthUpdateAttributeResult>> onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {
        getSelectedPlugin().updateUserAttributes(attributes, onSuccess, onError);
    }

    @Override
    public void resendUserAttributeConfirmationCode(
            @NonNull AuthUserAttributeKey attributeKey,
            @NonNull AuthResendUserAttributeConfirmationCodeOptions options,
            @NonNull Consumer<AuthCodeDeliveryDetails> onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {
        getSelectedPlugin().resendUserAttributeConfirmationCode(attributeKey, options, onSuccess, onError);
    }

    @Override
    public void resendUserAttributeConfirmationCode(
            @NonNull AuthUserAttributeKey attributeKey,
            @NonNull Consumer<AuthCodeDeliveryDetails> onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {
        getSelectedPlugin().resendUserAttributeConfirmationCode(attributeKey, onSuccess, onError);
    }

    @Override
    public void confirmUserAttribute(
            @NonNull AuthUserAttributeKey attributeKey,
            @NonNull String confirmationCode,
            @NonNull Action onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {
        getSelectedPlugin().confirmUserAttribute(attributeKey, confirmationCode, onSuccess, onError);
    }

    @Override
    public void getCurrentUser(@NonNull Consumer<AuthUser> onSuccess, @NonNull Consumer<AuthException> onError) {
        getSelectedPlugin().getCurrentUser(onSuccess, onError);
    }

    @Override
    public void signOut(@NonNull Consumer<AuthSignOutResult> onComplete) {
        getSelectedPlugin().signOut(onComplete);
    }

    @Override public void signOut(
            @NonNull AuthSignOutOptions options,
            @NonNull Consumer<AuthSignOutResult> onComplete
    ) {
        getSelectedPlugin().signOut(options, onComplete);
    }

    @Override
    public void deleteUser(
            @NonNull Action onSuccess,
            @NonNull Consumer<AuthException> onError) {
        getSelectedPlugin().deleteUser(onSuccess, onError);
    }

    @Override
    public void setUpTOTP(@NonNull Consumer<TOTPSetupDetails> onSuccess, @NonNull Consumer<AuthException> onError) {
        getSelectedPlugin().setUpTOTP(onSuccess, onError);
    }

    @Override
    public void verifyTOTPSetup(@NonNull String code, @NonNull Action onSuccess,
                                @NonNull Consumer<AuthException> onError) {
        getSelectedPlugin().verifyTOTPSetup(code, onSuccess, onError);
    }

    @Override
    public void verifyTOTPSetup(@NonNull String code, @NonNull AuthVerifyTOTPSetupOptions options,
                                @NonNull Action onSuccess, @NonNull Consumer<AuthException> onError) {
        getSelectedPlugin().verifyTOTPSetup(code, options, onSuccess, onError);
    }

    @Override
    public void associateWebAuthnCredential(
            @NonNull Activity callingActivity,
            @NonNull Action onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {
        getSelectedPlugin().associateWebAuthnCredential(callingActivity, onSuccess, onError);
    }

    @Override
    public void associateWebAuthnCredential(
            @NonNull Activity callingActivity,
            @NonNull AuthAssociateWebAuthnCredentialsOptions options,
            @NonNull Action onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {
        getSelectedPlugin().associateWebAuthnCredential(callingActivity, options, onSuccess, onError);
    }

    @Override
    public void listWebAuthnCredentials(
            @NonNull Consumer<AuthListWebAuthnCredentialsResult> onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {
        getSelectedPlugin().listWebAuthnCredentials(onSuccess, onError);
    }

    @Override
    public void listWebAuthnCredentials(
            @NonNull AuthListWebAuthnCredentialsOptions options,
            @NonNull Consumer<AuthListWebAuthnCredentialsResult> onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {
        getSelectedPlugin().listWebAuthnCredentials(options, onSuccess, onError);
    }

    @Override
    public void deleteWebAuthnCredential(
            @NonNull String credentialId,
            @NonNull Action onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {
        getSelectedPlugin().deleteWebAuthnCredential(credentialId, onSuccess, onError);
    }

    @Override
    public void deleteWebAuthnCredential(
            @NonNull String credentialId,
            @NonNull AuthDeleteWebAuthnCredentialOptions options,
            @NonNull Action onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {
        getSelectedPlugin().deleteWebAuthnCredential(credentialId, options, onSuccess, onError);
    }

    @Override
    public void autoSignIn(
            @NonNull Consumer<AuthSignInResult> onSuccess,
            @NonNull Consumer<AuthException> onError
    ) {
        getSelectedPlugin().autoSignIn(onSuccess, onError);
    }
}

