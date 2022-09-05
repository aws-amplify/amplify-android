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

package featureTest.utilities

import aws.sdk.kotlin.services.cognitoidentityprovider.CognitoIdentityProviderClient
import aws.sdk.kotlin.services.cognitoidentityprovider.model.CodeDeliveryDetailsType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.CognitoIdentityProviderException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.DeliveryMediumType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ForgotPasswordRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ForgotPasswordResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.SignUpRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.SignUpResponse
import com.amplifyframework.auth.cognito.helpers.AuthHelper
import com.amplifyframework.testutils.featuretest.MockResponse
import com.amplifyframework.testutils.featuretest.ResponseType
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.mockkObject
import io.mockk.slot
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Factory to mock aws sdk's cognito API calls and responses.
 */
class CognitoMockFactory(private val mockCognitoIPClient: CognitoIdentityProviderClient) {
    private val captures: MutableMap<String, CapturingSlot<*>> = mutableMapOf()

    fun mock(mockResponse: MockResponse) {
        val responseObject = mockResponse.response as JsonObject
        fun expectedException(responseObject: JsonObject) =
            CognitoIdentityProviderException((responseObject["message"] as JsonPrimitive).content)

        return when (mockResponse.apiName) {
            "forgotPassword" -> {
                val requestBuilderCaptor = slot<ForgotPasswordRequest.Builder.() -> Unit>()

                coEvery { mockCognitoIPClient.forgotPassword(capture(requestBuilderCaptor)) } coAnswers {
                    if (mockResponse.responseType == ResponseType.Failure) {
                        throw expectedException(responseObject)
                    }
                    ForgotPasswordResponse.invoke {
                        this.codeDeliveryDetails = parseCodeDeliveryDetails(responseObject)
                    }
                }

                captures[mockResponse.apiName] = requestBuilderCaptor
            }
            "signUp" -> {
                mockkObject(AuthHelper)
                coEvery { AuthHelper.getSecretHash(any(), any(), any()) } returns "a hash"

                val requestCaptor = slot<SignUpRequest.Builder.() -> Unit>()

                coEvery { mockCognitoIPClient.signUp(capture(requestCaptor)) } coAnswers {
                    if (mockResponse.responseType == ResponseType.Failure) {
                        throw expectedException(responseObject)
                    }
                    SignUpResponse.invoke {
                        this.codeDeliveryDetails = parseCodeDeliveryDetails(responseObject)
                    }
                }
                captures[mockResponse.apiName] = requestCaptor
            }

            else -> throw Error("mock for ${mockResponse.apiName} not defined!")
        }
    }

    private fun parseCodeDeliveryDetails(response: JsonObject): CodeDeliveryDetailsType {
        val codeDeliveryDetails = response["codeDeliveryDetails"] as JsonObject

        return CodeDeliveryDetailsType.invoke {
            destination = (codeDeliveryDetails["destination"] as JsonPrimitive).content
            deliveryMedium =
                DeliveryMediumType.fromValue((codeDeliveryDetails["deliveryMedium"] as JsonPrimitive).content)
            attributeName = (codeDeliveryDetails["attributeName"] as JsonPrimitive).content
        }
    }

    fun getActualResultFor(apiName: String): Any = when (apiName) {
        "forgotPassword" -> {
            val capturedVal = captures[apiName]?.captured as ForgotPasswordRequest.Builder.() -> Unit
            ForgotPasswordRequest.invoke(capturedVal)
        }
        "signUp" -> {
            val capturedVal = captures[apiName]?.captured as SignUpRequest.Builder.() -> Unit
            SignUpRequest.invoke(capturedVal)
        }
        else -> Error("Actual result for $apiName is not defined")
    }
}
