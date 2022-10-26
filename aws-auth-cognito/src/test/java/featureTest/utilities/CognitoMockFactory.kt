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

import aws.sdk.kotlin.services.cognitoidentity.CognitoIdentityClient
import aws.sdk.kotlin.services.cognitoidentity.model.Credentials
import aws.sdk.kotlin.services.cognitoidentity.model.GetCredentialsForIdentityRequest
import aws.sdk.kotlin.services.cognitoidentity.model.GetCredentialsForIdentityResponse
import aws.sdk.kotlin.services.cognitoidentity.model.GetIdRequest
import aws.sdk.kotlin.services.cognitoidentity.model.GetIdResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.CognitoIdentityProviderClient
import aws.sdk.kotlin.services.cognitoidentityprovider.model.AuthenticationResultType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ChallengeNameType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.CodeDeliveryDetailsType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.DeleteUserRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.DeleteUserResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.DeliveryMediumType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ForgotPasswordRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ForgotPasswordResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.GlobalSignOutRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.GlobalSignOutResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.InitiateAuthRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.InitiateAuthResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.RespondToAuthChallengeRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.RespondToAuthChallengeResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.RevokeTokenRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.RevokeTokenResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.SignUpRequest
import aws.sdk.kotlin.services.cognitoidentityprovider.model.SignUpResponse
import aws.smithy.kotlin.runtime.time.Instant
import com.amplifyframework.auth.cognito.featuretest.MockResponse
import com.amplifyframework.auth.cognito.featuretest.ResponseType
import com.amplifyframework.auth.cognito.featuretest.serializers.CognitoIdentityExceptionSerializer
import com.amplifyframework.auth.cognito.featuretest.serializers.CognitoIdentityProviderExceptionSerializer
import com.amplifyframework.auth.cognito.helpers.AuthHelper
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.mockkObject
import io.mockk.slot
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Factory to mock aws sdk's cognito API calls and responses.
 */
