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
import aws.sdk.kotlin.services.cognitoidentityprovider.model.AttributeType
import com.amplifyframework.auth.AuthUserAttribute
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
import com.amplifyframework.auth.exceptions.SignedOutException
import kotlinx.serialization.json.JsonObject

object FetchUserAttributesTestCaseGenerator : SerializableProvider {

    private val expectedSuccess = listOf<AuthUserAttribute>(
        AuthUserAttribute(AuthUserAttributeKey.email(), "email@email.com"),
        AuthUserAttribute(AuthUserAttributeKey.phoneNumber(), "000-000-0000")
    ).toJsonElement()

    private val mockCognitoResponse = MockResponse(
        CognitoType.CognitoIdentityProvider,
        "getUser",
        ResponseType.Success,
        mapOf(
            "userAttributes" to listOf<AttributeType>(
                AttributeType.invoke {
                    name = "email"
                    value = "email@email.com"
                },
                AttributeType.invoke {
                    name = "phone"
                    value = "000-000-0000"
                }
            )
        ).toJsonElement()
    )

    private val apiReturnValidation = ExpectationShapes.Amplify(
        AuthAPI.fetchUserAttributes,
        ResponseType.Success,
        expectedSuccess,
    )

    private val baseCase = FeatureTestCase(
        description = "Test that Cognito is called with given payload and returns successful data",
        preConditions = PreConditions(
            "authconfiguration.json",
            "SignedIn_SessionEstablished.json",
            mockedResponses = listOf(mockCognitoResponse)
        ),
        api = API(
            AuthAPI.fetchUserAttributes,
            JsonObject(emptyMap()),
            JsonObject(emptyMap()),
        ),
        validations = listOf(apiReturnValidation)
    )

    private val successCase: FeatureTestCase = baseCase.copy(
        description = "List of user attributes returned when fetch user attributes API succeeds",
        preConditions = baseCase.preConditions.copy(mockedResponses = listOf(mockCognitoResponse)),
        validations = baseCase.validations.plus(apiReturnValidation)
    )

    private val errorCase: FeatureTestCase
        get() {
            val errorResponse = SignedOutException()
            return baseCase.copy(
                description = "AuthException is thrown when fetchUserAttributes API is called without signing in",
                preConditions = baseCase.preConditions.copy(
                    state = "SignedOut_Configured.json",
                    mockedResponses = listOf(
                        MockResponse(
                            CognitoType.CognitoIdentityProvider,
                            "getUser",
                            ResponseType.Failure,
                            errorResponse.toJsonElement()
                        )
                    )
                ),
                validations = listOf(
                    ExpectationShapes.Amplify(
                        AuthAPI.fetchUserAttributes,
                        ResponseType.Failure,
                        SignedOutException().toJsonElement(),
                    )
                )
            )
        }

    override val serializables: List<Any> = listOf(baseCase, errorCase, successCase)
}
