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

import com.amplifyframework.auth.AuthCodeDeliveryDetails
import com.amplifyframework.auth.cognito.featuretest.generators.SerializableProvider
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.auth.result.step.AuthNextSignUpStep
import com.amplifyframework.auth.result.step.AuthSignUpStep
import com.amplifyframework.statemachine.codegen.data.AWSCredentials
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.AuthChallenge
import com.amplifyframework.statemachine.codegen.data.CognitoUserPoolTokens
import com.amplifyframework.statemachine.codegen.data.DeviceMetadata
import com.amplifyframework.statemachine.codegen.data.SignInMethod
import com.amplifyframework.statemachine.codegen.data.SignUpData
import com.amplifyframework.statemachine.codegen.data.SignedInData
import com.amplifyframework.statemachine.codegen.data.SignedOutData
import com.amplifyframework.statemachine.codegen.states.AuthState
import com.amplifyframework.statemachine.codegen.states.AuthenticationState
import com.amplifyframework.statemachine.codegen.states.AuthorizationState
import com.amplifyframework.statemachine.codegen.states.SignInChallengeState
import com.amplifyframework.statemachine.codegen.states.SignInState
import com.amplifyframework.statemachine.codegen.states.SignUpState
import java.util.Date

/**
 * Generates Json for given serializable class, this might be moved back to cognito auth due to added dependency on auth
 *
 */
object AuthStateJsonGenerator : SerializableProvider {
    const val dummyToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VySWQiLCJ1c2VybmFtZSI6InVzZXJuYW1l" +
        "IiwiZXhwIjoxNTE2MjM5MDIyLCJvcmlnaW5fanRpIjoib3JpZ2luX2p0aSJ9.Xqa-vjJe5wwwsqeRAdHf8kTBn_rYSkDn2lB7xj9Z1xU"

    const val dummyToken2 = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VySWQiLCJ1c2VybmFtZSI6InVzZXJuYW1l" +
        "IiwiZXhwIjoxNTE2MjM5MDI0LCJvcmlnaW5fanRpIjoib3JpZ2luX2p0aSJ9.po__hnGh2KF0ibpp--a2YZA9oBAKXc9BkX1IwdhvJp8"

    const val accessKeyId = "someAccessKey"
    const val secretAccessKey = "someSecretKey"
    const val identityId = "someIdentityId"
    const val expiration: Long = 2342134
    const val userId = "userId"

    const val username = "username"
    const val session = "session-id"
    val emptySession = null

    private val signedInData = SignedInData(
        userId = userId,
        username = username,
        signedInDate = Date(1707022800000),
        signInMethod = SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.USER_SRP_AUTH),
        cognitoUserPoolTokens = CognitoUserPoolTokens(
            idToken = dummyToken,
            accessToken = dummyToken,
            refreshToken = dummyToken,
            expiration = 300
        )
    )

    internal val signedInAmplifyCredential = AmplifyCredential.UserAndIdentityPool(
        signedInData,
        identityId = identityId,
        AWSCredentials(
            accessKeyId = accessKeyId,
            secretAccessKey = secretAccessKey,
            sessionToken = dummyToken,
            expiration = expiration
        )
    )

    private val signedInState = AuthState.Configured(
        AuthenticationState.SignedIn(signedInData, DeviceMetadata.Empty),
        AuthorizationState.SessionEstablished(signedInAmplifyCredential),
        SignUpState.NotStarted()
    )

    private val signedOutState = AuthState.Configured(
        AuthenticationState.SignedOut(SignedOutData(username)),
        AuthorizationState.Configured(),
        SignUpState.NotStarted()
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
                        ),
                    ),
                    SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.USER_SRP_AUTH)
                )
            )
        ),
        AuthorizationState.SigningIn(),
        SignUpState.NotStarted()
    )

    private val passwordlessSignUpAwaitingUserConfirmationState = AuthState.Configured(
        AuthenticationState.SignedOut(SignedOutData(username)),
        AuthorizationState.Configured(),
        SignUpState.AwaitingUserConfirmation(
            SignUpData(
                username,
                null,
                null,
                session,
                ""
            ),
            AuthSignUpResult(
                false,
                AuthNextSignUpStep(
                    AuthSignUpStep.CONFIRM_SIGN_UP_STEP,
                    emptyMap(),
                    AuthCodeDeliveryDetails(
                        "user@domain.com",
                        AuthCodeDeliveryDetails.DeliveryMedium.EMAIL,
                        "attributeName"
                    )
                ),
                "" // aligned with mock in CognitoMockFactory
            )
        )
    )

    private val nonPasswordlessSignUpAwaitingUserConfirmationState = AuthState.Configured(
        AuthenticationState.SignedOut(SignedOutData(username)),
        AuthorizationState.SessionEstablished(signedInAmplifyCredential),
        SignUpState.AwaitingUserConfirmation(
            SignUpData(
                username,
                null,
                null,
                emptySession,
                ""
            ),
            AuthSignUpResult(
                false,
                AuthNextSignUpStep(
                    AuthSignUpStep.CONFIRM_SIGN_UP_STEP,
                    emptyMap(),
                    AuthCodeDeliveryDetails(
                        "user@domain.com",
                        AuthCodeDeliveryDetails.DeliveryMedium.EMAIL,
                        "attributeName"
                    )
                ),
                "" // aligned with mock in CognitoMockFactory
            )
        )
    )

    private val passwordlessSignedUpState = AuthState.Configured(
        AuthenticationState.SignedOut(SignedOutData(username)),
        AuthorizationState.SessionEstablished(signedInAmplifyCredential),
        SignUpState.SignedUp(
            SignUpData(
                username,
                null,
                null,
                session,
                ""
            ),
            AuthSignUpResult(
                true,
                AuthNextSignUpStep(
                    AuthSignUpStep.COMPLETE_AUTO_SIGN_IN,
                    emptyMap(),
                    null
                ),
                "" // aligned with mock in CognitoMockFactory
            )
        )
    )

    private val receivedCustomChallengeState = AuthState.Configured(
        AuthenticationState.SigningIn(
            SignInState.ResolvingChallenge(
                SignInChallengeState.WaitingForAnswer(
                    AuthChallenge(
                        challengeName = "CUSTOM_CHALLENGE",
                        username = username,
                        session = "someSession",
                        parameters = mapOf(
                            "SALT" to "abc",
                            "SECRET_BLOCK" to "secretBlock",
                            "SRP_B" to "def",
                            "USERNAME" to "username"
                        )
                    ),
                    SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.CUSTOM_AUTH)
                )
            )
        ),
        AuthorizationState.SigningIn(),
        SignUpState.NotStarted()
    )

    override val serializables: List<Any> = listOf(
        signedInState,
        signedOutState,
        receivedChallengeState,
        passwordlessSignUpAwaitingUserConfirmationState,
        nonPasswordlessSignUpAwaitingUserConfirmationState,
        passwordlessSignedUpState
    )
}
