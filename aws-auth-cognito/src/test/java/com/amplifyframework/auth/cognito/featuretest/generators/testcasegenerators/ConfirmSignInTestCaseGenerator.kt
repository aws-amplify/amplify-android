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

package com.amplifyframework.auth.cognito.featuretest.generators.testcasegenerators

import aws.sdk.kotlin.services.cognitoidentityprovider.model.CodeMismatchException
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

object ConfirmSignInTestCaseGenerator : SerializableProvider {
    private const val challengeCode = "000000"

    private val mockedRespondToAuthChallengeResponse = MockResponse(
        CognitoType.CognitoIdentityProvider,
        "respondToAuthChallenge",
        ResponseType.Success,
        mapOf(
            "authenticationResult" to mapOf(
                "idToken" to AuthStateJsonGenerator.dummyToken,
                "accessToken" to AuthStateJsonGenerator.dummyToken,
                "refreshToken" to AuthStateJsonGenerator.dummyToken,
                "expiresIn" to 300
            )
        ).toJsonElement()
    )

    private val mockedRespondToAuthCustomChallengeResponse = MockResponse(
        CognitoType.CognitoIdentityProvider,
        "respondToAuthChallenge",
        ResponseType.Success,
        mapOf(
            "session" to "someSession",
            "challengeName" to "CUSTOM_CHALLENGE",
            "challengeParameters" to mapOf(
                "SALT" to "abc",
                "SECRET_BLOCK" to "secretBlock",
                "SRP_B" to "def"
            )
        ).toJsonElement()
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
        mapOf(
            "credentials" to mapOf(
                "accessKeyId" to "someAccessKey",
                "secretKey" to "someSecretKey",
                "sessionToken" to AuthStateJsonGenerator.dummyToken,
                "expiration" to 2342134
            )
        ).toJsonElement()
    )

    private val mockedSignInSuccessExpectation = ExpectationShapes.Amplify(
        apiName = AuthAPI.confirmSignIn,
        responseType = ResponseType.Success,
        response = mapOf(
            "isSignedIn" to true,
            "nextStep" to mapOf(
                "signInStep" to "DONE",
                "additionalInfo" to JsonObject(emptyMap()),
            )
        ).toJsonElement()
    )

    private val mockedConfirmSignInSuccessWithChallengeExpectation = ExpectationShapes.Amplify(
        apiName = AuthAPI.confirmSignIn,
        responseType = ResponseType.Success,
        response = mapOf(
            "isSignedIn" to false,
            "nextStep" to mapOf(
                "signInStep" to "CONFIRM_SIGN_IN_WITH_CUSTOM_CHALLENGE",
                "additionalInfo" to mapOf(
                    "SALT" to "abc",
                    "SECRET_BLOCK" to "secretBlock",
                    "SRP_B" to "def"
                ),
            )
        ).toJsonElement()
    )

    private val baseCase = FeatureTestCase(
        description = "Test that SignIn with SMS challenge invokes proper cognito request and returns success",
        preConditions = PreConditions(
            "authconfiguration.json",
            "SigningIn_SigningIn.json",
            mockedResponses = listOf(
                mockedRespondToAuthChallengeResponse,
                mockedIdentityIdResponse,
                mockedAWSCredentialsResponse,
            )
        ),
        api = API(
            AuthAPI.confirmSignIn,
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
                description = "Test that invalid code on confirm SignIn with SMS challenge errors out",
                preConditions = PreConditions(
                    "authconfiguration.json",
                    "SigningIn_SigningIn.json",
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
                        AuthAPI.confirmSignIn,
                        ResponseType.Failure,
                        CognitoAuthExceptionConverter.lookup(
                            exception,
                            "Confirm Sign in failed."
                        ).toJsonElement()
                    )
                )
            )
        }

    private val successCaseWithSecondaryChallenge = FeatureTestCase(
        description = "Test that confirmsignin secondary challenge processes the custom challenge returned",
        preConditions = PreConditions(
            "authconfiguration.json",
            "SigningIn_SigningIn.json",
            mockedResponses = listOf(
                mockedRespondToAuthCustomChallengeResponse
            )
        ),
        api = API(
            AuthAPI.confirmSignIn,
            params = mapOf(
                "challengeResponse" to challengeCode
            ).toJsonElement(),
            options = JsonObject(emptyMap())
        ),
        validations = listOf(
            mockedConfirmSignInSuccessWithChallengeExpectation,
            ExpectationShapes.State("SigningIn_SigningIn.json")
        )
    )

    override val serializables: List<Any> = listOf(baseCase, errorCase, successCaseWithSecondaryChallenge)
}
