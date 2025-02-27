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

package com.amplifyframework.auth.result.step;

import com.amplifyframework.core.Consumer;

/**
 * Represents the various common steps a user could be in for the sign in flow.
 */
public enum AuthSignInStep {
    /**
     * Multifactor authentication is enabled on this account and requires you to call
     * {@link com.amplifyframework.auth.AuthCategoryBehavior#confirmSignIn(String, Consumer, Consumer)}
     * with the code sent via SMS text message to proceed with the sign in flow.
     */
    CONFIRM_SIGN_IN_WITH_SMS_MFA_CODE,

    /**
     * Custom multifactor authentication is enabled on this account and requires you to call
     * {@link com.amplifyframework.auth.AuthCategoryBehavior#confirmSignIn(String, Consumer, Consumer)}
     * with the input expected by the custom challenge to proceed with the sign in flow.
     */
    CONFIRM_SIGN_IN_WITH_CUSTOM_CHALLENGE,

    /**
     * The user account is required to set a new password on login. Call
     * {@link com.amplifyframework.auth.AuthCategoryBehavior#confirmSignIn(String, Consumer, Consumer)}
     * with the new password specified by the user to proceed with the sign in flow.
     */
    CONFIRM_SIGN_IN_WITH_NEW_PASSWORD,

    /**
     * The user account is required to reset its password to be able to login. Call
     * {@link com.amplifyframework.auth.AuthCategoryBehavior#resetPassword(String, Consumer, Consumer)}
     * and proceed with the reset password flow until complete and then attempt sign in again.
     */
    RESET_PASSWORD,

    /**
     * The user account was signed up but never confirmed. Call
     * {@link com.amplifyframework.auth.AuthCategoryBehavior#resendSignUpCode(String, Consumer, Consumer)}
     * to get a new sign up confirmation code and then submit that using
     * {@link com.amplifyframework.auth.AuthCategoryBehavior#confirmSignUp(String, String, Consumer, Consumer)}
     * to confirm the user's account and then attempt sign in again.
     */
    CONFIRM_SIGN_UP,

    /**
     * User selects MFA type to use.
     * Call {@link com.amplifyframework.auth.AuthCategoryBehavior#confirmSignIn(String, Consumer, Consumer)}
     * with MFAType to select.
     */
    CONTINUE_SIGN_IN_WITH_MFA_SETUP_SELECTION,

    /**
     * Admin requires user to setup TOTP.
     * Call {@link com.amplifyframework.auth.AuthCategoryBehavior#confirmSignIn(String, Consumer, Consumer)}
     * with TOTP code to verify.
     */
    CONTINUE_SIGN_IN_WITH_TOTP_SETUP,

    /**
     * Admin requires user to setup email MFA.
     * Call {@link com.amplifyframework.auth.AuthCategoryBehavior#confirmSignIn(String, Consumer, Consumer)}
     * with email code to verify.
     */
    CONTINUE_SIGN_IN_WITH_EMAIL_MFA_SETUP,

    /**
     * The user account is required to set MFA selection.
     * Call {@link com.amplifyframework.auth.AuthCategoryBehavior#confirmSignIn(String, Consumer, Consumer)}
     * with preferred MFA option.
     */
    CONTINUE_SIGN_IN_WITH_MFA_SELECTION,

    /**
     * TOTP is enabled on this account and requires the user to confirm with the TOTP code.
     * Call {@link com.amplifyframework.auth.AuthCategoryBehavior#confirmSignIn(String, Consumer, Consumer)}
     * with TOTP Code.
     */
    CONFIRM_SIGN_IN_WITH_TOTP_CODE,

    /**
     * The user is required to select which form of first factor authentication to use
     * Call {@link com.amplifyframework.auth.AuthCategoryBehavior#confirmSignIn(String, Consumer, Consumer)}
     * with the preferred {@link com.amplifyframework.auth.AuthFactorType}.challengeResponse.
     */
    CONTINUE_SIGN_IN_WITH_FIRST_FACTOR_SELECTION,

    /**
     * MFA is enabled on this account and requires the user to confirm with the code received by
     * email, sms, etc.
     * Call {@link com.amplifyframework.auth.AuthCategoryBehavior#confirmSignIn(String, Consumer, Consumer)}
     * with the OTP code.
     */
    CONFIRM_SIGN_IN_WITH_OTP,

    /**
     * The user is required to provide a password for authentication
     * Call {@link com.amplifyframework.auth.AuthCategoryBehavior#confirmSignIn(String, Consumer, Consumer)}
     * with the password.
     */
    CONFIRM_SIGN_IN_WITH_PASSWORD,

    /**
     * No further steps are needed in the sign in flow.
     */
    DONE
}
