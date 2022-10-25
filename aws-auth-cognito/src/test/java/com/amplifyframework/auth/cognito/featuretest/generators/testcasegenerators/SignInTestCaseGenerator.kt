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

import aws.sdk.kotlin.services.cognitoidentityprovider.model.ChallengeNameType
import com.amplifyframework.auth.cognito.featuretest.API
import com.amplifyframework.auth.cognito.featuretest.AuthAPI
import com.amplifyframework.auth.cognito.featuretest.ExpectationShapes
import com.amplifyframework.auth.cognito.featuretest.FeatureTestCase
import com.amplifyframework.auth.cognito.featuretest.MockResponse
import com.amplifyframework.auth.cognito.featuretest.PreConditions
import com.amplifyframework.auth.cognito.featuretest.ResponseType
import com.amplifyframework.auth.cognito.featuretest.generators.SerializableProvider
import com.amplifyframework.auth.cognito.featuretest.generators.authstategenerators.AuthStateJsonGenerator
import com.amplifyframework.auth.cognito.featuretest.generators.toJsonElement
import kotlinx.serialization.json.JsonObject

object SignInTestCaseGenerator : SerializableProvider {
    private const val userId = "userId"
    private const val username = "username"
    private const val password = "password"
    private const val phone = "+12345678900"

    private val mockedInitiateAuthResponse = MockResponse(
        "cognito",
        "initiateAuth",
        ResponseType.Success,
        mapOf(
            "challengeName" to ChallengeNameType.PasswordVerifier,
            "challengeParameters" to mapOf(
                "SALT" to "abc",
                "SECRET_BLOCK" to "secretBlock",
                "SRP_B" to "def",
                "USERNAME" to username,
                "USER_ID_FOR_SRP" to userId
            )
        ).toJsonElement()
    )

    private val mockedRespondToAuthChallengeResponse = MockResponse(
        "cognito",
        "respondToAuthChallenge",
        ResponseType.Success,
        mapOf(
            "authenticationResult" to mapOf(
                "idToken" to "someToken",
                "accessToken" to AuthStateJsonGenerator.dummyToken,
                "refreshToken" to "someRefreshToken",
                "expiresIn" to 300
            )
        ).toJsonElement()
    )

    private val mockedSMSChallengeResponse = MockResponse(
        "cognito",
        "respondToAuthChallenge",
        ResponseType.Success,
        mapOf(
            "session" to "someSession",
            "challengeName" to "SMS_MFA",
            "challengeParameters" to mapOf(
                "CODE_DELIVERY_DELIVERY_MEDIUM" to "SMS",
                "CODE_DELIVERY_DESTINATION" to phone
            )
        ).toJsonElement()
    )

    private val mockedIdentityIdResponse = MockResponse(
        "cognito",
        "getId",
        ResponseType.Success,
        mapOf("identityId" to "someIdentityId").toJsonElement()
    )

    private val mockedAWSCredentialsResponse = MockResponse(
        "cognito",
        "getCredentialsForIdentity",
        ResponseType.Success,
        mapOf(
            "credentials" to mapOf(
                "accessKeyId" to "someAccessKey",
                "secretKey" to "someSecretKey",
                "sessionToken" to "someSessionToken",
                "expiration" to 2342134
            )
        ).toJsonElement()
    )

    private val mockedSignInSuccessExpectation = ExpectationShapes.Amplify(
        apiName = AuthAPI.signIn,
        responseType = ResponseType.Success,
        response = mapOf(
            "isSignedIn" to true,
            "nextStep" to mapOf(
                "signInStep" to "DONE",
                "additionalInfo" to JsonObject(emptyMap()),
                "codeDeliveryDetails" to null
            )
        ).toJsonElement()
    )

    private val mockedSignInSMSChallengeExpectation = ExpectationShapes.Amplify(
        apiName = AuthAPI.signIn,
        responseType = ResponseType.Success,
        response = mapOf(
            "isSignedIn" to false,
            "nextStep" to mapOf(
                "signInStep" to "CONFIRM_SIGN_IN_WITH_SMS_MFA_CODE",
                "additionalInfo" to JsonObject(emptyMap()),
                "codeDeliveryDetails" to mapOf(
                    "destination" to phone,
                    "deliveryMedium" to "SMS",
                    "attributeName" to null
                )
            )
        ).toJsonElement()
    )

    private val baseCase = FeatureTestCase(
        description = "Test that SRP signIn invokes proper cognito request and returns success",
        preConditions = PreConditions(
            "authconfiguration.json",
            "SignedOut_Configured.json",
            mockedResponses = listOf(
                mockedInitiateAuthResponse,
                mockedRespondToAuthChallengeResponse,
                mockedIdentityIdResponse,
                mockedAWSCredentialsResponse,
            )
        ),
        api = API(
            AuthAPI.signIn,
            params = mapOf(
                "username" to username,
                "password" to password
            ).toJsonElement(),
            options = JsonObject(emptyMap())
        ),
        validations = listOf(
//            ExpectationShapes.Cognito(
//                apiName = "signIn",
//                // see [https://docs.aws.amazon.com/cognito-user-identity-pools/latest/APIReference/API_InitiateAuth.html]
//                // see [https://docs.aws.amazon.com/cognito-user-identity-pools/latest/APIReference/API_RespondToAuthChallenge.html]
//                request = mapOf(
//                    "clientId" to "testAppClientId", // This should be pulled from configuration
//                    "authFlow" to AuthFlowType.UserSrpAuth,
//                    "authParameters" to mapOf("username" to username, "SRP_A" to "123")
//                ).toJsonElement()
//            ),
            mockedSignInSuccessExpectation,
            ExpectationShapes.State("SignedIn_SessionEstablished.json")
        )
    )

    private val challengeCase = baseCase.copy(
        description = "Test that SRP signIn invokes proper cognito request and returns SMS challenge",
        PreConditions(
            "authconfiguration.json",
            "SignedOut_Configured.json",
            mockedResponses = listOf(
                mockedInitiateAuthResponse,
                mockedSMSChallengeResponse,
            )
        ),
        validations = listOf(
            mockedSignInSMSChallengeExpectation,
            ExpectationShapes.State("SigningIn_SigningIn.json")
        )
    )

    override val serializables: List<Any> = listOf(baseCase, challengeCase)
}
