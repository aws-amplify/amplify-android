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
import aws.sdk.kotlin.services.cognitoidentityprovider.model.CodeMismatchException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.NotAuthorizedException
import com.amplifyframework.auth.AuthFactorType
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
    private const val CHALLENGE_CODE = "000000"
    private const val USER_ID = "userId"
    private const val USERNAME = "username"
    private const val PASSWORD = "password"
    private const val PHONE = "+12345678900"
    private const val EMAIL = "test@****.com"
    private const val SESSION = "someSession"

    private val mockedRespondToAuthChallengeResponse = MockResponse(
        CognitoType.CognitoIdentityProvider,
        "respondToAuthChallenge",
        ResponseType.Success,
        mapOf(
            "authenticationResult" to mapOf(
                "idToken" to AuthStateJsonGenerator.DUMMY_TOKEN,
                "accessToken" to AuthStateJsonGenerator.DUMMY_TOKEN,
                "refreshToken" to AuthStateJsonGenerator.DUMMY_TOKEN,
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

    private val mockedRespondToAuthSrpResponse = MockResponse(
        CognitoType.CognitoIdentityProvider,
        "respondToAuthChallenge",
        ResponseType.Success,
        mapOf(
            "challengeName" to ChallengeNameType.PasswordVerifier.value,
            "challengeParameters" to mapOf(
                "SALT" to "abc",
                "SECRET_BLOCK" to "secretBlock",
                "SRP_B" to "def",
                "USERNAME" to USERNAME,
                "USER_ID_FOR_SRP" to USER_ID
            )
        ).toJsonElement()
    )

    private fun mockedRespondToAuthEmailOrSmsResponse(challengeNameType: ChallengeNameType): MockResponse {
        val (medium, destination) = if (challengeNameType == ChallengeNameType.EmailOtp) {
            Pair("EMAIL", EMAIL)
        } else {
            Pair("SMS", PHONE)
        }

        return MockResponse(
            CognitoType.CognitoIdentityProvider,
            "respondToAuthChallenge",
            ResponseType.Success,
            mapOf(
                "challengeName" to challengeNameType.value,
                "session" to SESSION,
                "parameters" to JsonObject(emptyMap()),
                "challengeParameters" to mapOf(
                    "CODE_DELIVERY_DELIVERY_MEDIUM" to medium,
                    "CODE_DELIVERY_DESTINATION" to destination
                )
            ).toJsonElement()
        )
    }

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
                "sessionToken" to AuthStateJsonGenerator.DUMMY_TOKEN,
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
                "additionalInfo" to JsonObject(emptyMap())
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
                )
            )
        ).toJsonElement()
    )

    private val mockedRespondToAuthNotAuthorizedException = MockResponse(
        CognitoType.CognitoIdentityProvider,
        "respondToAuthChallenge",
        ResponseType.Failure,
        NotAuthorizedException.invoke {
            message = "Incorrect username or password."
        }.toJsonElement()
    )

    private val mockedRespondToAuthCodeMismatchException = MockResponse(
        CognitoType.CognitoIdentityProvider,
        "respondToAuthChallenge",
        ResponseType.Failure,
        CodeMismatchException.invoke {
            message = "Confirmation code entered is not correct."
        }.toJsonElement()
    )

    private val notAuthorizedExceptionExpectation = ExpectationShapes.Amplify(
        AuthAPI.confirmSignIn,
        ResponseType.Failure,
        com.amplifyframework.auth.exceptions.NotAuthorizedException(
            cause = NotAuthorizedException.invoke {
                message = "Incorrect username or password."
            }
        ).toJsonElement()
    )

    private val codeMismatchExceptionExpectation = ExpectationShapes.Amplify(
        AuthAPI.confirmSignIn,
        ResponseType.Failure,
        CognitoAuthExceptionConverter.lookup(
            CodeMismatchException.invoke {
                message = "Confirmation code entered is not correct."
            },
            "Confirm Sign in failed."
        ).toJsonElement()
    )

    private fun mockedConfirmSignInWithOtpExpectation(challengeNameType: ChallengeNameType): ExpectationShapes.Amplify {
        val (medium, destination) = if (challengeNameType == ChallengeNameType.EmailOtp) {
            Pair("EMAIL", EMAIL)
        } else {
            Pair("SMS", PHONE)
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

    private val baseCase = FeatureTestCase(
        description = "Test that SignIn with SMS challenge invokes proper cognito request and returns success",
        preConditions = PreConditions(
            "authconfiguration.json",
            "SigningIn_SigningIn.json",
            mockedResponses = listOf(
                mockedRespondToAuthChallengeResponse,
                mockedIdentityIdResponse,
                mockedAWSCredentialsResponse
            )
        ),
        api = API(
            AuthAPI.confirmSignIn,
            params = mapOf(
                "challengeResponse" to CHALLENGE_CODE
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
            "SigningIn_SigningIn_Custom.json",
            mockedResponses = listOf(
                mockedRespondToAuthCustomChallengeResponse
            )
        ),
        api = API(
            AuthAPI.confirmSignIn,
            params = mapOf(
                "challengeResponse" to CHALLENGE_CODE
            ).toJsonElement(),
            options = JsonObject(emptyMap())
        ),
        validations = listOf(
            mockedConfirmSignInSuccessWithChallengeExpectation,
            ExpectationShapes.State("SigningIn_CustomChallenge.json")
        )
    )

    // SELECT_CHALLENGE > Select Email OTP
    private val userAuthSelectEmailOtpChallenge = FeatureTestCase(
        description = "Test that selecting the email OTP challenge returns the proper state",
        preConditions = PreConditions(
            "authconfiguration_userauth.json",
            "SigningIn_SelectChallenge.json",
            mockedResponses = listOf(
                mockedRespondToAuthEmailOrSmsResponse(ChallengeNameType.EmailOtp)
            )
        ),
        api = API(
            AuthAPI.confirmSignIn,
            params = mapOf(
                "challengeResponse" to AuthFactorType.EMAIL_OTP.challengeResponse
            ).toJsonElement(),
            options = JsonObject(emptyMap())
        ),
        validations = listOf(
            mockedConfirmSignInWithOtpExpectation(ChallengeNameType.EmailOtp),
            ExpectationShapes.State("SigningIn_EmailOtp.json")
        )
    )

    // Email OTP > Enter Correct Challenge Code
    private val userAuthConfirmEmailOtpCodeSucceeds = FeatureTestCase(
        description = "Test that entering the correct email OTP code signs the user in",
        preConditions = PreConditions(
            "authconfiguration_userauth.json",
            "SigningIn_EmailOtp.json",
            mockedResponses = listOf(
                mockedRespondToAuthChallengeResponse,
                mockedIdentityIdResponse,
                mockedAWSCredentialsResponse
            )
        ),
        api = API(
            AuthAPI.confirmSignIn,
            params = mapOf(
                "challengeResponse" to CHALLENGE_CODE
            ).toJsonElement(),
            options = JsonObject(emptyMap())
        ),
        validations = listOf(
            mockedSignInSuccessExpectation,
            ExpectationShapes.State("SignedIn_SessionEstablished_User_Auth.json")
        )
    )

    // Email OTP > Enter Incorrect Challenge Code
    private val userAuthConfirmEmailOtpCodeFails = FeatureTestCase(
        description = "Test that entering the incorrect email OTP code fails",
        preConditions = PreConditions(
            "authconfiguration_userauth.json",
            "SigningIn_EmailOtp.json",
            mockedResponses = listOf(
                mockedRespondToAuthCodeMismatchException
            )
        ),
        api = API(
            AuthAPI.confirmSignIn,
            params = mapOf(
                "challengeResponse" to CHALLENGE_CODE
            ).toJsonElement(),
            options = JsonObject(emptyMap())
        ),
        validations = listOf(
            codeMismatchExceptionExpectation
        )
    )

    // SELECT_CHALLENGE > Select SMS OTP
    private val userAuthSelectSmsOtpChallenge = FeatureTestCase(
        description = "Test that selecting the SMS OTP challenge returns the proper state",
        preConditions = PreConditions(
            "authconfiguration_userauth.json",
            "SigningIn_SelectChallenge.json",
            mockedResponses = listOf(
                mockedRespondToAuthEmailOrSmsResponse(ChallengeNameType.SmsOtp)
            )
        ),
        api = API(
            AuthAPI.confirmSignIn,
            params = mapOf(
                "challengeResponse" to AuthFactorType.SMS_OTP.challengeResponse
            ).toJsonElement(),
            options = JsonObject(emptyMap())
        ),
        validations = listOf(
            mockedConfirmSignInWithOtpExpectation(ChallengeNameType.SmsOtp),
            ExpectationShapes.State("SigningIn_SmsOtp.json")
        )
    )

    // SMS OTP > Enter Correct Challenge Code
    private val userAuthConfirmSmsOtpCodeSucceeds = FeatureTestCase(
        description = "Test that entering the correct SMS OTP code signs the user in",
        preConditions = PreConditions(
            "authconfiguration_userauth.json",
            "SigningIn_SmsOtp.json",
            mockedResponses = listOf(
                mockedRespondToAuthChallengeResponse,
                mockedIdentityIdResponse,
                mockedAWSCredentialsResponse
            )
        ),
        api = API(
            AuthAPI.confirmSignIn,
            params = mapOf(
                "challengeResponse" to CHALLENGE_CODE
            ).toJsonElement(),
            options = JsonObject(emptyMap())
        ),
        validations = listOf(
            mockedSignInSuccessExpectation,
            ExpectationShapes.State("SignedIn_SessionEstablished_User_Auth.json")
        )
    )

    // SMS OTP > Enter Incorrect Challenge Code
    private val userAuthConfirmSmsOtpCodeFails = FeatureTestCase(
        description = "Test that entering the incorrect SMS OTP code fails",
        preConditions = PreConditions(
            "authconfiguration_userauth.json",
            "SigningIn_SmsOtp.json",
            mockedResponses = listOf(
                mockedRespondToAuthCodeMismatchException
            )
        ),
        api = API(
            AuthAPI.confirmSignIn,
            params = mapOf(
                "challengeResponse" to CHALLENGE_CODE
            ).toJsonElement(),
            options = JsonObject(emptyMap())
        ),
        validations = listOf(
            codeMismatchExceptionExpectation
        )
    )

    // SELECT_CHALLENGE > Select Password w/Correct Password
    private val userAuthSelectPasswordChallengeSucceeds = FeatureTestCase(
        description = "Test that selecting the PASSWORD challenge with the correct password succeeds",
        preConditions = PreConditions(
            "authconfiguration_userauth.json",
            "SigningIn_SelectChallenge.json",
            mockedResponses = listOf(
                mockedRespondToAuthChallengeResponse,
                mockedIdentityIdResponse,
                mockedAWSCredentialsResponse
            )
        ),
        api = API(
            AuthAPI.confirmSignIn,
            params = mapOf(
                "challengeResponse" to PASSWORD
            ).toJsonElement(),
            options = JsonObject(emptyMap())
        ),
        validations = listOf(
            mockedSignInSuccessExpectation,
            ExpectationShapes.State("SignedIn_SessionEstablished_User_Auth.json")
        )
    )

    // SELECT_CHALLENGE > Select Password w/Incorrect Password
    private val userAuthSelectPasswordChallengeFails = FeatureTestCase(
        description = "Test that selecting the PASSWORD challenge with the incorrect password fails",
        preConditions = PreConditions(
            "authconfiguration_userauth.json",
            "SigningIn_SelectChallenge.json",
            mockedResponses = listOf(
                mockedRespondToAuthNotAuthorizedException
            )
        ),
        api = API(
            AuthAPI.confirmSignIn,
            params = mapOf(
                "challengeResponse" to PASSWORD
            ).toJsonElement(),
            options = JsonObject(emptyMap())
        ),
        validations = listOf(
            notAuthorizedExceptionExpectation
        )
    )

    // SELECT_CHALLENGE > Select Password_SRP w/Correct Password
    private val userAuthSelectPasswordSrpChallengeSucceeds = FeatureTestCase(
        description = "Test that selecting the PASSWORD_SRP challenge with the correct password succeeds",
        preConditions = PreConditions(
            "authconfiguration_userauth.json",
            "SigningIn_SelectChallenge.json",
            mockedResponses = listOf(
                mockedRespondToAuthSrpResponse,
                mockedRespondToAuthChallengeResponse,
                mockedIdentityIdResponse,
                mockedAWSCredentialsResponse
            )
        ),
        api = API(
            AuthAPI.confirmSignIn,
            params = mapOf(
                "challengeResponse" to PASSWORD
            ).toJsonElement(),
            options = JsonObject(emptyMap())
        ),
        validations = listOf(
            mockedSignInSuccessExpectation,
            ExpectationShapes.State("SignedIn_SessionEstablished_User_Auth.json")
        )
    )

    // SELECT_CHALLENGE > Select Password_SRP w/Incorrect Password
    private val userAuthSelectPasswordSrpChallengeFails = FeatureTestCase(
        description = "Test that selecting the PASSWORD_SRP challenge with the incorrect password fails",
        preConditions = PreConditions(
            "authconfiguration_userauth.json",
            "SigningIn_SelectChallenge.json",
            mockedResponses = listOf(
                mockedRespondToAuthNotAuthorizedException
            )
        ),
        api = API(
            AuthAPI.confirmSignIn,
            params = mapOf(
                "challengeResponse" to PASSWORD
            ).toJsonElement(),
            options = JsonObject(emptyMap())
        ),
        validations = listOf(
            notAuthorizedExceptionExpectation
        )
    )

    override val serializables: List<Any> = listOf(
        baseCase,
        errorCase,
        successCaseWithSecondaryChallenge,
        userAuthSelectEmailOtpChallenge,
        userAuthConfirmEmailOtpCodeSucceeds,
        userAuthConfirmEmailOtpCodeFails,
        userAuthSelectSmsOtpChallenge,
        userAuthConfirmSmsOtpCodeSucceeds,
        userAuthConfirmSmsOtpCodeFails,
        userAuthSelectPasswordChallengeSucceeds,
        userAuthSelectPasswordChallengeFails,
        userAuthSelectPasswordSrpChallengeSucceeds,
        userAuthSelectPasswordSrpChallengeFails
    )
}
