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

package com.amplifyframework.rx;

import android.app.Activity;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.auth.AuthCategoryBehavior;
import com.amplifyframework.auth.AuthCodeDeliveryDetails;
import com.amplifyframework.auth.AuthDevice;
import com.amplifyframework.auth.AuthException;
import com.amplifyframework.auth.AuthProvider;
import com.amplifyframework.auth.AuthSession;
import com.amplifyframework.auth.AuthUser;
import com.amplifyframework.auth.AuthUserAttribute;
import com.amplifyframework.auth.AuthUserAttributeKey;
import com.amplifyframework.auth.TOTPSetupDetails;
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
import com.amplifyframework.auth.result.AuthWebAuthnCredential;

import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

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
     * @param options Advanced options such as a map of auth information for custom auth
     * @return An Rx {@link Single} which emits an {@link AuthSignUpResult} on successful confirmation,
     *         or an {@link AuthException} on failure
     */
    Single<AuthSignUpResult> confirmSignUp(
            @NonNull String username,
            @NonNull String confirmationCode,
            @NonNull AuthConfirmSignUpOptions options
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
     * @param options Advanced options such as a map of auth information for custom auth
     * @return An Rx {@link Single} which emits an {@link AuthCodeDeliveryDetails} on successful confirmation,
     *         or an {@link AuthException} on failure
     */
    Single<AuthCodeDeliveryDetails> resendSignUpCode(
            @NonNull String username,
            @NonNull AuthResendSignUpCodeOptions options
    );

    /**
     * If the user's code expires or they just missed it, this method can
     * be used to send them a new one.
     * @param username A login identifier e.g. `superdog22`; or an email/phone number, depending on configuration
     * @return An Rx {@link Single} which emits an {@link AuthCodeDeliveryDetails} on successful confirmation,
     *         or an {@link AuthException} on failure
     */
    Single<AuthCodeDeliveryDetails> resendSignUpCode(@NonNull String username);

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
     * @param challengeResponse The code received as part of the multi-factor authentication process
     * @param options Advanced options such as a map of auth information for custom auth
     * @return An Rx {@link Single} which emits {@link AuthSignInResult} on success,
     *         {@link AuthException} on failure
     */
    Single<AuthSignInResult> confirmSignIn(
            @NonNull String challengeResponse,
            @NonNull AuthConfirmSignInOptions options
    );

    /**
     * Submit the confirmation code received as part of multi-factor Authentication during sign in.
     * @param challengeResponse The code received as part of the multi-factor authentication process
     * @return An Rx {@link Single} which emits {@link AuthSignInResult} on success,
     *         {@link AuthException} on failure
     */
    Single<AuthSignInResult> confirmSignIn(@NonNull String challengeResponse);

    /**
     * Sign in the user after signed up confirmation.
     * @return An Rx {@link Single} which emits {@link AuthSignInResult} on success,
     *         {@link AuthException} on failure
     */
    Single<AuthSignInResult> autoSignIn();

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
     * Retrieve the user's current session information - by default just whether they are signed out or in.
     * Depending on how a plugin implements this, the resulting AuthSession can also be cast to a type specific
     * to that plugin which contains the various security tokens and other identifying information if you want to
     * manually use them outside the plugin. Within Amplify this should not be needed as the other categories will
     * automatically work as long as you are signed in.
     * @param options Advanced options for fetching auth session.
     * @return An Rx {@link Single} which emits {@link AuthSession} on success,
     *         {@link AuthException} on failure
     */
    Single<AuthSession> fetchAuthSession(@NonNull AuthFetchSessionOptions options);

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
     * @param options Advanced options such as a map of auth information for custom auth
     * @return An Rx {@link Single} which emits {@link AuthResetPasswordResult} on success,
     *         {@link AuthException} on failure
     */
    Single<AuthResetPasswordResult> resetPassword(
            @NonNull String username,
            @NonNull AuthResetPasswordOptions options
    );

    /**
     * Trigger password recovery for the given username.
     * @param username A login identifier e.g. `superdog22`; or an email/phone number, depending on configuration
     * @return An Rx {@link Single} which emits {@link AuthResetPasswordResult} on success,
     *         {@link AuthException} on failure
     */
    Single<AuthResetPasswordResult> resetPassword(@NonNull String username);

    /**
     * Complete password recovery process by inputting user's desired new password and confirmation code.
     * @param username A login identifier e.g. `superdog22`; or an email/phone number, depending on configuration
     * @param newPassword The user's desired new password
     * @param confirmationCode The confirmation code the user received after starting the forgotPassword process
     * @param options Advanced options such as a map of auth information for custom auth
     * @return An Rx {@link Completable} which completes successfully if password reset is confirmed,
     *         emits an {@link AuthException} otherwise
     */
    Completable confirmResetPassword(
            @NonNull String username,
            @NonNull String newPassword,
            @NonNull String confirmationCode,
            @NonNull AuthConfirmResetPasswordOptions options
    );

    /**
     * Complete password recovery process by inputting user's desired new password and confirmation code.
     * @param username A login identifier e.g. `superdog22`; or an email/phone number, depending on configuration
     * @param newPassword The user's desired new password
     * @param confirmationCode The confirmation code the user received after starting the forgotPassword process
     * @return An Rx {@link Completable} which completes successfully if password reset is confirmed,
     *         emits an {@link AuthException} otherwise
     */
    Completable confirmResetPassword(String username, @NonNull String newPassword, @NonNull String confirmationCode);

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
     * Fetch the user attributes of an existing user - must be signed in to perform this action.
     * @return An Rx {@link Single} which emits {@link List} of {@link AuthUserAttribute} on success,
     *          {@link AuthException} on failure
     */
    Single<List<AuthUserAttribute>> fetchUserAttributes();

    /**
     * Update a user attribute of a user who is signed in.
     * @param attribute The user attribute to be updated
     * @param options Advanced options such as a map of auth information for custom auth
     * @return An Rx {@link Single} which emits {@link AuthUpdateAttributeResult} on success,
     *         {@link AuthException} on failure
     */
    Single<AuthUpdateAttributeResult> updateUserAttribute(
            @NonNull AuthUserAttribute attribute,
            @NonNull AuthUpdateUserAttributeOptions options
    );

    /**
     * Update a user attribute of a user who is signed in.
     * @param attribute The user attribute to be updated
     * @return An Rx {@link Single} which emits {@link AuthUpdateAttributeResult} on success,
     *         {@link AuthException} on failure
     */
    Single<AuthUpdateAttributeResult> updateUserAttribute(@NonNull AuthUserAttribute attribute);

    /**
     * Update a list of user attributes of a user who is signed in.
     * @param attributes A list of user attributes to be updated
     * @param options Advanced options such as a map of auth information for custom auth
     * @return An Rx {@link Single} which emits a map which maps {@link AuthUserAttributeKey} into
     *         {@link AuthUpdateAttributeResult} on success, {@link AuthException} on failure
     */
    Single<Map<AuthUserAttributeKey, AuthUpdateAttributeResult>> updateUserAttributes(
            @NonNull List<AuthUserAttribute> attributes,
            @NonNull AuthUpdateUserAttributesOptions options
    );

    /**
     * Update a list of user attributes of a user who is signed in.
     * @param attributes A list of user attributes to be updated
     * @return An Rx {@link Single} which emits a map which maps {@link AuthUserAttributeKey} into
     *         {@link AuthUpdateAttributeResult} on success, {@link AuthException} on failure
     */
    Single<Map<AuthUserAttributeKey, AuthUpdateAttributeResult>> updateUserAttributes(
            @NonNull List<AuthUserAttribute> attributes);

    /**
     * Resend user attribute confirmation code to verify user attribute.
     * @param attributeKey The attribute key to be confirmed.
     * @param options Advanced options such as a map of auth information for custom auth
     * @return An Rx {@link Single} which emits {@link AuthCodeDeliveryDetails} on success,
     *         {@link AuthException} on failure
     */
    Single<AuthCodeDeliveryDetails> resendUserAttributeConfirmationCode(
            @NonNull AuthUserAttributeKey attributeKey,
            @NonNull AuthResendUserAttributeConfirmationCodeOptions options
    );

    /**
     * Resend user attribute confirmation code to verify user attribute.
     * @param attributeKey The attribute key to be confirmed.
     * @return An Rx {@link Single} which emits {@link AuthCodeDeliveryDetails} on success,
     *         {@link AuthException} on failure
     */
    Single<AuthCodeDeliveryDetails> resendUserAttributeConfirmationCode(@NonNull AuthUserAttributeKey attributeKey);

    /**
     * Confirm user attribute.
     * @param attributeKey The user attribute key
     * @param confirmationCode The confirmation code the user received after starting
     *                         the resendUserAttributeConfirmationCode process
     * @return An Rx {@link Completable} which completes successfully if attributeKey is confirmed,
     *         emits an {@link AuthException} otherwise
     */
    Completable confirmUserAttribute(@NonNull AuthUserAttributeKey attributeKey, @NonNull String confirmationCode);

    /**
     * Gets the currently logged in User.
     * @return the currently logged in user with basic info and methods for fetching/updating user attributes
     */
    Single<AuthUser> getCurrentUser();

    /**
     * Sign out of the current device.
     * @return An Rx {@link Single} which emits {@link AuthSignOutResult} on completion
     */
    Single<AuthSignOutResult> signOut();

    /**
     * Sign out with advanced options.
     * @param options Advanced options for sign out (e.g. whether to sign out of all devices globally)
     * @return An Rx {@link Single} which emits {@link AuthSignOutResult} on completion
     */
    Single<AuthSignOutResult> signOut(@NonNull AuthSignOutOptions options);

    /**
     * Delete the account of the currently signed in user.
     * @return An Rx {@link Completable} which completes upon successfully deleting the user;
     *         emits an {@link AuthException} otherwise
     */
    Completable deleteUser();

    /**
     * Setup TOTP for the currently signed in user.
     * @return An Rx {@link Single} which emits {@link TOTPSetupDetails} on completion
     */
    Single<TOTPSetupDetails> setUpTOTP();

    /**
     * Verify TOTP setup for the currently signed in user.
     * @param code TOTP code to verify TOTP setup
     * @return An Rx {@link Completable} which completes upon successfully verifying totp code;
     *         emits an {@link AuthException} otherwise
     */
    Completable verifyTOTPSetup(@NonNull String code);

    /**
     * Verify TOTP setup for the currently signed in user.
     * @param code TOTP code to verify TOTP setup
     * @param options additional options to verify totp setup
     * @return An Rx {@link Completable} which completes upon successfully verifying totp code;
     *         emits an {@link AuthException} otherwise
     */
    Completable verifyTOTPSetup(@NonNull String code, @NonNull AuthVerifyTOTPSetupOptions options);

    /**
     * Create and register a passkey on this device, enabling passwordless sign in using passkeys.
     * The user must be signed in to call this API.
     * @param callingActivity The current Activity instance, used for launching the CredentialManager UI
     * @return An Rx {@link Completable} which completes upon successfully associating a new credential;
     *         emits an {@link AuthException} otherwise
     */
    Completable associateWebAuthnCredential(@NonNull Activity callingActivity);

    /**
     * Create and register a passkey on this device, enabling passwordless sign in using passkeys.
     * The user must be signed in to call this API.
     * @param callingActivity The current Activity instance, used for launching the CredentialManager UI
     * @param options Advanced options for associating credentials
     * @return An Rx {@link Completable} which completes upon successfully associating a new credential;
     *         emits an {@link AuthException} otherwise
     */
    Completable associateWebAuthnCredential(
            @NonNull Activity callingActivity,
            @NonNull AuthAssociateWebAuthnCredentialsOptions options
    );

    /**
     * Retrieve a list of WebAuthn credentials that are associated with the user's account.
     * The user must be signed in to call this API.
     * @return An Rx {@link Single} which emits a list of {@link AuthWebAuthnCredential} on completion
     */
    Single<AuthListWebAuthnCredentialsResult> listWebAuthnCredentials();

    /**
     * Retrieve a list of WebAuthn credentials that are associated with the user's account.
     * The user must be signed in to call this API.
     * @param options Advanced options for listing credentials
     * @return An Rx {@link Single} which emits a list of {@link AuthWebAuthnCredential} on completion
     */
    Single<AuthListWebAuthnCredentialsResult> listWebAuthnCredentials(
            @NonNull AuthListWebAuthnCredentialsOptions options
    );

    /**
     * Delete the credential matching the given identifier.
     * @param credentialId The identifier for the credential to delete
     * @return An Rx {@link Completable} which completes upon successfully deleting the credential;
     *         emits an {@link AuthException} otherwise
     */
    Completable deleteWebAuthnCredential(@NonNull String credentialId);

    /**
     * Delete the credential matching the given identifier.
     * @param credentialId The identifier for the credential to delete
     * @param options Advanced options for deleting credentials
     * @return An Rx {@link Completable} which completes upon successfully deleting the credential;
     *         emits an {@link AuthException} otherwise
     */
    Completable deleteWebAuthnCredential(
            @NonNull String credentialId,
            @NonNull AuthDeleteWebAuthnCredentialOptions options
    );
}
