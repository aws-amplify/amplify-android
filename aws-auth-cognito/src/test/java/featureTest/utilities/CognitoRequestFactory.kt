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

import aws.sdk.kotlin.services.cognitoidentityprovider.model.AttributeType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.AuthFlowType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ConfirmSignUpRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ForgotPasswordRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.InitiateAuthRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.SignUpRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.GetTokensFromRefreshTokenRequest
import com.amplifyframework.auth.cognito.featuretest.ExpectationShapes
import com.amplifyframework.auth.cognito.helpers.AuthHelper
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * Factory to generate request object for aws SDK's cognito APIs
 */
object CognitoRequestFactory {
    fun getExpectedRequestFor(targetApi: ExpectationShapes.Cognito): Any = when (targetApi.apiName) {
        "forgotPassword" -> {
            val params = targetApi.request as JsonObject
            val expectedRequestBuilder: ForgotPasswordRequest.Builder.() -> Unit = {
                username = (params["username"] as JsonPrimitive).content
                clientMetadata =
                    Json.decodeFromJsonElement<Map<String, String>>(params["clientMetadata"] as JsonObject)
                clientId = (params["clientId"] as JsonPrimitive).content
                secretHash = (params["secretHash"] as JsonPrimitive).content
            }

            ForgotPasswordRequest.invoke(expectedRequestBuilder)
        }

        "confirmSignUp" -> {
            val params = targetApi.request as JsonObject
            val expectedRequest: ConfirmSignUpRequest.Builder.() -> Unit = {
                clientId = (params["clientId"] as JsonPrimitive).content
                username = (params["username"] as JsonPrimitive).content
                confirmationCode = (params["confirmationCode"] as JsonPrimitive).content
                session = (params["session"] as? JsonPrimitive)?.content

                secretHash = AuthHelper.getSecretHash("", "", "")
            }
            ConfirmSignUpRequest.invoke(expectedRequest)
        }

        "initiateAuth" -> {
            val params = targetApi.request as JsonObject
            val expectedRequestBuilder: InitiateAuthRequest.Builder.() -> Unit = {
                authFlow = AuthFlowType.fromValue((params["authFlow"] as JsonPrimitive).content)
                clientId = (params["clientId"] as JsonPrimitive).content
                authParameters =
                    Json.decodeFromJsonElement<Map<String, String>>(params["authParameters"] as JsonObject)
                session = (params["session"] as JsonPrimitive).content
                clientMetadata =
                    Json.decodeFromJsonElement<Map<String, String>>(params["clientMetadata"] as JsonObject)
            }
            InitiateAuthRequest.invoke(expectedRequestBuilder)
        }

        "signUp" -> {
            val params = targetApi.request as JsonObject
            val expectedRequest: SignUpRequest.Builder.() -> Unit = {
                clientId = (params["clientId"] as JsonPrimitive).content
                username = (params["username"] as JsonPrimitive).content
                password = (params["password"] as JsonPrimitive).content

                /*
                 * "userAttributes": [
                      {
                        "name": "email",
                        "value": "user@domain.com"
                      }
                    ]
                 */
                userAttributes = (params["userAttributes"] as JsonArray).mapNotNull {
                    val entry = it as JsonObject
                    AttributeType {
                        name = (entry["name"] as JsonPrimitive).content
                        value = (entry["value"] as JsonPrimitive).content
                    }
                }
                secretHash = AuthHelper.getSecretHash("", "", "")
            }
            SignUpRequest.invoke(expectedRequest)
        }

        "getTokensFromRefreshToken" -> {
            val params = targetApi.request as JsonObject
            val expectedRequestBuilder: GetTokensFromRefreshTokenRequest.Builder.() -> Unit = {
                refreshToken = (params["refreshToken"] as JsonPrimitive).content
                clientId = (params["clientId"] as JsonPrimitive).content
                clientSecret = (params["clientSecret"] as? JsonPrimitive)?.content
                deviceKey = (params["deviceKey"] as? JsonPrimitive)?.content
                clientMetadata = params["clientMetadata"]?.let {
                    Json.decodeFromJsonElement<Map<String, String>>(it as JsonObject)
                }
            }
            GetTokensFromRefreshTokenRequest.invoke(expectedRequestBuilder)
        }

        else -> error("Expected request for $targetApi for Cognito is not defined")
    }
}
