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
     * No further steps are needed in the sign in flow.
     */
    DONE
}
