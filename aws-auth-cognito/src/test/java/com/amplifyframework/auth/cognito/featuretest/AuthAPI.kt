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

package com.amplifyframework.auth.cognito.featuretest

/**
 * List of APIs supported by Auth.
 * Note that case of ENUMs are not capitalized so as to serialze it into proper case
 * something like `resetPassword` instead of `RESET_PASSWORD`
 */
enum class AuthAPI {
    clearFederationToIdentityPool,
    configure,
    confirmResetPassword,
    confirmSignIn,
    confirmSignUp,
    confirmUserAttribute,
    deleteUser,
    federateToIdentityPool,
    fetchAuthSession,
    fetchDevices,
    fetchUserAttributes,
    forgetDevice,
    getCurrentUser,
    getEscapeHatch,
    getPluginKey,
    getVersion,
    handleWebUISignInResponse,
    rememberDevice,
    resendSignUpCode,
    resendUserAttributeConfirmationCode,
    resetPassword,
    signIn,
    signInWithSocialWebUI,
    signInWithWebUI,
    signOut,
    signUp,
    updatePassword,
    updateUserAttribute,
    updateUserAttributes
}
