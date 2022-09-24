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

package com.amplifyframework.testutils.featuretest.auth.generators.testcasegenerators

import aws.sdk.kotlin.services.cognitoidentityprovider.model.AuthFlowType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ChallengeNameType
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.result.step.AuthSignUpStep
import com.amplifyframework.testutils.featuretest.API
import com.amplifyframework.testutils.featuretest.ExpectationShapes
import com.amplifyframework.testutils.featuretest.FeatureTestCase
import com.amplifyframework.testutils.featuretest.MockResponse
import com.amplifyframework.testutils.featuretest.PreConditions
import com.amplifyframework.testutils.featuretest.ResponseType
import com.amplifyframework.testutils.featuretest.auth.AuthAPI
import com.amplifyframework.testutils.featuretest.auth.generators.SerializableProvider
import com.amplifyframework.testutils.featuretest.auth.generators.toJsonElement
import kotlinx.serialization.json.JsonObject

object SignInTestCaseGenerator : SerializableProvider {
    private val userId = "userId"
    private val username = "username"
    private val password = "password"

    val baseCase = FeatureTestCase(
        description = "Test that SRP signIn invokes proper cognito request and returns success",
        preConditions = PreConditions(
            "authconfiguration.json",
            "SignedOut_Configured.json",
            mockedResponses = listOf(
                MockResponse(
                    "cognito",
                    "initiateAuth",
                    ResponseType.Success,
                    mapOf(
                        "challengeName" to ChallengeNameType.PasswordVerifier,
                        "challengeParameters" to mapOf(
                            "SALT" to "abc",
                            "SECRET_BLOCK" to "secretBlock",
                            "SRP_B" to "def",
                            "USERNAME" to username,
                            "USER_ID_FOR_SRP" to userId
                        )
                    ).toJsonElement()
                ),
                MockResponse(
                    "cognito",
                    "respondToAuthChallenge",
                    ResponseType.Success,
                    mapOf(
                        "authenticationResult" to mapOf(
                            "idToken" to "someToken",
                            "accessToken" to "someAccessToken",
                            "refreshToken" to "someRefreshToken",
                            "expiresIn" to 300
                        )
                    ).toJsonElement()
                )
            )
        ),
        api = API(
            AuthAPI.signIn,
            params = mapOf(
                "username" to username,
                "password" to password
            ).toJsonElement(),
            options = JsonObject(emptyMap())
        ),
        validations = listOf(
            ExpectationShapes.Cognito(
                apiName = "signIn",
                // see [https://docs.aws.amazon.com/cognito-user-identity-pools/latest/APIReference/API_SignUp.html]
                request = mapOf(
                    "clientId" to "testAppClientId", // This should be pulled from configuration
                    "authFlow" to AuthFlowType.UserSrpAuth,
                    "authParameters" to mapOf("username" to username, "SRP_A" to "123")
                ).toJsonElement()
            ),
//            ExpectationShapes.Amplify(
//                apiName = AuthAPI.signIn,
//                responseType = ResponseType.Success,
//                response = mapOf(
//                    "user" to mapOf(
//                        "userId" to "",
//                        "username" to username
//                    )
//                ).toJsonElement()
//            ),
            ExpectationShapes.State("SignedIn_SessionEstablished.json")
        )
    )

    override val serializables: List<Any> = listOf(baseCase)
}
