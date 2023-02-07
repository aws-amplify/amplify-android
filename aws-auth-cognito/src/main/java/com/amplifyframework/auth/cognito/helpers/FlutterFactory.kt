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

package com.amplifyframework.auth.cognito.helpers

import com.amplifyframework.auth.AWSCognitoUserPoolTokens
import com.amplifyframework.auth.AWSCredentials
import com.amplifyframework.auth.cognito.AWSCognitoAuthSession
import com.amplifyframework.auth.result.AuthSessionResult

/**
 * Although this has public access, it is intended for internal use and should not be used directly by host
 * applications. The behavior of this may change without warning.
 */
object FlutterFactory {

    fun createAWSCognitoUserPoolTokens(
        accessToken: String?,
        idToken: String?,
        refreshToken: String?
    ) = AWSCognitoUserPoolTokens(accessToken, idToken, refreshToken)

    fun createAWSCognitoAuthSession(
        isSignedIn: Boolean,
        identityIdResult: AuthSessionResult<String>,
        awsCredentialsResult: AuthSessionResult<AWSCredentials>,
        userSubResult: AuthSessionResult<String>,
        userPoolTokensResult: AuthSessionResult<AWSCognitoUserPoolTokens>
    ) = AWSCognitoAuthSession(
        isSignedIn,
        identityIdResult,
        awsCredentialsResult,
        userSubResult,
        userPoolTokensResult
    )
}
