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
import aws.sdk.kotlin.services.cognitoidentity.model.GetCredentialsForIdentityResponse
import aws.sdk.kotlin.services.cognitoidentity.model.GetIdResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.CognitoIdentityProviderClient
import aws.sdk.kotlin.services.cognitoidentityprovider.model.AttributeType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.AuthenticationResultType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ChallengeNameType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.CodeDeliveryDetailsType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ConfirmDeviceResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.DeleteUserResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.DeliveryMediumType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.DeviceType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ForgetDeviceResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ForgotPasswordResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.GetUserResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.GlobalSignOutResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.InitiateAuthResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ListDevicesResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.RespondToAuthChallengeResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.RevokeTokenResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.SignUpResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.UpdateDeviceStatusResponse
import aws.smithy.kotlin.runtime.time.Instant
import com.amplifyframework.auth.cognito.featuretest.CognitoType
import com.amplifyframework.auth.cognito.featuretest.MockResponse
import com.amplifyframework.auth.cognito.featuretest.ResponseType
import com.amplifyframework.auth.cognito.featuretest.serializers.CognitoIdentityExceptionSerializer
import com.amplifyframework.auth.cognito.featuretest.serializers.CognitoIdentityProviderExceptionSerializer
import io.mockk.coEvery
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean

/**
 * Factory to mock aws sdk's cognito API calls and responses.
 */
class CognitoMockFactory(
    private val mockCognitoIPClient: CognitoIdentityProviderClient,
    private val mockCognitoIdClient: CognitoIdentityClient
) {
    fun mock(mockResponse: MockResponse) {
        val responseObject = mockResponse.response as JsonObject

        when (mockResponse.apiName) {
            "forgotPassword" -> {
                coEvery { mockCognitoIPClient.forgotPassword(any()) } coAnswers {
                    setupError(mockResponse, responseObject)
                    ForgotPasswordResponse.invoke {
                        this.codeDeliveryDetails = parseCodeDeliveryDetails(responseObject)
                    }
                }
            }
            "signUp" -> {
                coEvery { mockCognitoIPClient.signUp(any()) } coAnswers {
                    setupError(mockResponse, responseObject)
                    SignUpResponse.invoke {
                        this.codeDeliveryDetails = parseCodeDeliveryDetails(responseObject)
                        this.userConfirmed = if (responseObject.containsKey("userConfirmed")) {
                            (responseObject["userConfirmed"] as? JsonPrimitive)?.boolean ?: false
                        } else false
                        this.userSub = ""
                    }
                }
            }
            "initiateAuth" -> {
                coEvery { mockCognitoIPClient.initiateAuth(any()) } coAnswers {
                    setupError(mockResponse, responseObject)
                    InitiateAuthResponse.invoke {
                        this.challengeName = responseObject["challengeName"]?.let {
                            ChallengeNameType.fromValue((it as JsonPrimitive).content)
                        }

                        this.challengeParameters = responseObject["challengeParameters"]?.let {
                            parseChallengeParams(it as JsonObject)
                        }
                    }
                }
            }
            "respondToAuthChallenge" -> {
                coEvery { mockCognitoIPClient.respondToAuthChallenge(any()) } coAnswers {
                    setupError(mockResponse, responseObject)
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
            }
            "confirmDevice" -> {
                coEvery { mockCognitoIPClient.confirmDevice(any()) } coAnswers {
                    setupError(mockResponse, responseObject)
                    ConfirmDeviceResponse.invoke {}
                }
            }
            "getId" -> {
                coEvery { mockCognitoIdClient.getId(any()) } coAnswers {
                    setupError(mockResponse, responseObject)
                    GetIdResponse.invoke {
                        this.identityId = (responseObject["identityId"] as JsonPrimitive).content
                    }
                }
            }
            "getUser" -> {
                coEvery { mockCognitoIPClient.getUser(any()) } coAnswers {
                    setupError(mockResponse, responseObject)
                    GetUserResponse.invoke {
                        userAttributes = listOf<AttributeType>(
                            AttributeType.invoke {
                                name = "email"
                                value = "email@email.com"
                            },
                            AttributeType.invoke {
                                name = "phone_number"
                                value = "000-000-0000"
                            }
                        )
                        username = ""
                    }
                }
            }
            "getCredentialsForIdentity" -> {
                coEvery { mockCognitoIdClient.getCredentialsForIdentity(any()) } coAnswers {
                    setupError(mockResponse, responseObject)
                    GetCredentialsForIdentityResponse.invoke {
                        this.credentials = parseCredentials(responseObject["credentials"] as JsonObject)
                    }
                }
            }
            "deleteUser" -> {
                coEvery { mockCognitoIPClient.deleteUser(any()) } coAnswers {
                    setupError(mockResponse, responseObject)
                    DeleteUserResponse.invoke {}
                }
            }
            "revokeToken" -> {
                coEvery { mockCognitoIPClient.revokeToken(any()) } coAnswers {
                    setupError(mockResponse, responseObject)
                    RevokeTokenResponse.invoke {}
                }
            }
            "globalSignOut" -> {
                coEvery { mockCognitoIPClient.globalSignOut(any()) } coAnswers {
                    setupError(mockResponse, responseObject)
                    GlobalSignOutResponse.invoke {}
                }
            }
            "updateDeviceStatus" -> {
                coEvery { mockCognitoIPClient.updateDeviceStatus(any()) } coAnswers {
                    setupError(mockResponse, responseObject)
                    UpdateDeviceStatusResponse.invoke { }
                }
            }
            "forgetDevice" -> {
                coEvery { mockCognitoIPClient.forgetDevice(any()) } coAnswers {
                    setupError(mockResponse, responseObject)
                    ForgetDeviceResponse.invoke {}
                }
            }
            "listDevices" -> {
                coEvery { mockCognitoIPClient.listDevices(any()) } coAnswers {
                    setupError(mockResponse, responseObject)
                    ListDevicesResponse.invoke {
                        devices = listOf<DeviceType>(
                            DeviceType.invoke {
                                deviceAttributes = listOf<AttributeType>(
                                    AttributeType.invoke {
                                        name = "name"
                                        value = "value"
                                    }
                                )
                                deviceKey = "deviceKey"
                                deviceCreateDate = Instant.now()
                                deviceLastAuthenticatedDate = Instant.now()
                                deviceLastModifiedDate = Instant.now()
                            }
                        )
                    }
                }
            }
            else -> throw Error("mock for ${mockResponse.apiName} not defined!")
        }
    }

    private fun setupError(
        mockResponse: MockResponse,
        responseObject: JsonObject
    ) {
        if (mockResponse.responseType == ResponseType.Failure) {
            val response = Json.decodeFromString(
                when (mockResponse.type) {
                    CognitoType.CognitoIdentity -> CognitoIdentityExceptionSerializer
                    CognitoType.CognitoIdentityProvider -> CognitoIdentityProviderExceptionSerializer
                },
                responseObject.toString()
            )
            throw response
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
}
