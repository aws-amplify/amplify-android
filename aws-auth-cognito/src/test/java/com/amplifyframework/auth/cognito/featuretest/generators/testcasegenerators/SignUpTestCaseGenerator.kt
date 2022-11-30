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

package com.amplifyframework.auth.cognito.featuretest.generators.testcasegenerators

import com.amplifyframework.auth.AuthCodeDeliveryDetails
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.cognito.featuretest.API
import com.amplifyframework.auth.cognito.featuretest.AuthAPI
import com.amplifyframework.auth.cognito.featuretest.CognitoType
import com.amplifyframework.auth.cognito.featuretest.ExpectationShapes
import com.amplifyframework.auth.cognito.featuretest.FeatureTestCase
import com.amplifyframework.auth.cognito.featuretest.MockResponse
import com.amplifyframework.auth.cognito.featuretest.PreConditions
import com.amplifyframework.auth.cognito.featuretest.ResponseType
import com.amplifyframework.auth.cognito.featuretest.generators.SerializableProvider
import com.amplifyframework.auth.cognito.featuretest.generators.toJsonElement
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.auth.result.step.AuthNextSignUpStep
import com.amplifyframework.auth.result.step.AuthSignUpStep

object SignUpTestCaseGenerator : SerializableProvider {
    private val username = "user"
    private val password = "password"
    private val email = "user@domain.com"

    private val codeDeliveryDetails = mapOf(
        "destination" to email,
        "deliveryMedium" to "EMAIL",
        "attributeName" to "attributeName"
    )

    private val emptyCodeDeliveryDetails = mapOf(
        "destination" to "",
        "deliveryMedium" to "",
        "attributeName" to ""
    )

    val baseCase = FeatureTestCase(
        description = "Test that signup invokes proper cognito request and returns success",
        preConditions = PreConditions(
            "authconfiguration.json",
            "SignedOut_Configured.json",
            mockedResponses = listOf(
                MockResponse(
                    CognitoType.CognitoIdentityProvider,
                    "signUp",
                    ResponseType.Success,
                    mapOf("codeDeliveryDetails" to codeDeliveryDetails).toJsonElement()
                )
            )
        ),
        api = API(
            AuthAPI.signUp,
            params = mapOf(
                "username" to username,
                "password" to password
            ).toJsonElement(),
            options = mapOf(
                "userAttributes" to mapOf(AuthUserAttributeKey.email().keyString to email)
            ).toJsonElement()
        ),
        validations = listOf(
            ExpectationShapes.Cognito.CognitoIdentityProvider(
                apiName = "signUp",
                // see [https://docs.aws.amazon.com/cognito-user-identity-pools/latest/APIReference/API_SignUp.html]
                request = mapOf(
                    "clientId" to "testAppClientId", // This should be pulled from configuration
                    "username" to username,
                    "password" to password,
                    "userAttributes" to listOf(mapOf("name" to "email", "value" to email))
                ).toJsonElement()
            ),
            ExpectationShapes.Amplify(
                apiName = AuthAPI.signUp,
                responseType = ResponseType.Success,
                response =
                    AuthSignUpResult(
                        false,
                        AuthNextSignUpStep(
                            AuthSignUpStep.CONFIRM_SIGN_UP_STEP,
                            emptyMap(),
                            AuthCodeDeliveryDetails(
                                email,
                                AuthCodeDeliveryDetails.DeliveryMedium.EMAIL,
                                "attributeName"
                            )
                        ),
                        null
                    ).toJsonElement()
            )
        )
    )

    val signupSuccessCase = baseCase.copy(
        description = "Sign up finishes if user is confirmed in the first step",
        preConditions = baseCase.preConditions.copy(
            mockedResponses = listOf(
                MockResponse(
                    CognitoType.CognitoIdentityProvider,
                    "signUp",
                    ResponseType.Success,
                    mapOf("codeDeliveryDetails" to emptyCodeDeliveryDetails, "userConfirmed" to true).toJsonElement()
                )
            )
        ),
        validations = listOf(
            ExpectationShapes.Cognito.CognitoIdentityProvider(
                apiName = "signUp",
                // see [https://docs.aws.amazon.com/cognito-user-identity-pools/latest/APIReference/API_SignUp.html]
                request = mapOf(
                    "clientId" to "testAppClientId", // This should be pulled from configuration
                    "username" to username,
                    "password" to password,
                    "userAttributes" to listOf(mapOf("name" to "email", "value" to email))
                ).toJsonElement()
            ),
            ExpectationShapes.Amplify(
                apiName = AuthAPI.signUp,
                responseType = ResponseType.Success,
                response =
                    AuthSignUpResult(
                        true,
                        AuthNextSignUpStep(
                            AuthSignUpStep.DONE,
                            emptyMap(),
                            null
                        ),
                        null
                    ).toJsonElement()
            )
        )
    )

    override val serializables: List<Any> = listOf(baseCase, signupSuccessCase)
}
