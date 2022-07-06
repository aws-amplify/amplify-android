/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.kotlin.auth

import android.app.Activity
import android.content.Intent
import com.amplifyframework.auth.AuthCodeDeliveryDetails
import com.amplifyframework.auth.AuthDevice
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.AuthProvider
import com.amplifyframework.auth.AuthSession
import com.amplifyframework.auth.AuthUser
import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.options.AuthConfirmResetPasswordOptions
import com.amplifyframework.auth.options.AuthConfirmSignInOptions
import com.amplifyframework.auth.options.AuthConfirmSignUpOptions
import com.amplifyframework.auth.options.AuthResendSignUpCodeOptions
import com.amplifyframework.auth.options.AuthResendUserAttributeConfirmationCodeOptions
import com.amplifyframework.auth.options.AuthResetPasswordOptions
import com.amplifyframework.auth.options.AuthSignInOptions
import com.amplifyframework.auth.options.AuthSignOutOptions
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.auth.options.AuthUpdateUserAttributeOptions
import com.amplifyframework.auth.options.AuthUpdateUserAttributesOptions
import com.amplifyframework.auth.options.AuthWebUISignInOptions
import com.amplifyframework.auth.result.AuthResetPasswordResult
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.auth.result.AuthUpdateAttributeResult

/**
 * Defines Authentication behaviors available from Kotlin.
 */
interface Auth {

    /**
     * Creates a new user account with the specified username and password.
     * Can also pass in user attributes to associate with the user through
     * the options object.
     * @param username A login identifier e.g. `tony44`; or an email/phone number,
     *                 depending on configuration
     * @param password The user's password
     * @param options Advanced options such as additional attributes of the user
     *                or validation data.
     *                If not provided, default options will be used.
     * @return A sign-up result; check the nextStep field to determine which
     *         action to take next. The next step is typically to confirm a
     *         code sent over email/SMS.
     */
    @Throws(AuthException::class)
    suspend fun signUp(
        username: String,
        password: String,
        options: AuthSignUpOptions = AuthSignUpOptions.builder().build()
    ):
        AuthSignUpResult

    /**
     * If you have attribute confirmation enabled, this will allow the user
     * to enter the confirmation code they received to activate their account.
     * @param username A login identifier e.g. `tony44`; or an email/phone number,
     *                 depending on configuration
     * @param confirmationCode The confirmation code the user received
     * @param options Advanced options such as a map of auth information for custom auth,
     *                If not provided, default options will be used
     * @return A sign-up result; if the code has been confirmed successfully, the result
     *         will show true for isSignUpComplete().
     */
    @Throws(AuthException::class)
    suspend fun confirmSignUp(
        username: String,
        confirmationCode: String,
        options: AuthConfirmSignUpOptions = AuthConfirmSignUpOptions.defaults()
    ): AuthSignUpResult

    /**
     * If the user's code expires or they just missed it, this method can
     * be used to send them a new one.
     * @param username A login identifier e.g. `tony44`; or an email/phone number,
     *                 depending on configuration
     * @param options Advanced options such as a map of auth information for custom auth,
     *                If not provided, default options will be used
     * @return A sign-up result; if the code is requested, typically the result will
     *         include a next step requiring confirmation of the re-sent code.
     */
    @Throws(AuthException::class)
    suspend fun resendSignUpCode(
        username: String,
        options: AuthResendSignUpCodeOptions = AuthResendSignUpCodeOptions.defaults()
    ): AuthSignUpResult

    /**
     * Basic authentication to the app with a username and password or, if custom auth is setup,
     * you can send null for those and the necessary authentication details in the options object.
     * @param username A login identifier e.g. `tony44`; or an email/phone number, depending on configuration.
     *                 May be omitted or null when using custom auth.
     * @param password User's password for normal sign-up. May be omitted or null if custom auth or
     *                 password-less configurations are in use
     * @param options Advanced options such as a map of auth information for custom auth,
     *                If not provided, default options will be used
     * @return A sign-in result. The nextStep field may indicate additional actions to be taken
     *         to confirm the sign-in, or it may show isSignInComplete as true, in which case
     *         an authenticated session is available
     */
    @Throws(AuthException::class)
    suspend fun signIn(
        username: String? = null,
        password: String? = null,
        options: AuthSignInOptions = AuthSignInOptions.defaults()
    ):
        AuthSignInResult

