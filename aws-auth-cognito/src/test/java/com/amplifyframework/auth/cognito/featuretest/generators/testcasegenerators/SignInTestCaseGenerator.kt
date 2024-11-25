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
import aws.sdk.kotlin.services.cognitoidentityprovider.model.NotAuthorizedException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.ResourceNotFoundException
import com.amplifyframework.auth.AuthFactorType
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
import com.amplifyframework.auth.exceptions.InvalidStateException
import kotlinx.serialization.json.JsonObject

object SignInTestCaseGenerator : SerializableProvider {
    private const val userId = "userId"
    private const val username = "username"
    private const val password = "password"
    private const val phone = "+12345678900"
    private const val email = "test@****.com"
    private const val session = "someSession"

    private val mockedInitiateAuthResponse = MockResponse(
        CognitoType.CognitoIdentityProvider,
        "initiateAuth",
        ResponseType.Success,
        mapOf(
            "challengeName" to ChallengeNameType.PasswordVerifier.value,
            "challengeParameters" to mapOf(
                "SALT" to "abc",
                "SECRET_BLOCK" to "secretBlock",
                "SRP_B" to "def",
                "USERNAME" to username,
                "USER_ID_FOR_SRP" to userId
            )
        ).toJsonElement()
    )

    private val mockedInitiateAuthPasswordResponse = MockResponse(
        CognitoType.CognitoIdentityProvider,
        "initiateAuth",
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

    private val mockedInitiateAuthSelectChallengeResponse = MockResponse(
        CognitoType.CognitoIdentityProvider,
        "initiateAuth",
        ResponseType.Success,
        mapOf(
            "challengeName" to ChallengeNameType.SelectChallenge.value,
            "session" to session,
            "parameters" to JsonObject(emptyMap()),
            "availableChallenges" to listOf(
                ChallengeNameType.Password.value,
                ChallengeNameType.WebAuthn.value,
                ChallengeNameType.EmailOtp.value
            )
        ).toJsonElement()
    )

    private fun mockedInitiateAuthEmailOrSmsResponse(challengeNameType: ChallengeNameType): MockResponse {
        val (medium, destination) = if (challengeNameType == ChallengeNameType.EmailOtp) {
            Pair("EMAIL", email)
        } else {
            Pair("SMS", phone)
        }

        return MockResponse(
            CognitoType.CognitoIdentityProvider,
            "initiateAuth",
            ResponseType.Success,
            mapOf(
                "challengeName" to challengeNameType.value,
                "session" to session,
                "parameters" to JsonObject(emptyMap()),
                "challengeParameters" to mapOf(
                    "CODE_DELIVERY_DELIVERY_MEDIUM" to medium,
                    "CODE_DELIVERY_DESTINATION" to destination
                )
            ).toJsonElement()
        )
    }

    private val mockedInitiateAuthForCustomAuthWithoutSRPResponse = MockResponse(
        CognitoType.CognitoIdentityProvider,
        "initiateAuth",
        ResponseType.Success,
        mapOf(
            "challengeName" to ChallengeNameType.CustomChallenge.value,
            "challengeParameters" to mapOf(
                "SALT" to "abc",
                "SECRET_BLOCK" to "secretBlock",
                "SRP_B" to "def",
                "USERNAME" to username
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

    private val mockedInitAuthNotAuthorizedException = MockResponse(
        CognitoType.CognitoIdentityProvider,
        "initiateAuth",
        ResponseType.Failure,
        NotAuthorizedException.invoke {
            message = "Incorrect username or password."
        }.toJsonElement()
    )

    private val notAuthorizedExceptionExpectation = ExpectationShapes.Amplify(
        AuthAPI.signIn,
        ResponseType.Failure,
        com.amplifyframework.auth.exceptions.NotAuthorizedException(
            cause = NotAuthorizedException.invoke {
                message = "Incorrect username or password."
            }
        ).toJsonElement()
    )

    private val mockedInvalidStateException = ExpectationShapes.Amplify(
        AuthAPI.autoSignIn,
        ResponseType.Failure,
        InvalidStateException().toJsonElement()
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
                "additionalInfo" to JsonObject(emptyMap())
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
                    "deliveryMedium" to "SMS"
                )
            )
        ).toJsonElement()
    )

    private val mockedSignInSelectChallengeExpectation = ExpectationShapes.Amplify(
        apiName = AuthAPI.signIn,
        responseType = ResponseType.Success,
        response = mapOf(
            "isSignedIn" to false,
            "nextStep" to mapOf(
                "signInStep" to "CONTINUE_SIGN_IN_WITH_FIRST_FACTOR_SELECTION",
                "additionalInfo" to JsonObject(emptyMap()),
                "availableFactors" to listOf(
                    AuthFactorType.PASSWORD.challengeResponse,
                    AuthFactorType.WEB_AUTHN.challengeResponse,
                    AuthFactorType.EMAIL_OTP.challengeResponse
                )
            )
        ).toJsonElement()
    )

    private fun mockedConfirmSignInWithOtpExpectation(challengeNameType: ChallengeNameType): ExpectationShapes.Amplify {
        val (medium, destination) = if (challengeNameType == ChallengeNameType.EmailOtp) {
            Pair("EMAIL", email)
        } else {
            Pair("SMS", phone)
        }
        return ExpectationShapes.Amplify(
            apiName = AuthAPI.signIn,
            responseType = ResponseType.Success,
            response = mapOf(
                "isSignedIn" to false,
                "nextStep" to mapOf(
                    "signInStep" to "CONFIRM_SIGN_IN_WITH_OTP",
                    "additionalInfo" to JsonObject(emptyMap()),
                    "codeDeliveryDetails" to mapOf(
                        "destination" to destination,
                        "deliveryMedium" to medium
                    )
                )
            ).toJsonElement()
        )
    }

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

    private val expectedCognitoAutoSignInRequest = ExpectationShapes.Cognito.CognitoIdentityProvider(
        apiName = "initiateAuth",
        request = mapOf(
            "authFlow" to aws.sdk.kotlin.services.cognitoidentityprovider.model.AuthFlowType.UserAuth,
            "clientId" to "testAppClientId", // This should be pulled from configuration
            "authParameters" to mapOf(
                "USERNAME" to AuthStateJsonGenerator.username, // pulled from loaded SignedUp state
                "SECRET_HASH" to "a hash"
            ),
            "clientMetadata" to emptyMap<String, String>(),
            "session" to AuthStateJsonGenerator.session // pulled from loaded SignedUp state
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
                mockedAWSCredentialsResponse
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

    // Init USER_AUTH with no preference
    private val signInWithUserAuthWithNoPreferenceReturnsSelectChallenge = FeatureTestCase(
        description = "Test that USER_AUTH signIn with no preference returns Select Challenge",
        preConditions = PreConditions(
            "authconfiguration_userauth.json",
            "SignedOut_Configured.json",
            mockedResponses = listOf(
                mockedInitiateAuthSelectChallengeResponse
            )
        ),
        api = API(
            AuthAPI.signIn,
            params = mapOf(
                "username" to username,
                "password" to ""
            ).toJsonElement(),
            options = mapOf(
                "signInOptions" to
                    mapOf(
                        "authFlow" to AuthFlowType.USER_AUTH.toString(),
                        "preferredFirstFactor" to null
                    )
            ).toJsonElement()

        ),
        validations = listOf(
            mockedSignInSelectChallengeExpectation,
            ExpectationShapes.State("SigningIn_SelectChallenge.json")
        )
    )

    // Init USER_AUTH with a preference not supported for the user
    private val signInWithUserAuthWithUnsupportedPreferenceReturnsSelectChallenge = FeatureTestCase(
        description = "Test that USER_AUTH signIn with an unsupported preference returns Select Challenge",
        preConditions = PreConditions(
            "authconfiguration_userauth.json",
            "SignedOut_Configured.json",
            mockedResponses = listOf(
                mockedInitiateAuthSelectChallengeResponse
            )
        ),
        api = API(
            AuthAPI.signIn,
            params = mapOf(
                "username" to username,
                "password" to ""
            ).toJsonElement(),
            options = mapOf(
                "signInOptions" to
                    mapOf(
                        "authFlow" to AuthFlowType.USER_AUTH.toString(),
                        "preferredFirstFactor" to AuthFactorType.SMS_OTP.challengeResponse
                    )
            ).toJsonElement()

        ),
        validations = listOf(
            mockedSignInSelectChallengeExpectation,
            ExpectationShapes.State("SigningIn_SelectChallenge.json")
        )
    )

    // Init USER_AUTH with EMAIL_OTP preference
    private val signInWithUserAuthWithEmailOtpPreferenceReturnsVerifyChallenge = FeatureTestCase(
        description = "Test that USER_AUTH signIn with EMAIL preference returns Confirm Sign In With OTP",
        preConditions = PreConditions(
            "authconfiguration_userauth.json",
            "SignedOut_Configured.json",
            mockedResponses = listOf(
                mockedInitiateAuthEmailOrSmsResponse(ChallengeNameType.EmailOtp)
            )
        ),
        api = API(
            AuthAPI.signIn,
            params = mapOf(
                "username" to username,
                "password" to ""
            ).toJsonElement(),
            options = mapOf(
                "signInOptions" to
                    mapOf(
                        "authFlow" to AuthFlowType.USER_AUTH.toString(),
                        "preferredFirstFactor" to AuthFactorType.EMAIL_OTP.challengeResponse
                    )
            ).toJsonElement()

        ),
        validations = listOf(
            mockedConfirmSignInWithOtpExpectation(ChallengeNameType.EmailOtp),
            ExpectationShapes.State("SigningIn_EmailOtp.json")
        )
    )

    // Init USER_AUTH with SMS_OTP preference
    private val signInWithUserAuthWithSmsOtpPreferenceReturnsVerifyChallenge = FeatureTestCase(
        description = "Test that USER_AUTH signIn with SMS preference returns Confirm Sign In With OTP",
        preConditions = PreConditions(
            "authconfiguration_userauth.json",
            "SignedOut_Configured.json",
            mockedResponses = listOf(
                mockedInitiateAuthEmailOrSmsResponse(ChallengeNameType.SmsOtp)
            )
        ),
        api = API(
            AuthAPI.signIn,
            params = mapOf(
                "username" to username,
                "password" to ""
            ).toJsonElement(),
            options = mapOf(
                "signInOptions" to
                    mapOf(
                        "authFlow" to AuthFlowType.USER_AUTH.toString(),
                        "preferredFirstFactor" to AuthFactorType.SMS_OTP.challengeResponse
                    )
            ).toJsonElement()

        ),
        validations = listOf(
            mockedConfirmSignInWithOtpExpectation(ChallengeNameType.SmsOtp),
            ExpectationShapes.State("SigningIn_SmsOtp.json")
        )
    )

    // Init USER_AUTH with PASSWORD_SRP preference with correct password
    private val signInWithUserAuthWithPasswordSrpPreferenceSucceeds = FeatureTestCase(
        description = "Test that USER_AUTH signIn with PASSWORD_SRP preference succeeds",
        preConditions = PreConditions(
            "authconfiguration_userauth.json",
            "SignedOut_Configured.json",
            mockedResponses = listOf(
                mockedInitiateAuthResponse,
                mockedRespondToAuthChallengeResponse,
                mockedIdentityIdResponse,
                mockedAWSCredentialsResponse
            )
        ),
        api = API(
            AuthAPI.signIn,
            params = mapOf(
                "username" to username,
                "password" to password
            ).toJsonElement(),
            options = mapOf(
                "signInOptions" to
                    mapOf(
                        "authFlow" to AuthFlowType.USER_AUTH.toString(),
                        "preferredFirstFactor" to AuthFactorType.PASSWORD_SRP.challengeResponse
                    )
            ).toJsonElement()

        ),
        validations = listOf(
            mockedSignInSuccessExpectation,
            ExpectationShapes.State("SignedIn_SessionEstablished.json")
        )
    )

    // Init USER_AUTH with PASSWORD_SRP preference with incorrect password
    private val signInWithUserAuthWithPasswordSrpPreferenceFails = FeatureTestCase(
        description = "Test that USER_AUTH signIn with PASSWORD_SRP preference fails",
        preConditions = PreConditions(
            "authconfiguration_userauth.json",
            "SignedOut_Configured.json",
            mockedResponses = listOf(
                mockedInitAuthNotAuthorizedException
            )
        ),
        api = API(
            AuthAPI.signIn,
            params = mapOf(
                "username" to username,
                "password" to password
            ).toJsonElement(),
            options = mapOf(
                "signInOptions" to
                    mapOf(
                        "authFlow" to AuthFlowType.USER_AUTH.toString(),
                        "preferredFirstFactor" to AuthFactorType.PASSWORD_SRP.challengeResponse
                    )
            ).toJsonElement()

        ),
        validations = listOf(
            notAuthorizedExceptionExpectation
        )
    )

    // Init USER_AUTH with PASSWORD preference with correct password
    private val signInWithUserAuthWithPasswordPreferenceSucceeds = FeatureTestCase(
        description = "Test that USER_AUTH signIn with PASSWORD preference succeeds",
        preConditions = PreConditions(
            "authconfiguration_userauth.json",
            "SignedOut_Configured.json",
            mockedResponses = listOf(
                mockedInitiateAuthPasswordResponse
            )
        ),
        api = API(
            AuthAPI.signIn,
            params = mapOf(
                "username" to username,
                "password" to password
            ).toJsonElement(),
            options = mapOf(
                "signInOptions" to
                    mapOf(
                        "authFlow" to AuthFlowType.USER_AUTH.toString(),
                        "preferredFirstFactor" to ChallengeNameType.Password.value
                    )
            ).toJsonElement()

        ),
        validations = listOf(
            mockedSignInSuccessExpectation,
            ExpectationShapes.State("SignedIn_SessionEstablished_User_Auth.json")
        )
    )

    // Init USER_AUTH with PASSWORD preference with incorrect password
    private val signInWithUserAuthWithPasswordPreferenceFails = FeatureTestCase(
        description = "Test that USER_AUTH signIn with PASSWORD preference fails",
        preConditions = PreConditions(
            "authconfiguration_userauth.json",
            "SignedOut_Configured.json",
            mockedResponses = listOf(
                mockedInitAuthNotAuthorizedException
            )
        ),
        api = API(
            AuthAPI.signIn,
            params = mapOf(
                "username" to username,
                "password" to password
            ).toJsonElement(),
            options = mapOf(
                "signInOptions" to
                    mapOf(
                        "authFlow" to AuthFlowType.USER_AUTH.toString(),
                        "preferredFirstFactor" to ChallengeNameType.Password.value
                    )
            ).toJsonElement()

        ),
        validations = listOf(
            notAuthorizedExceptionExpectation
        )
    )

    private val autoSignInSucceeds = FeatureTestCase(
        description = "Test that autoSignIn invokes proper cognito request and returns DONE",
        preConditions = PreConditions(
            "authconfiguration_userauth.json",
            "SignedOut_SessionEstablished_SignedUp.json",
            mockedResponses = listOf(
                mockedInitiateAuthPasswordResponse
            )
        ),
        api = API(
            AuthAPI.autoSignIn,
            params = emptyMap<Any, Any>().toJsonElement(),
            options = JsonObject(emptyMap())
        ),
        validations = listOf(
            expectedCognitoAutoSignInRequest,
            mockedSignInSuccessExpectation
        )
    )

    private val autoSignInWithoutConfirmSignUpFails = FeatureTestCase(
        description = "Test that autoSignIn without ConfirmSignUp fails",
        preConditions = PreConditions(
            "authconfiguration_userauth.json",
            "SignedOut_SessionEstablished_AwaitingUserConfirmation.json",
            mockedResponses = listOf()
        ),
        api = API(
            AuthAPI.autoSignIn,
            params = emptyMap<Any, Any>().toJsonElement(),
            options = JsonObject(emptyMap())
        ),
        validations = listOf(
            mockedInvalidStateException
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
                mockedAWSCredentialsResponse
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
                mockedAWSCredentialsResponse
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
                mockedSMSChallengeResponse
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
                mockedSMSChallengeResponse
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
                "password" to ""
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
                "password" to ""
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
                "password" to ""
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
                "password" to ""
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
                "password" to ""
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
        customAuthWithSRPCaseWhenAliasIsUsedToSignIn,
        signInWithUserAuthWithNoPreferenceReturnsSelectChallenge,
        signInWithUserAuthWithUnsupportedPreferenceReturnsSelectChallenge,
        signInWithUserAuthWithEmailOtpPreferenceReturnsVerifyChallenge,
        signInWithUserAuthWithSmsOtpPreferenceReturnsVerifyChallenge,
        signInWithUserAuthWithPasswordSrpPreferenceSucceeds,
        signInWithUserAuthWithPasswordSrpPreferenceFails,
        signInWithUserAuthWithPasswordPreferenceSucceeds,
        signInWithUserAuthWithPasswordPreferenceFails,
        autoSignInSucceeds,
        autoSignInWithoutConfirmSignUpFails
    )
}
