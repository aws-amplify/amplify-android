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
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ResourceNotFoundException
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
import com.amplifyframework.auth.cognito.options.AuthFlowType
import kotlinx.serialization.json.JsonObject

object SignInTestCaseGenerator : SerializableProvider {
    private const val userId = "userId"
    private const val username = "username"
    private const val password = "password"
    private const val phone = "+12345678900"

    private val mockedInitiateAuthResponse = MockResponse(
        CognitoType.CognitoIdentityProvider,
        "initiateAuth",
        ResponseType.Success,
        mapOf(
            "challengeName" to ChallengeNameType.PasswordVerifier.toString(),
            "challengeParameters" to mapOf(
                "SALT" to "abc",
                "SECRET_BLOCK" to "secretBlock",
                "SRP_B" to "def",
                "USERNAME" to username,
                "USER_ID_FOR_SRP" to userId
            )
        ).toJsonElement()
    )

    private val mockedInitiateAuthForCustomAuthWithoutSRPResponse = MockResponse(
        CognitoType.CognitoIdentityProvider,
        "initiateAuth",
        ResponseType.Success,
        mapOf(
            "challengeName" to ChallengeNameType.CustomChallenge.toString(),
            "challengeParameters" to mapOf(
                "SALT" to "abc",
                "SECRET_BLOCK" to "secretBlock",
                "SRP_B" to "def",
                "USERNAME" to username,
            )
        ).toJsonElement()
    )

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

    private val mockedRespondToAuthChallengeResponseWhenResourceNotFoundException = MockResponse(
        CognitoType.CognitoIdentityProvider,
        "respondToAuthChallenge",
        ResponseType.Failure,
        ResourceNotFoundException.invoke {}.toJsonElement()
    )

    private val mockedRespondToAuthChallengeWithDeviceMetadataResponse = MockResponse(
        CognitoType.CognitoIdentityProvider,
        "respondToAuthChallenge",
        ResponseType.Success,
        mapOf(
            "authenticationResult" to mapOf(
                "idToken" to AuthStateJsonGenerator.dummyToken,
                "accessToken" to AuthStateJsonGenerator.dummyToken,
                "refreshToken" to AuthStateJsonGenerator.dummyToken,
                "expiresIn" to 300,
                "newDeviceMetadata" to mapOf(
                    "deviceKey" to "someDeviceKey",
                    "deviceGroupKey" to "someDeviceGroupKey"
                )
            )
        ).toJsonElement()
    )