    /**
     * Submit the confirmation code received as part of multi-factor Authentication during sign in.
     * @param confirmationCode The code received as part of the multi-factor authentication process
     * @param options Advanced options such as a map of auth information for custom auth,
     *                If not provided, default options will be used
     * @return A sign-in result; check the nextStep field for cues on additional sign-in challenges
     */
    @Throws(AuthException::class)
    suspend fun confirmSignIn(
        confirmationCode: String,
        options: AuthConfirmSignInOptions = AuthConfirmSignInOptions.defaults()
    ):
        AuthSignInResult

    /**
     * Launch the specified auth provider's web UI sign in experience. You should also put the
     * {@link #handleWebUISignInResponse(Intent)} method in your activity's onNewIntent method to
     * capture the response which comes back from the UI flow.
     * @param provider The auth provider you want to launch the web ui for (e.g. Facebook, Google, etc.)
     * @param callingActivity The activity in your app you are calling this from
     * @param options Advanced options for signing in with an auth provider's hosted web ui.
     *                If not provided, default options will be used.
     * @return A sign-in result; check the nextStep for additional sign-in confirmation requirements
     */
    @Throws(AuthException::class)
    suspend fun signInWithSocialWebUI(
        provider: AuthProvider,
        callingActivity: Activity,
        options: AuthWebUISignInOptions = AuthWebUISignInOptions.builder().build()
    ):
        AuthSignInResult

    /**
     * Launch a hosted web sign in UI flow. You should also put the {@link #handleWebUISignInResponse(Intent)}
     * method in your activity's onNewIntent method to capture the response which comes back from the UI flow.
     * @param callingActivity The activity in your app you are calling this from
     * @param options Advanced options for signing in with a hosted web ui.
     *                If not provided, default options will be used.
     * @return A sign-in result; check the nextStep for additional sign-in confirmation requirements
     */
    @Throws(AuthException::class)
    suspend fun signInWithWebUI(
        callingActivity: Activity,
        options: AuthWebUISignInOptions = AuthWebUISignInOptions.builder().build()
    ):
        AuthSignInResult

    /**
     * Handles the response which comes back from {@link #signInWithWebUI(Activity, Consumer, Consumer)}.
     * @param intent The app activity's intent
     */
    fun handleWebUISignInResponse(intent: Intent)

    /**
     * Retrieve the user's current session information - by default just whether they are signed out or in.
     * Depending on how a plugin implements this, the resulting AuthSession can also be cast to a type specific
     * to that plugin which contains the various security tokens and other identifying information if you want to
     * manually use them outside the plugin. Within Amplify this should not be needed as the other categories will
     * automatically work as long as you are signed in.
     * @return Information about the current authenticated session, where applicable
     */
    @Throws(AuthException::class)
    suspend fun fetchAuthSession(): AuthSession

    /**
     * Remember the user device that is currently being used.
     */
    @Throws(AuthException::class)
    suspend fun rememberDevice()

    /**
     * Forget the user device that is currently being used from the list
     * of remembered devices.
     * @param device Auth device to forget. If not provided, the device currently in use
     *               will be forgotten
     */
    @Throws(AuthException::class)
    suspend fun forgetDevice(device: AuthDevice? = null)

    /**
     * Obtain a list of devices that are being tracked by the category.
     * @return The list of devices currently being tracked
     */
    @Throws(AuthException::class)
    suspend fun fetchDevices(): List<AuthDevice>

