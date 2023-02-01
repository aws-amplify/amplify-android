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
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ForgotPasswordRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.SignUpRequest
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

        else -> error("Expected request for $targetApi for Cognito is not defined")
    }
}
