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

package com.amplifyframework.auth.cognito.featuretest.generators.authstategenerators

import com.amplifyframework.auth.cognito.featuretest.generators.SerializableProvider
import com.amplifyframework.statemachine.codegen.data.AWSCredentials
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.AuthChallenge
import com.amplifyframework.statemachine.codegen.data.CognitoUserPoolTokens
import com.amplifyframework.statemachine.codegen.data.DeviceMetadata
import com.amplifyframework.statemachine.codegen.data.SignInMethod
import com.amplifyframework.statemachine.codegen.data.SignedInData
import com.amplifyframework.statemachine.codegen.data.SignedOutData
import com.amplifyframework.statemachine.codegen.states.AuthState
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import com.amplifyframework.statemachine.codegen.states.AuthorizationState
import com.amplifyframework.statemachine.codegen.states.SignInChallengeState
import com.amplifyframework.statemachine.codegen.states.SignInState
import java.time.Instant
import java.util.Date

/**
 * Generates Json for given serializable class, this might be moved back to cognito auth due to added dependency on auth
 *
 */
object AuthStateJsonGenerator : SerializableProvider {
    const val dummyToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VySWQiLCJ1c2VybmFtZSI6InVzZXJuYW1l" +
        "IiwiZXhwIjoxNTE2MjM5MDIyfQ.WK8_fwfvSU7wdDl5sovnRApzUp6FHNB5ljS8KAO0QeA"

    private const val username = "username"

    private val signedInData = SignedInData(
        userId = "userId",
        username = username,
        signedInDate = Date.from(Instant.ofEpochSecond(0)),
        signInMethod = SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.USER_SRP_AUTH),
        cognitoUserPoolTokens = CognitoUserPoolTokens(
            idToken = "someToken",
            accessToken = dummyToken,
            refreshToken = "someRefreshToken",
            expiration = 300
        )
    )

    private val signedInState = AuthState.Configured(
        AuthenticationState.SignedIn(signedInData, DeviceMetadata.Empty),
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
        AuthenticationState.SignedOut(SignedOutData(username)),
        AuthorizationState.Configured()
    )

    private val receivedChallengeState = AuthState.Configured(
        AuthenticationState.SigningIn(
            SignInState.ResolvingChallenge(
                SignInChallengeState.WaitingForAnswer(
                    AuthChallenge(
                        challengeName = "SMS_MFA",
                        username = username,
                        session = "someSession",
                        parameters = mapOf(
                            "CODE_DELIVERY_DELIVERY_MEDIUM" to "SMS",
                            "CODE_DELIVERY_DESTINATION" to "+12345678900"
                        )
                    )
                )
            )
        ),
        AuthorizationState.SigningIn()
    )

    override val serializables: List<Any> = listOf(signedInState, signedOutState, receivedChallengeState)
}