    /**
     * Trigger password recovery for the given username.
     * @param username A login identifier e.g. `tony44`; or an email/phone number, depending on configuration
     * @param options Advanced options such as a map of auth information for custom auth,
     *                If not provided, default options will be used
     * @return A password resest result
     */
    @Throws(AuthException::class)
    suspend fun resetPassword(
        username: String,
        options: AuthResetPasswordOptions = AuthResetPasswordOptions.defaults()
    ): AuthResetPasswordResult

    /**
     * Complete password recovery process by inputting user's desired new password and confirmation code.
     * @param newPassword The user's desired new password
     * @param confirmationCode The confirmation code the user received after starting the forgotPassword process
     * @param options Advanced options such as a map of auth information for custom auth,
     *                If not provided, default options will be used
     */
    @Throws(AuthException::class)
    suspend fun confirmResetPassword(
        newPassword: String,
        confirmationCode: String,
        options: AuthConfirmResetPasswordOptions = AuthConfirmResetPasswordOptions.defaults()
    )

    /**
     * Update the password of an existing user - must be signed in to perform this action.
     * @param oldPassword The user's existing password
     * @param newPassword The new password desired on the user account
     */
    @Throws(AuthException::class)
    suspend fun updatePassword(oldPassword: String, newPassword: String)

    /**
     * Fetch user attributes.
     * @return A list of attributes associated to the user
     */
    @Throws(AuthException::class)
    suspend fun fetchUserAttributes(): List<AuthUserAttribute>

    /**
     * Update a single user attribute.
     * @param attribute Attribute to be updated
     * @param options Advanced options such as a map of auth information for custom auth,
     *                If not provided, default options will be used
     * @return The result of updating the provided attribute
     */
    @Throws(AuthException::class)
    suspend fun updateUserAttribute(
        attribute: AuthUserAttribute,
        options: AuthUpdateUserAttributeOptions = AuthUpdateUserAttributeOptions.defaults()
    ): AuthUpdateAttributeResult

    /**
     * Update multiple user attributes.
     * @param attributes Attributes to be updated
     * @param options Advanced options such as a map of auth information for custom auth,
     *                If not provided, default options will be used
     * @return The result of updating the provided attribute
     */
    @Throws(AuthException::class)
    suspend fun updateUserAttributes(
        attributes: List<AuthUserAttribute>,
        options: AuthUpdateUserAttributesOptions = AuthUpdateUserAttributesOptions.defaults()
    ): Map<AuthUserAttributeKey, AuthUpdateAttributeResult>

    /**
     * If the user's confirmation code expires or they just missed it, this method
     * can be used to send them a new one.
     * @param attributeKey Key of attribute that user wants to operate on
     * @param options Advanced options such as a map of auth information for custom auth,
     *                If not provided, default options will be used
     * @return Details about the delivery of an authentication code
     */
    @Throws(AuthException::class)
    suspend fun resendUserAttributeConfirmationCode(
        attributeKey: AuthUserAttributeKey,
        options: AuthResendUserAttributeConfirmationCodeOptions =
            AuthResendUserAttributeConfirmationCodeOptions.defaults()
    ): AuthCodeDeliveryDetails

    /**
     * Use attribute key and confirmation code to confirm user attribute.
     * @param attributeKey Key of attribute that user wants to operate on
     * @param confirmationCode The confirmation code the user received after starting the user attribute operation
     */
    @Throws(AuthException::class)
    suspend fun confirmUserAttribute(attributeKey: AuthUserAttributeKey, confirmationCode: String)

    /**
     * Gets the currently logged in User.
     * @return the currently logged in user with basic info and methods for fetching/updating user attributes
     * @return Information about the current user
     */
    suspend fun getCurrentUser(): AuthUser

    /**
     * Sign out with advanced options.
     * @param options Advanced options for sign out (e.g. whether to sign out of all devices globally).
     *                If not provided, default options are used.
     */
    @Throws(AuthException::class)
    suspend fun signOut(options: AuthSignOutOptions = AuthSignOutOptions.builder().build())

    /**
     * Delete the account of the currently signed in user.
     */
    @Throws(AuthException::class)
    suspend fun deleteUser()
}