class CognitoMockFactory(
    private val mockCognitoIPClient: CognitoIdentityProviderClient,
    private val mockCognitoIdClient: CognitoIdentityClient
) {
    private val captures: MutableMap<String, CapturingSlot<*>> = mutableMapOf()

    fun mock(mockResponse: MockResponse) {
        val responseObject = mockResponse.response as JsonObject

        return when (mockResponse.apiName) {
            "forgotPassword" -> {
                val requestBuilderCaptor = slot<ForgotPasswordRequest>()

                coEvery { mockCognitoIPClient.forgotPassword(capture(requestBuilderCaptor)) } coAnswers {
                    if (mockResponse.responseType == ResponseType.Failure) {
                        throw Json.decodeFromString(
                            CognitoIdentityProviderExceptionSerializer,
                            responseObject.toString()
                        )
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

                val requestCaptor = slot<SignUpRequest>()

                coEvery { mockCognitoIPClient.signUp(capture(requestCaptor)) } coAnswers {
                    if (mockResponse.responseType == ResponseType.Failure) {
                        throw Json.decodeFromString(
                            CognitoIdentityProviderExceptionSerializer,
                            responseObject.toString()
                        )
                    }
                    SignUpResponse.invoke {
                        this.codeDeliveryDetails = parseCodeDeliveryDetails(responseObject)
                    }
                }
                captures[mockResponse.apiName] = requestCaptor
            }
            "initiateAuth" -> {
                mockkObject(AuthHelper)
                coEvery { AuthHelper.getSecretHash(any(), any(), any()) } returns "a hash"

                val requestCaptor = slot<InitiateAuthRequest>()

                coEvery { mockCognitoIPClient.initiateAuth(capture(requestCaptor)) } coAnswers {
                    if (mockResponse.responseType == ResponseType.Failure) {
                        throw Json.decodeFromString(
                            CognitoIdentityProviderExceptionSerializer,
                            responseObject.toString()
                        )
                    }
                    InitiateAuthResponse.invoke {
                        this.challengeName = responseObject["challengeName"]?.let {
                            ChallengeNameType.fromValue((it as JsonPrimitive).content)
                        }

                        this.challengeParameters = responseObject["challengeParameters"]?.let {
                            parseChallengeParams(it as JsonObject)
                        }
                    }
                }
                captures[mockResponse.apiName] = requestCaptor
            }
            "respondToAuthChallenge" -> {
                mockkObject(AuthHelper)
                coEvery { AuthHelper.getSecretHash(any(), any(), any()) } returns "a hash"

                val requestCaptor = slot<RespondToAuthChallengeRequest>()

                coEvery { mockCognitoIPClient.respondToAuthChallenge(capture(requestCaptor)) } coAnswers {
                    if (mockResponse.responseType == ResponseType.Failure) {
                        throw Json.decodeFromString(
                            CognitoIdentityProviderExceptionSerializer,
                            responseObject.toString()
                        )
                    }
                    RespondToAuthChallengeResponse.invoke {
                        this.authenticationResult = responseObject["authenticationResult"]?.let {
                            parseAuthenticationResult(it as JsonObject)
                        }
                        this.session = responseObject["session"]?.let { (it as JsonPrimitive).content }
                        this.challengeName = responseObject["challengeName"]?.let {
                            ChallengeNameType.fromValue((it as JsonPrimitive).content)
                        }
                        this.challengeParameters = responseObject["challengeParameters"]?.let {
                            parseChallengeParams(it as JsonObject)
                        }
                    }
                }
                captures[mockResponse.apiName] = requestCaptor
            }
            "getId" -> {
                val requestCaptor = slot<GetIdRequest>()

                coEvery { mockCognitoIdClient.getId(capture(requestCaptor)) } coAnswers {
                    if (mockResponse.responseType == ResponseType.Failure) {
                        throw Json.decodeFromString(
                            CognitoIdentityExceptionSerializer,
                            responseObject.toString()
                        )
                    }
                    GetIdResponse.invoke {
                        this.identityId = (responseObject["identityId"] as JsonPrimitive).content
                    }
                }
                captures[mockResponse.apiName] = requestCaptor
            }
            "getCredentialsForIdentity" -> {
                val requestCaptor = slot<GetCredentialsForIdentityRequest>()

                coEvery { mockCognitoIdClient.getCredentialsForIdentity(capture(requestCaptor)) } coAnswers {
                    if (mockResponse.responseType == ResponseType.Failure) {
                        throw Json.decodeFromString(
                            CognitoIdentityExceptionSerializer,
                            responseObject.toString()
                        )
                    }
                    GetCredentialsForIdentityResponse.invoke {
                        this.credentials = parseCredentials(responseObject["credentials"] as JsonObject)
                    }
                }
                captures[mockResponse.apiName] = requestCaptor
            }
            "deleteUser" -> {
                mockkObject(AuthHelper)
                coEvery { AuthHelper.getSecretHash(any(), any(), any()) } returns "a hash"
                val requestCaptor = slot<DeleteUserRequest>()

                coEvery { mockCognitoIPClient.deleteUser(capture(requestCaptor)) } coAnswers {
                    if (mockResponse.responseType == ResponseType.Failure) {
                        throw Json.decodeFromString(
                            CognitoIdentityProviderExceptionSerializer,
                            responseObject.toString()
                        )
                    }
                    DeleteUserResponse.invoke {}
                }
                captures[mockResponse.apiName] = requestCaptor
            }
            "revokeToken" -> {
                val requestCaptor = slot<RevokeTokenRequest>()
                coEvery { mockCognitoIPClient.revokeToken(capture(requestCaptor)) } coAnswers {
                    if (mockResponse.responseType == ResponseType.Failure) {
                        throw Json.decodeFromString(
                            CognitoIdentityExceptionSerializer,
                            responseObject.toString()
                        )
                    }
                    RevokeTokenResponse.invoke {}
                }
                captures[mockResponse.apiName] = requestCaptor
            }
            "globalSignOut" -> {
                val requestCaptor = slot<GlobalSignOutRequest>()
                coEvery { mockCognitoIPClient.globalSignOut(capture(requestCaptor)) } coAnswers {
                    if (mockResponse.responseType == ResponseType.Failure) {
                        throw Json.decodeFromString(
                            CognitoIdentityExceptionSerializer,
                            responseObject.toString()
                        )
                    }
                    GlobalSignOutResponse.invoke {}
                }
                captures[mockResponse.apiName] = requestCaptor
            }
            else -> throw Error("mock for ${mockResponse.apiName} not defined!")
        }
    }

    private fun parseChallengeParams(params: JsonObject): Map<String, String> {
        return params.mapValues { (k, v) -> (v as JsonPrimitive).content }
    }

    private fun parseAuthenticationResult(result: JsonObject): AuthenticationResultType {
        return AuthenticationResultType.invoke {
            idToken = (result["idToken"] as JsonPrimitive).content
            accessToken = (result["accessToken"] as JsonPrimitive).content
            refreshToken = (result["refreshToken"] as JsonPrimitive).content
            expiresIn = (result["expiresIn"] as JsonPrimitive).content.toInt()
        }
    }

    private fun parseCredentials(result: JsonObject): Credentials {
        return Credentials.invoke {
            accessKeyId = (result["accessKeyId"] as JsonPrimitive).content
            secretKey = (result["secretKey"] as JsonPrimitive).content
            sessionToken = (result["sessionToken"] as JsonPrimitive).content
            expiration = Instant.fromEpochSeconds((result["expiration"] as JsonPrimitive).content)
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

    fun getActualResultFor(apiName: String): Any =
        captures[apiName]?.captured ?: throw Error("Actual result for $apiName is not defined")
}
