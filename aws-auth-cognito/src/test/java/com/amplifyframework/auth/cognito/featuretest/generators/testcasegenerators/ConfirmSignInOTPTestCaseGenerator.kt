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

import aws.sdk.kotlin.services.cognitoidentity.model.Credentials
import aws.sdk.kotlin.services.cognitoidentity.model.GetCredentialsForIdentityResponse
import aws.sdk.kotlin.services.cognitoidentityprovider.model.AuthenticationResultType
import aws.sdk.kotlin.services.cognitoidentityprovider.model.CodeMismatchException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.RespondToAuthChallengeResponse
import aws.smithy.kotlin.runtime.time.Instant
import com.amplifyframework.auth.cognito.CognitoAuthExceptionConverter
import com.amplifyframework.auth.cognito.featuretest.API
import com.amplifyframework.auth.cognito.featuretest.AuthAPI
import com.amplifyframework.auth.cognito.featuretest.CognitoType
import com.amplifyframework.auth.cognito.featuretest.ExpectationShapes
import com.amplifyframework.auth.cognito.featuretest.FeatureTestCase
import com.amplifyframework.auth.cognito.featuretest.MockResponse
import com.amplifyframework.auth.cognito.featuretest.PreConditions
import com.amplifyframework.auth.cognito.featuretest.ResponseType
import com.amplifyframework.auth.cognito.featuretest.generators.SerializableProvider
import com.amplifyframework.auth.cognito.featuretest.generators.authstategenerators.AuthStateJsonGenerator
import com.amplifyframework.auth.cognito.featuretest.generators.toJsonElement
import kotlinx.serialization.json.JsonObject

object ConfirmSignInOTPTestCaseGenerator : SerializableProvider {
    private const val challengeCode = "000000"

    private val mockedRespondToAuthChallengeResponse = MockResponse(
        CognitoType.CognitoIdentityProvider,
        "respondToAuthChallenge",
        ResponseType.Success,
        RespondToAuthChallengeResponse.invoke {
            authenticationResult = AuthenticationResultType.invoke {
                idToken = AuthStateJsonGenerator.dummyToken
                accessToken = AuthStateJsonGenerator.dummyToken
                refreshToken = AuthStateJsonGenerator.dummyToken
                expiresIn = 300
            }
        }.toJsonElement()
    )

    private val mockedIdentityIdResponse = MockResponse(
        CognitoType.CognitoIdentity,
        "getId",
        ResponseType.Success,
        mapOf("identityId" to "someIdentityId").toJsonElement()
    )

    private val mockedAWSCredentialsResponse = MockResponse(
        CognitoType.CognitoIdentity,
        "getCredentialsForIdentity",
        ResponseType.Success,
        GetCredentialsForIdentityResponse.invoke {
            credentials = Credentials.invoke {
                accessKeyId = "someAccessKey"
                secretKey = "someAccessKey"
                sessionToken = AuthStateJsonGenerator.dummyToken
                expiration = Instant.MAX_VALUE
            }
        }.toJsonElement()

    )

    private val mockedSignInSuccessExpectation = ExpectationShapes.Amplify(
        apiName = AuthAPI.confirmSignInWithOTP,
        responseType = ResponseType.Success,
        response = mapOf(
            "isSignedIn" to true,
            "nextStep" to mapOf(
                "signInStep" to "DONE",
                "additionalInfo" to JsonObject(emptyMap()),
            )
        ).toJsonElement()
    )

    private val baseCase = FeatureTestCase(
        description = "Test that confirm signIn with OTP invokes proper cognito request and returns success",
        preConditions = PreConditions(
            "authconfiguration.json",
            "PasswordlessSignIn_SigningIn.json",
            mockedResponses = listOf(
                mockedRespondToAuthChallengeResponse,
                mockedIdentityIdResponse,
                mockedAWSCredentialsResponse,
            )
        ),
        api = API(
            AuthAPI.confirmSignInWithOTP,
            params = mapOf(
                "challengeResponse" to challengeCode
            ).toJsonElement(),
            options = JsonObject(emptyMap())
        ),
        validations = listOf(
            mockedSignInSuccessExpectation,
            ExpectationShapes.State("SignedIn_SessionEstablished.json")
        )
    )

    private val errorCase: FeatureTestCase
        get() {
            val exception = CodeMismatchException.invoke {
                message = "Confirmation code entered is not correct."
            }
            return baseCase.copy(
                description = "Test that invalid code on confirm SignIn with OTP errors out",
                preConditions = PreConditions(
                    "authconfiguration.json",
                    "PasswordlessSignIn_SigningIn.json",
                    mockedResponses = listOf(
                        MockResponse(
                            CognitoType.CognitoIdentityProvider,
                            "respondToAuthChallenge",
                            ResponseType.Failure,
                            exception.toJsonElement()
                        )
                    )
                ),
                validations = listOf(
                    ExpectationShapes.Amplify(
                        AuthAPI.confirmSignInWithOTP,
                        ResponseType.Failure,
                        CognitoAuthExceptionConverter.lookup(
                            exception,
                            "Confirm Sign with OTP  in failed."
                        ).toJsonElement()
                    )
                )
            )
        }

    override val serializables: List<Any> = listOf(baseCase, errorCase)
}
