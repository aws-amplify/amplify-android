/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import aws.sdk.kotlin.services.cognitoidentityprovider.model.NotAuthorizedException
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
import kotlinx.serialization.json.JsonObject

object ResetPasswordTestCaseGenerator : SerializableProvider {
    private val mockCognitoResponse = MockResponse(
        CognitoType.CognitoIdentityProvider,
        "forgotPassword",
        ResponseType.Success,
        mapOf(
            "codeDeliveryDetails" to mapOf(
                "destination" to "dummy destination",
                "deliveryMedium" to "EMAIL",
                "attributeName" to "dummy attribute"
            )
        ).toJsonElement()
    )

    private val codeDeliveryDetails = mapOf(
        "destination" to "dummy destination",
        "deliveryMedium" to "EMAIL",
        "attributeName" to "dummy attribute"
    )

    private val expectedSuccess =
        mapOf(
            "isPasswordReset" to false,
            "nextStep" to
                mapOf(
                    "resetPasswordStep" to "CONFIRM_RESET_PASSWORD_WITH_CODE",
                    "additionalInfo" to emptyMap<String, String>(),
                    "codeDeliveryDetails" to codeDeliveryDetails
                )
        ).toJsonElement()

    private val cognitoValidation = ExpectationShapes.Cognito.CognitoIdentityProvider(
        "forgotPassword",
        mapOf(
            "username" to "someUsername",
            "clientId" to "testAppClientId",
            "secretHash" to "a hash",
            "clientMetadata" to emptyMap<String, String>()
        ).toJsonElement()
    )

    private val apiReturnValidation = ExpectationShapes.Amplify(
        AuthAPI.resetPassword,
        ResponseType.Success,
        expectedSuccess,
    )
    private val finalStateValidation = ExpectationShapes.State("AuthenticationState_SignedIn.json")

    private val baseCase = FeatureTestCase(
        description = "Test that Cognito is called with given payload and returns successful data",
        preConditions = PreConditions(
            "authconfiguration.json",
            "SignedOut_Configured.json",
            mockedResponses = listOf()
        ),
        api = API(
            AuthAPI.resetPassword,
            mapOf("username" to "someUsername").toJsonElement(),
            JsonObject(emptyMap())
        ),
        validations = listOf(cognitoValidation)
    )

    private val successCase: FeatureTestCase = baseCase.copy(
        description = "AuthResetPasswordResult object is returned when reset password succeeds",
        preConditions = baseCase.preConditions.copy(mockedResponses = listOf(mockCognitoResponse)),
        validations = baseCase.validations.plus(apiReturnValidation)
    )

    private val errorCase: FeatureTestCase
        get() {
            val errorResponse = NotAuthorizedException.invoke { message = "Cognito error message" }
            return baseCase.copy(
                description = "AuthException is thrown when forgotPassword API call fails",
                preConditions = baseCase.preConditions.copy(
                    mockedResponses = listOf(
                        MockResponse(
                            CognitoType.CognitoIdentityProvider,
                            "forgotPassword",
                            ResponseType.Failure,
                            errorResponse.toJsonElement()
                        )
                    )
                ),
                validations = listOf(
                    ExpectationShapes.Amplify(
                        AuthAPI.resetPassword,
                        ResponseType.Failure,
                        com.amplifyframework.auth.exceptions.NotAuthorizedException(
                            cause = errorResponse
                        ).toJsonElement(),
                    )
                )
            )
        }

    override val serializables: List<Any> = listOf(baseCase, errorCase, successCase)
}