    private val mockedSMSChallengeResponse = MockResponse(
        CognitoType.CognitoIdentityProvider,
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
                "SRP_B" to "def",
                "USERNAME" to username
            )
        ).toJsonElement()
    )

    private val mockedRespondToAuthCustomChallengeResponseWithAlias = MockResponse(
        CognitoType.CognitoIdentityProvider,
        "respondToAuthChallenge",
        ResponseType.Success,
        mapOf(
            "session" to "someSession",
            "challengeName" to "CUSTOM_CHALLENGE",
            "challengeParameters" to mapOf(
                "SALT" to "abc",
                "SECRET_BLOCK" to "secretBlock",
                "SRP_B" to "def",
                "USERNAME" to "alternateUsername"
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
        apiName = AuthAPI.signIn,
        responseType = ResponseType.Success,
        response = mapOf(
            "isSignedIn" to true,
            "nextStep" to mapOf(
                "signInStep" to "DONE",
                "additionalInfo" to JsonObject(emptyMap()),
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
                )
            )
        ).toJsonElement()
    )

    private val mockedSignInCustomAuthChallengeExpectation = ExpectationShapes.Amplify(
        apiName = AuthAPI.signIn,
        responseType = ResponseType.Success,
        response = mapOf(
            "isSignedIn" to false,
            "nextStep" to mapOf(
                "signInStep" to "CONFIRM_SIGN_IN_WITH_CUSTOM_CHALLENGE",
                "additionalInfo" to mapOf(
                    "SALT" to "abc",
                    "SECRET_BLOCK" to "secretBlock",
                    "SRP_B" to "def",
                    "USERNAME" to username
                )
            )
        ).toJsonElement()
    )

    private val mockedSignInCustomAuthChallengeExpectationWithAlias = ExpectationShapes.Amplify(
        apiName = AuthAPI.signIn,
        responseType = ResponseType.Success,
        response = mapOf(
            "isSignedIn" to false,
            "nextStep" to mapOf(
                "signInStep" to "CONFIRM_SIGN_IN_WITH_CUSTOM_CHALLENGE",
                "additionalInfo" to mapOf(
                    "SALT" to "abc",
                    "SECRET_BLOCK" to "secretBlock",
                    "SRP_B" to "def",
                    "USERNAME" to "alternateUsername"
                )
            )
        ).toJsonElement()
    )

    private val mockConfirmDeviceResponse = MockResponse(
        CognitoType.CognitoIdentityProvider,
        "confirmDevice",
        ResponseType.Success,
        JsonObject(emptyMap())
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
            mockedSignInSuccessExpectation,
            ExpectationShapes.State("SignedIn_SessionEstablished.json")
        )
    )

    private val signInWhenResourceNotFoundExceptionCase = FeatureTestCase(
        description = "Test that SRP signIn invokes proper cognito request and returns " +
            "ResourceNotFoundException but still signs in successfully",
        preConditions = PreConditions(
            "authconfiguration.json",
            "SignedOut_Configured.json",
            mockedResponses = listOf(
                mockedInitiateAuthResponse,
                mockedRespondToAuthChallengeResponseWhenResourceNotFoundException,
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
            mockedSignInSuccessExpectation,
            ExpectationShapes.State("SignedIn_SessionEstablished.json")
        )
    )

    private val deviceSRPTestCase = FeatureTestCase(
        description = "Test that Device SRP signIn invokes proper cognito request and returns success",
        preConditions = PreConditions(
            "authconfiguration.json",
            "SignedOut_Configured.json",
            mockedResponses = listOf(
                mockedInitiateAuthResponse,
                mockedRespondToAuthChallengeWithDeviceMetadataResponse,
                mockConfirmDeviceResponse,
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
            mockedSignInSuccessExpectation,
            ExpectationShapes.State("SignedIn_SessionEstablished.json")
        )
    )

    private val challengeCase = baseCase.copy(
        description = "Test that SRP signIn invokes proper cognito request and returns SMS challenge",
        preConditions = PreConditions(
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

    private val signInWhenAlreadySigningInAuthCase = FeatureTestCase(
        description = "Test that overriding signIn when already signing in returns success",
        preConditions = PreConditions(
            "authconfiguration.json",
            "SigningIn_SigningIn.json",
            mockedResponses = listOf(
                mockedInitiateAuthResponse,
                mockedSMSChallengeResponse,
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
            mockedSignInSMSChallengeExpectation,
            ExpectationShapes.State("SigningIn_SigningIn.json")
        )
    )

    private val customAuthCase = FeatureTestCase(
        description = "Test that Custom Auth signIn invokes proper cognito request and returns custom challenge",
        preConditions = PreConditions(
            "authconfiguration.json",
            "SignedOut_Configured.json",
            mockedResponses = listOf(
                mockedInitiateAuthForCustomAuthWithoutSRPResponse,
                mockedRespondToAuthCustomChallengeResponse
            )
        ),
        api = API(
            AuthAPI.signIn,
            params = mapOf(
                "username" to username,
                "password" to "",
            ).toJsonElement(),
            options = mapOf(
                "signInOptions" to
                    mapOf("authFlow" to AuthFlowType.CUSTOM_AUTH_WITHOUT_SRP.toString())
            ).toJsonElement()
        ),
        validations = listOf(
            mockedSignInCustomAuthChallengeExpectation,
            ExpectationShapes.State("CustomSignIn_SigningIn.json")
        )
    )

    private val customAuthCaseWhenResourceNotFoundExceptionCase = FeatureTestCase(
        description = "Test that Custom Auth signIn invokes ResourceNotFoundException " +
            "and then receive proper cognito request and returns custom challenge",
        preConditions = PreConditions(
            "authconfiguration.json",
            "SignedOut_Configured.json",
            mockedResponses = listOf(
                mockedInitiateAuthForCustomAuthWithoutSRPResponse,
                mockedRespondToAuthChallengeResponseWhenResourceNotFoundException,
                mockedRespondToAuthCustomChallengeResponse
            )
        ),
        api = API(
            AuthAPI.signIn,
            params = mapOf(
                "username" to username,
                "password" to "",
            ).toJsonElement(),
            options = mapOf(
                "signInOptions" to
                    mapOf("authFlow" to AuthFlowType.CUSTOM_AUTH_WITHOUT_SRP.toString())
            ).toJsonElement()
        ),
        validations = listOf(
            mockedSignInCustomAuthChallengeExpectation,
            ExpectationShapes.State("CustomSignIn_SigningIn.json")
        )
    )

    private val customAuthWithSRPCase = FeatureTestCase(
        description = "Test that Custom Auth signIn invokes proper cognito request and returns password challenge",
        preConditions = PreConditions(
            "authconfiguration.json",
            "SignedOut_Configured.json",
            mockedResponses = listOf(
                mockedInitiateAuthResponse,
                mockedRespondToAuthCustomChallengeResponse
            )
        ),
        api = API(
            AuthAPI.signIn,
            params = mapOf(
                "username" to username,
                "password" to "",
            ).toJsonElement(),
            options = mapOf(
                "signInOptions" to mapOf("authFlow" to AuthFlowType.CUSTOM_AUTH_WITH_SRP.toString())
            ).toJsonElement()
        ),
        validations = listOf(
            mockedSignInCustomAuthChallengeExpectation,
            ExpectationShapes.State("CustomSignIn_SigningIn.json")
        )
    )

    private val customAuthWithSRPCaseWhenAliasIsUsedToSignIn = FeatureTestCase(
        description = "Test that Custom Auth signIn invokes proper cognito request " +
            "and returns password challenge when alias is used",
        preConditions = PreConditions(
            "authconfiguration.json",
            "SignedOut_Configured.json",
            mockedResponses = listOf(
                mockedInitiateAuthResponse,
                mockedRespondToAuthCustomChallengeResponseWithAlias
            )
        ),
        api = API(
            AuthAPI.signIn,
            params = mapOf(
                "username" to username,
                "password" to "",
            ).toJsonElement(),
            options = mapOf(
                "signInOptions" to mapOf("authFlow" to AuthFlowType.CUSTOM_AUTH_WITH_SRP.toString())
            ).toJsonElement()
        ),
        validations = listOf(
            mockedSignInCustomAuthChallengeExpectationWithAlias,
            ExpectationShapes.State("CustomSignIn_SigningIn.json")
        )
    )

    private val customAuthWithSRPWhenResourceNotFoundExceptionCase = FeatureTestCase(
        description = "Test that Custom Auth signIn invokes ResourceNotFoundException" +
            " and then received proper cognito request and returns password challenge",
        preConditions = PreConditions(
            "authconfiguration.json",
            "SignedOut_Configured.json",
            mockedResponses = listOf(
                mockedInitiateAuthResponse,
                mockedRespondToAuthChallengeResponseWhenResourceNotFoundException,
                mockedRespondToAuthCustomChallengeResponse
            )
        ),
        api = API(
            AuthAPI.signIn,
            params = mapOf(
                "username" to username,
                "password" to "",
            ).toJsonElement(),
            options = mapOf(
                "signInOptions" to mapOf("authFlow" to AuthFlowType.CUSTOM_AUTH_WITH_SRP.toString())
            ).toJsonElement()
        ),
        validations = listOf(
            mockedSignInCustomAuthChallengeExpectation,
            ExpectationShapes.State("CustomSignIn_SigningIn.json")
        )
    )

    override val serializables: List<Any> = listOf(
        baseCase,
        challengeCase,
        deviceSRPTestCase,
        customAuthCase,
        customAuthWithSRPCase,
        signInWhenAlreadySigningInAuthCase,
        customAuthWithSRPWhenResourceNotFoundExceptionCase,
        customAuthCaseWhenResourceNotFoundExceptionCase,
        signInWhenResourceNotFoundExceptionCase,
        customAuthWithSRPCaseWhenAliasIsUsedToSignIn
    )
}
