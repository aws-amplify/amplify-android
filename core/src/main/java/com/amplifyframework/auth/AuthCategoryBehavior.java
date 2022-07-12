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

import com.amplifyframework.auth.options.AuthConfirmResetPasswordOptions;
import com.amplifyframework.auth.options.AuthConfirmSignInOptions;
import com.amplifyframework.auth.options.AuthConfirmSignUpOptions;
import com.amplifyframework.auth.options.AuthResendSignUpCodeOptions;
import com.amplifyframework.auth.options.AuthResendUserAttributeConfirmationCodeOptions;
import com.amplifyframework.auth.options.AuthResetPasswordOptions;
import com.amplifyframework.auth.options.AuthSignInOptions;
import com.amplifyframework.auth.options.AuthSignOutOptions;
import com.amplifyframework.auth.options.AuthSignUpOptions;
import com.amplifyframework.auth.options.AuthUpdateUserAttributeOptions;
import com.amplifyframework.auth.options.AuthUpdateUserAttributesOptions;
import com.amplifyframework.auth.options.AuthWebUISignInOptions;
import com.amplifyframework.auth.result.AuthResetPasswordResult;
import com.amplifyframework.auth.result.AuthSignInResult;
import com.amplifyframework.auth.result.AuthSignUpResult;
import com.amplifyframework.auth.result.AuthUpdateAttributeResult;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Consumer;

