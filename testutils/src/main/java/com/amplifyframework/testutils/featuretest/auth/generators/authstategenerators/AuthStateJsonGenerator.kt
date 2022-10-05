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

package com.amplifyframework.testutils.featuretest.auth.generators.authstategenerators

import com.amplifyframework.statemachine.codegen.data.AWSCredentials
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.CognitoUserPoolTokens
import com.amplifyframework.statemachine.codegen.data.DeviceMetadata
import com.amplifyframework.statemachine.codegen.data.SignInMethod
import com.amplifyframework.statemachine.codegen.data.SignedInData
import com.amplifyframework.statemachine.codegen.data.SignedOutData
import com.amplifyframework.statemachine.codegen.states.AuthState
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import com.amplifyframework.statemachine.codegen.states.AuthorizationState
import com.amplifyframework.testutils.featuretest.auth.generators.SerializableProvider
import java.time.Instant
import java.util.Date

/**
 * Generates Json for given serializable class, this might be moved back to cognito auth due to added dependency on auth
 *
 */
object AuthStateJsonGenerator : SerializableProvider {
    private val signedInData = SignedInData(
        userId = "userId",
        username = "username",
        signedInDate = Date.from(Instant.ofEpochSecond(324234123)),
        signInMethod = SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.USER_SRP_AUTH),
        deviceMetadata = DeviceMetadata.Metadata(
            deviceKey = "someDeviceKey",
            deviceGroupKey = "someDeviceGroupKey",
            deviceSecret = "someSecret"
        ),
        cognitoUserPoolTokens = CognitoUserPoolTokens(
            idToken = "someToken",
            accessToken = "someAccessToken",
            refreshToken = "someRefreshToken",
            expiration = 300
        )
    )

    private val signedInState = AuthState.Configured(
        AuthenticationState.SignedIn(signedInData),
        AuthorizationState.SessionEstablished(
            AmplifyCredential.UserAndIdentityPool(
                signedInData,
                identityId = "someIdentityId",
                AWSCredentials(
                    accessKeyId = "someAccessKey",
                    secretAccessKey = "someSecretKey",
                    sessionToken = "someSessionToken",
                    expiration = 2342134
                )
            )
        )
    )

    private val signedOutState = AuthState.Configured(
        AuthenticationState.SignedOut(SignedOutData("username")),
        AuthorizationState.Configured()
    )

    override val serializables: List<Any> = listOf(signedInState, signedOutState)
}
