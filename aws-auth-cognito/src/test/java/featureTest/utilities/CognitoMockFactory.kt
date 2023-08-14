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
import aws.sdk.kotlin.services.cognitoidentity.model.NotAuthorizedException
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
import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.cognito.featuretest.AuthAPI
import com.amplifyframework.auth.cognito.featuretest.CognitoType
import com.amplifyframework.auth.cognito.featuretest.MockResponse
import com.amplifyframework.auth.cognito.featuretest.ResponseType
import com.amplifyframework.auth.exceptions.InvalidStateException
import com.amplifyframework.auth.exceptions.ValidationException
import com.amplifyframework.statemachine.codegen.data.UserPoolConfiguration
import generated.model.MockedResponse
import generated.model.UnitTest
import generated.model.TypeResponse
import io.mockk.InternalPlatformDsl.toStr
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import org.json.JSONObject

/**
 * Factory to mock aws sdk's cognito API calls and responses.
 */
class CognitoMockFactory(
    private val mockCognitoIPClient: CognitoIdentityProviderClient,
    private val mockCognitoIdClient: CognitoIdentityClient,
    private val test: generated.model.UnitTest
) {
    fun mock() {
        val responseObject = test.preConditions!!.mockedResponses!!

        for (resp in responseObject) {
            when (resp.apiName) {
                "forgotPassword" -> {
                    coEvery { mockCognitoIPClient.forgotPassword(any()) } coAnswers {
                        setupError(resp)
                        ForgotPasswordResponse.invoke {
                            this.codeDeliveryDetails = parseCodeDeliveryDetails(resp)
                        }
                    }

                }
                "signUp" -> {
                    coEvery { mockCognitoIPClient.signUp(any()) } coAnswers {
                        setupError(resp)
                        SignUpResponse.invoke {
                            this.codeDeliveryDetails = parseCodeDeliveryDetails(resp)
                            this.userConfirmed = false
                            val userSubHelp = JSONObject(resp!!.response!!.asSuccess().asString())
                            val userSub = userSubHelp["userId"].toStr()
                            this.userSub = userSub
                        }
                    }
                }
                "initiateAuth" -> {
                    coEvery { mockCognitoIPClient.initiateAuth(any()) } coAnswers {
                        setupError(resp)
                        InitiateAuthResponse.invoke {
                            this.challengeName = JSONObject(resp.response!!.asSuccess().asString())["challengeName"]?.let {
                                ChallengeNameType.fromValue(it.toStr())
                            }

                            this.challengeParameters = JSONObject(resp.response!!.asSuccess().asString())?.let {
                                parseChallengeParams(it)
                            }

                        }
                    }
                }
                "respondToAuthChallenge" -> {
                    coEvery { mockCognitoIPClient.respondToAuthChallenge(any()) } coAnswers {
                        setupError(resp)
                        RespondToAuthChallengeResponse.invoke {
                            this.authenticationResult = JSONObject(resp.response!!.asSuccess().asString())["authenticationResult"]?.let {
                                parseAuthenticationResult(JSONObject(it.toStr()))
                            }
                            this.session = JSONObject(resp!!.response!!.asSuccess().asString())["session"]?.let { (it as String) }
                            this.challengeName = JSONObject(resp!!.response!!.asSuccess().asString())["challengeName"]?.let {
                                ChallengeNameType.fromValue(it as String)
                            }
                            this.challengeParameters = JSONObject(resp!!.response!!.asSuccess().asString())["challengeParameters"]?.let {
                                parseChallengeParams(JSONObject(it.toStr()))
                            }
                        }
                    }
                }
                "confirmDevice" -> {
                    coEvery { mockCognitoIPClient.confirmDevice(any()) } coAnswers {
                        setupError(resp)
                        ConfirmDeviceResponse.invoke {}
                    }
                }
                "getId" -> {
                    coEvery { mockCognitoIdClient.getId(any()) } coAnswers {
                        setupError(resp)
                        GetIdResponse.invoke {
                            this.identityId = (JSONObject(resp!!.response!!.asSuccess().asString())["identityId"] as String)
                        }
                    }
                }
                "getUser" -> {
                    coEvery { mockCognitoIPClient.getUser(any()) } coAnswers {
                        setupError(resp)
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
                        }
                    }
                }
                "getCredentialsForIdentity" -> {
                    coEvery { mockCognitoIdClient.getCredentialsForIdentity(any()) } coAnswers {
                        setupError(resp)
                        GetCredentialsForIdentityResponse.invoke {
                            this.credentials = parseCredentials(JSONObject(JSONObject(resp!!.response!!.asSuccess().asString())["credentials"].toStr()))
                        }
                    }
                }
                "deleteUser" -> {
                    coEvery { mockCognitoIPClient.deleteUser(any()) } coAnswers {
                        setupError(resp)
                        DeleteUserResponse.invoke {}
                    }
                }
                "revokeToken" -> {
                    coEvery { mockCognitoIPClient.revokeToken(any()) } coAnswers {
                        setupError(resp)
                        RevokeTokenResponse.invoke {}
                    }
                }
                "globalSignOut" -> {
                    coEvery { mockCognitoIPClient.globalSignOut(any()) } coAnswers {
                        setupError(resp)
                        GlobalSignOutResponse.invoke {}
                    }
                }
                "updateDeviceStatus" -> {
                    coEvery { mockCognitoIPClient.updateDeviceStatus(any()) } coAnswers {
                        setupError(resp)
                        UpdateDeviceStatusResponse.invoke { }
                    }
                }
                "forgetDevice" -> {
                    coEvery { mockCognitoIPClient.forgetDevice(any()) } coAnswers {
                        setupError(resp)
                        ForgetDeviceResponse.invoke {}
                    }
                }
                "listDevices" -> {
                    coEvery { mockCognitoIPClient.listDevices(any()) } coAnswers {
                        setupError(resp)
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




                else -> println("no mock for this")//throw Error("mock for ${test.api!!.name} not defined!")

            }
        }

    }
    private fun setupError(response: MockedResponse) {
        if (response!!.responseType == TypeResponse.Error) {


            val currentError = response!!.response!!.asError()

            when (currentError.errorType) {
                "NotAuthorizedException" -> throw NotAuthorizedException(mockk())
                "InvalidStateException" -> throw InvalidStateException()
                else -> throw ValidationException(currentError.errorType!!, currentError.errorMessage!!)

            }
        }
    }



    private fun parseChallengeParams(params: JSONObject): Map<String, String> {
        val keys = params.keys()
        val curr : MutableMap<String, String> = mutableMapOf()
        for (key in keys) {
            curr[key.toStr()] = params[key.toStr()].toStr()
        }
        return curr
    }

    private fun parseAuthenticationResult(result: JSONObject): AuthenticationResultType {
        return AuthenticationResultType.invoke {
            idToken = (result["idToken"].toStr())
            accessToken = (result["accessToken"].toStr())
            refreshToken = (result["refreshToken"].toStr())
            expiresIn = (result["expiresIn"].toStr().toInt())
        }
    }

    private fun parseCredentials(result: JSONObject): Credentials {
        return Credentials.invoke {
            accessKeyId = (result["accessKeyId"].toStr())
            secretKey = (result["secretKey"].toStr())
            sessionToken = (result["sessionToken"].toStr())
            expiration = Instant.fromEpochSeconds((result["expiration"].toStr()))
        }
    }

    private fun parseCodeDeliveryDetails(response: MockedResponse): CodeDeliveryDetailsType {
        val codeDeliveryDetails = JSONObject(JSONObject(response.response!!.asSuccess().asString())["codeDeliveryDetails"].toStr())

        return CodeDeliveryDetailsType.invoke {
            destination = (codeDeliveryDetails["destination"].toStr())
            deliveryMedium =
                DeliveryMediumType.fromValue((codeDeliveryDetails["deliveryMedium"].toStr()))
            attributeName = (codeDeliveryDetails["attributeName"].toStr())

        }
    }
}
