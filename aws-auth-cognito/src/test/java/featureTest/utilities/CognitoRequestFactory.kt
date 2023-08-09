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

import aws.sdk.kotlin.services.cognitoidentityprovider.model.AnalyticsMetadataType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.AttributeType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ForgotPasswordRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.SignUpRequest
import aws.smithy.kotlin.runtime.content.Document
import aws.smithy.kotlin.runtime.util.length
import com.amplifyframework.auth.cognito.featuretest.ExpectationShapes
import com.amplifyframework.auth.cognito.helpers.AuthHelper
import generated.model.Cognito
import generated.model.Response
import generated.model.Validation
import io.mockk.InternalPlatformDsl.toStr
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import org.json.JSONArray
import org.json.JSONObject


/**
 * Factory to generate request object for aws SDK's cognito APIs
 */
object CognitoRequestFactory {
    fun getExpectedRequestFor(apiName : String, targetApi: Map<String, Document?>): Any = when (apiName) {
        "forgotPassword" -> {
            val params = JSONObject(targetApi["request"]!!.asString())
            val expectedRequestBuilder: ForgotPasswordRequest.Builder.() -> Unit = {
                username = (params["username"]!!.toStr())
                //clientMetadata =
                    //Json.decodeFromJsonElement<Map<String, String>>(params["clientMetadata"] as JsonObject)
                clientId = "app Client Id"
                secretHash = AuthHelper.getSecretHash("", "", "")

                analyticsMetadata = AnalyticsMetadataType.invoke {
                    analyticsEndpointId = "test-endpoint-id"
                }
                clientMetadata = mapOf()
            }



            ForgotPasswordRequest.invoke(expectedRequestBuilder)

        }

        "signUp" -> {
            val params = JSONObject(targetApi["request"]!!.asString())

            val expectedRequest: SignUpRequest.Builder.() -> Unit = {

                clientId = "app Client Id"//(params["clientId"]!!.toStr())
                username = (params["username"]!!.toStr())
                password = (params["password"]!!.toStr())

                    /*
                     * "userAttributes": [
                          {

                            "name": "email",
                            "value": "user@domain.com"
                          }
                        ]
                     */
                var curr = listOf<AttributeType>()
                val curren = JSONArray(params["userAttributes"].toStr())

                for (i in 0 until curren.length()){
                    val att = AttributeType {
                        name = JSONObject(curren[i].toStr())["name"].toStr()
                        value = JSONObject(curren[i].toStr())["value"].toStr()
                    }
                    curr = curr + att

                }

                userAttributes = curr
                secretHash = AuthHelper.getSecretHash("", "", "")

                analyticsMetadata = AnalyticsMetadataType.invoke {
                    analyticsEndpointId = "test-endpoint-id"
                }


            }
            SignUpRequest.invoke(expectedRequest)
        }
        else -> {
            error("Expected request for $targetApi for Cognito is undefined")
        }


    }




}