import java.util.List;
import java.util.Map;

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
     * @param options Advanced options such as a map of auth information for custom auth
     * @param onSuccess Success callback
     * @param onError Error callback
     */
    void confirmSignUp(
            @NonNull String username,
            @NonNull String confirmationCode,
            @NonNull AuthConfirmSignUpOptions options,
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
     * @param options Advanced options such as a map of auth information for custom auth
     * @param onSuccess Success callback
     * @param onError Error callback
     */
    void resendSignUpCode(
            @NonNull String username,
            @NonNull AuthResendSignUpCodeOptions options,
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
     * @param options Advanced options such as a map of auth information for custom auth
     * @param onSuccess Success callback
     * @param onError Error callback
     */
    void confirmSignIn(
            @NonNull String confirmationCode,
            @NonNull AuthConfirmSignInOptions options,
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
     * Launch the specified auth provider's web UI sign in experience. You should also put the
     * {@link #handleWebUISignInResponse(Intent)} method in your activity's onNewIntent method to
     * capture the response which comes back from the UI flow.
     * @param provider The auth provider you want to launch the web ui for (e.g. Facebook, Google, etc.)
     * @param callingActivity The activity in your app you are calling this from
     * @param onSuccess Success callback
     * @param onError Error callback
     */
    void signInWithSocialWebUI(
            @NonNull AuthProvider provider,
            @NonNull Activity callingActivity,
            @NonNull Consumer<AuthSignInResult> onSuccess,
            @NonNull Consumer<AuthException> onError);

    /**
     * Launch the specified auth provider's web UI sign in experience. You should also put the
     * {@link #handleWebUISignInResponse(Intent)} method in your activity's onNewIntent method to
     * capture the response which comes back from the UI flow.
     * @param provider The auth provider you want to launch the web ui for (e.g. Facebook, Google, etc.)
     * @param callingActivity The activity in your app you are calling this from
     * @param options Advanced options for signing in with an auth provider's hosted web ui.
     * @param onSuccess Success callback
     * @param onError Error callback
     */
    void signInWithSocialWebUI(
            @NonNull AuthProvider provider,
            @NonNull Activity callingActivity,
            @NonNull AuthWebUISignInOptions options,
            @NonNull Consumer<AuthSignInResult> onSuccess,
            @NonNull Consumer<AuthException> onError);

    /**
     * Launch a hosted web sign in UI flow. You should also put the {@link #handleWebUISignInResponse(Intent)} method in
     * your activity's onNewIntent method to capture the response which comes back from the UI flow.
     * @param callingActivity The activity in your app you are calling this from
     * @param onSuccess Success callback
     * @param onError Error callback
     */
    void signInWithWebUI(
            @NonNull Activity callingActivity,
            @NonNull Consumer<AuthSignInResult> onSuccess,
            @NonNull Consumer<AuthException> onError);

    /**
     * Launch a hosted web sign in UI flow. You should also put the {@link #handleWebUISignInResponse(Intent)}
     * method in your activity's onNewIntent method to capture the response which comes back from the UI flow.
     * @param callingActivity The activity in your app you are calling this from
     * @param options Advanced options for signing in with a hosted web ui.
     * @param onSuccess Success callback
     * @param onError Error callback
     */
    void signInWithWebUI(
            @NonNull Activity callingActivity,
            @NonNull AuthWebUISignInOptions options,
            @NonNull Consumer<AuthSignInResult> onSuccess,
            @NonNull Consumer<AuthException> onError);

    /**
     * Handles the response which comes back from {@link #signInWithWebUI(Activity, Consumer, Consumer)}.
     * @param intent The app activity's intent
     */
    void handleWebUISignInResponse(Intent intent);

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
     * Remember the user device that is currently being used.
     * @param onSuccess Success callback
     * @param onError Error callback
     */
    void rememberDevice(
            @NonNull Action onSuccess,
            @NonNull Consumer<AuthException> onError);

    /**
     * Forget the user device that is currently being used from the list
     * of remembered devices.
     * @param onSuccess Success callback
     * @param onError Error callback
     */
    void forgetDevice(
            @NonNull Action onSuccess,
            @NonNull Consumer<AuthException> onError);

    /**
     * Forget a specific user device from the list of remembered devices.
     * @param device Auth device to forget
     * @param onSuccess Success callback
     * @param onError Error callback
     */
    void forgetDevice(
            @NonNull AuthDevice device,
            @NonNull Action onSuccess,
            @NonNull Consumer<AuthException> onError);

    /**
     * Obtain a list of devices that are being tracked by the category.
     * @param onSuccess Success callback
     * @param onError Error callback
     */
    void fetchDevices(
            @NonNull Consumer<List<AuthDevice>> onSuccess,
            @NonNull Consumer<AuthException> onError);

    /**
     * Trigger password recovery for the given username.
     * @param username A login identifier e.g. `superdog22`; or an email/phone number, depending on configuration
     * @param options Advanced options such as a map of auth information for custom auth
     * @param onSuccess Success callback
     * @param onError Error callback
     */
    void resetPassword(
            @NonNull String username,
            @NonNull AuthResetPasswordOptions options,
            @NonNull Consumer<AuthResetPasswordResult> onSuccess,
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
     * @param options Advanced options such as a map of auth information for custom auth
     * @param onSuccess Success callback
     * @param onError Error callback
     */
    void confirmResetPassword(
            @NonNull String newPassword,
            @NonNull String confirmationCode,
            @NonNull AuthConfirmResetPasswordOptions options,
            @NonNull Action onSuccess,
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
     * Fetch user attributes.
     * @param onSuccess Success callback
     * @param onError Error callback
     */
    void fetchUserAttributes(
            @NonNull Consumer<List<AuthUserAttribute>> onSuccess,
            @NonNull Consumer<AuthException> onError
    );

    /**
     * Update a single user attribute.
     * @param attribute Attribute to be updated
     * @param options Advanced options such as a map of auth information for custom auth
     * @param onSuccess Success callback
     * @param onError Error callback
     */
    void updateUserAttribute(
            @NonNull AuthUserAttribute attribute,
            @NonNull AuthUpdateUserAttributeOptions options,
            @NonNull Consumer<AuthUpdateAttributeResult> onSuccess,
            @NonNull Consumer<AuthException> onError
    );

    /**
     * Update a single user attribute.
     * @param attribute Attribute to be updated
     * @param onSuccess Success callback
     * @param onError Error callback
     */
    void updateUserAttribute(
            @NonNull AuthUserAttribute attribute,
            @NonNull Consumer<AuthUpdateAttributeResult> onSuccess,
            @NonNull Consumer<AuthException> onError);

    /**
     * Update multiple user attributes.
     * @param attributes Attributes to be updated
     * @param options Advanced options such as a map of auth information for custom auth
     * @param onSuccess Success callback
     * @param onError Error callback
     */
    void updateUserAttributes(
            @NonNull List<AuthUserAttribute> attributes,
            @NonNull AuthUpdateUserAttributesOptions options,
            @NonNull Consumer<Map<AuthUserAttributeKey, AuthUpdateAttributeResult>> onSuccess,
            @NonNull Consumer<AuthException> onError
    );

    /**
     * Update multiple user attributes.
     * @param attributes Attributes to be updated
     * @param onSuccess Success callback
     * @param onError Error callback
     */
    void updateUserAttributes(
            @NonNull List<AuthUserAttribute> attributes,
            @NonNull Consumer<Map<AuthUserAttributeKey, AuthUpdateAttributeResult>> onSuccess,
            @NonNull Consumer<AuthException> onError
    );

    /**
     * If the user's confirmation code expires or they just missed it, this method
     * can be used to send them a new one.
     * @param attributeKey Key of attribute that user wants to operate on
     * @param options Advanced options such as a map of auth information for custom auth
     * @param onSuccess Success callback
     * @param onError Error callback
     */
    void resendUserAttributeConfirmationCode(
            @NonNull AuthUserAttributeKey attributeKey,
            @NonNull AuthResendUserAttributeConfirmationCodeOptions options,
            @NonNull Consumer<AuthCodeDeliveryDetails> onSuccess,
            @NonNull Consumer<AuthException> onError
    );

    /**
     * If the user's confirmation code expires or they just missed it, this method
     * can be used to send them a new one.
     * @param attributeKey Key of attribute that user wants to operate on
     * @param onSuccess Success callback
     * @param onError Error callback
     */
    void resendUserAttributeConfirmationCode(
            @NonNull AuthUserAttributeKey attributeKey,
            @NonNull Consumer<AuthCodeDeliveryDetails> onSuccess,
            @NonNull Consumer<AuthException> onError
    );

    /**
     * Use attribute key and confirmation code to confirm user attribute.
     * @param attributeKey Key of attribute that user wants to operate on
     * @param confirmationCode The confirmation code the user received after starting the user attribute operation
     * @param onSuccess Success callback
     * @param onError Error callback
     */
    void confirmUserAttribute(
            @NonNull AuthUserAttributeKey attributeKey,
            @NonNull String confirmationCode,
            @NonNull Action onSuccess,
            @NonNull Consumer<AuthException> onError
    );

    /**
     * Gets the currently logged in User.
     * @param onSuccess Success callback
     * @param onError Error callback
     */
    void getCurrentUser(
            @NonNull Consumer<AuthUser> onSuccess,
            @NonNull Consumer<AuthException> onError
    );

    /**
     * Sign out of the current device.
     * @param onSuccess Success callback
     * @param onError Error callback
     */
    void signOut(
            @NonNull Action onSuccess,
            @NonNull Consumer<AuthException> onError);

    /**
     * Sign out with advanced options.
     * @param options Advanced options for sign out (e.g. whether to sign out of all devices globally)
     * @param onSuccess Success callback
     * @param onError Error callback
     */
    void signOut(
            @NonNull AuthSignOutOptions options,
            @NonNull Action onSuccess,
            @NonNull Consumer<AuthException> onError);

    /**
     * Delete the account of the currently signed in user.
     * @param onSuccess Success callback
     * @param onError Error callback
     */
    void deleteUser(
            @NonNull Action onSuccess,
            @NonNull Consumer<AuthException> onError);
}
