/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.auth.cognito.exceptions.webauthn

/**
 * This Exception indicates that there is was a problem verifying your application against the configured relying party
 * in your User Pool. This could be because your application has a different package or signing key than the ones
 * specified in the deployed assetlinks.json file. For more details about this file please refer to the
 * Android documentation here: https://developer.android.com/identity/sign-in/credential-manager#add-support-dal
 */
class WebAuthnRpMismatchException internal constructor(cause: Throwable?) :
    WebAuthnFailedException(
        message = "Unable to verify Relying Party data",
        recoverySuggestion =
        "Check that you have a valid assetlinks.json file deployed to your RP that specifies the " +
            "correct package name, signing key fingerprints, and grants permission for " +
            "delegate_permission/common.get_login_creds. See Android Credential Manager documentation for details.",
        cause = cause
    )
