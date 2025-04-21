/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import aws.sdk.kotlin.services.cognitoidentityprovider.model.InvalidParameterException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.UserNotFoundException
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
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.auth.result.step.AuthNextSignUpStep
import com.amplifyframework.auth.result.step.AuthSignUpStep
import kotlinx.serialization.json.JsonObject

object ConfirmSignUpTestCaseGenerator : SerializableProvider {
    private val username = AuthStateJsonGenerator.USERNAME
    private val session = AuthStateJsonGenerator.SESSION
    private val confirmationCode = "123"

    private val unregisteredUserException = UserNotFoundException.invoke {}
    private val invalidUsernameException = InvalidParameterException.invoke {}
    private val invalidConfirmationCodeException = CodeMismatchException.invoke {}

    private val expectedPasswordlessCognitoConfirmSignUpRequest = ExpectationShapes.Cognito.CognitoIdentityProvider(
        apiName = "confirmSignUp",
        request = mapOf(
            "clientId" to "testAppClientId", // This should be pulled from configuration
            "username" to username,
            "confirmationCode" to confirmationCode,
            "session" to session // non-null value in passwordless (is set to session stored in previous state)
        ).toJsonElement()
    )

    private val expectedNonPasswordlessCognitoConfirmSignUpRequest = ExpectationShapes.Cognito.CognitoIdentityProvider(
        apiName = "confirmSignUp",
        request = mapOf(
            "clientId" to "testAppClientId", // This should be pulled from configuration
            "username" to username,
            "confirmationCode" to confirmationCode
            // the retrieved session from the previous state is null in non-passwordless
        ).toJsonElement()
    )

    private val passwordlessConfirmSignUpReturnsCompleteAutoSignIn = FeatureTestCase(
        description = "Test that passwordless confirmSignUp returns CompleteAutoSignIn",
        preConditions = PreConditions(
            "authconfiguration_userauth.json",
            "SignedOut_Configured_AwaitingUserConfirmation.json",
            mockedResponses = listOf(
                MockResponse(
                    CognitoType.CognitoIdentityProvider,
                    "confirmSignUp",
                    ResponseType.Success,
                    mapOf(
                        "session" to session
                    ).toJsonElement()
                )
            )
        ),
        api = API(
            AuthAPI.confirmSignUp,
            params = mapOf(
                "username" to username,
                "confirmationCode" to confirmationCode
            ).toJsonElement(),
            options = JsonObject(emptyMap())
        ),
        validations = listOf(
            expectedPasswordlessCognitoConfirmSignUpRequest,
            ExpectationShapes.Amplify(
                apiName = AuthAPI.confirmSignUp,
                responseType = ResponseType.Success,
                response = AuthSignUpResult(
                    true,
                    AuthNextSignUpStep(
                        AuthSignUpStep.COMPLETE_AUTO_SIGN_IN,
                        emptyMap(),
                        null
                    ),
                    "" // set to userId stored in previous state
                ).toJsonElement()
            )
        )
    )

    private val passwordlessConfirmSignUpWithUnregisteredUserReturnsException = FeatureTestCase(
        description = "Test that passwordless confirmSignUp with Unregistered User returns Exception",
        preConditions = PreConditions(
            "authconfiguration_userauth.json",
            "SignedOut_Configured_AwaitingUserConfirmation.json",
            mockedResponses = listOf(
                MockResponse(
                    CognitoType.CognitoIdentityProvider,
                    "confirmSignUp",
                    ResponseType.Failure,
                    unregisteredUserException.toJsonElement()
                )
            )
        ),
        api = API(
            AuthAPI.confirmSignUp,
            params = mapOf(
                "username" to username,
                "confirmationCode" to confirmationCode
            ).toJsonElement(),
            options = JsonObject(emptyMap())
        ),
        validations = listOf(
            expectedPasswordlessCognitoConfirmSignUpRequest,
            ExpectationShapes.Amplify(
                apiName = AuthAPI.confirmSignUp,
                responseType = ResponseType.Failure,
                com.amplifyframework.auth.cognito.exceptions.service.UserNotFoundException(
                    unregisteredUserException
                ).toJsonElement()
            )
        )
    )

    private val passwordlessConfirmSignUpWithInvalidUsernameReturnsException = FeatureTestCase(
        description = "Test that passwordless confirmSignUp with Invalid Username returns Exception",
        preConditions = PreConditions(
            "authconfiguration_userauth.json",
            "SignedOut_Configured_AwaitingUserConfirmation.json",
            mockedResponses = listOf(
                MockResponse(
                    CognitoType.CognitoIdentityProvider,
                    "confirmSignUp",
                    ResponseType.Failure,
                    invalidUsernameException.toJsonElement()
                )
            )
        ),
        api = API(
            AuthAPI.confirmSignUp,
            params = mapOf(
                "username" to username,
                "confirmationCode" to confirmationCode
            ).toJsonElement(),
            options = JsonObject(emptyMap())
        ),
        validations = listOf(
            expectedPasswordlessCognitoConfirmSignUpRequest,
            ExpectationShapes.Amplify(
                apiName = AuthAPI.confirmSignUp,
                responseType = ResponseType.Failure,
                com.amplifyframework.auth.cognito.exceptions.service.InvalidParameterException(
                    cause = invalidUsernameException
                ).toJsonElement()
            )
        )
    )

    private val passwordlessConfirmSignUpWithInvalidConfirmationCodeReturnsException = FeatureTestCase(
        description = "Test that passwordless confirmSignUp with Invalid Confirmation Code returns Exception",
        preConditions = PreConditions(
            "authconfiguration_userauth.json",
            "SignedOut_Configured_AwaitingUserConfirmation.json",
            mockedResponses = listOf(
                MockResponse(
                    CognitoType.CognitoIdentityProvider,
                    "confirmSignUp",
                    ResponseType.Failure,
                    invalidConfirmationCodeException.toJsonElement()
                )
            )
        ),
        api = API(
            AuthAPI.confirmSignUp,
            params = mapOf(
                "username" to username,
                "confirmationCode" to confirmationCode
            ).toJsonElement(),
            options = JsonObject(emptyMap())
        ),
        validations = listOf(
            expectedPasswordlessCognitoConfirmSignUpRequest,
            ExpectationShapes.Amplify(
                apiName = AuthAPI.confirmSignUp,
                responseType = ResponseType.Failure,
                com.amplifyframework.auth.cognito.exceptions.service.CodeMismatchException(
                    cause = invalidConfirmationCodeException
                ).toJsonElement()
            )
        )
    )

    private val nonpasswordlessConfirmSignUpReturnsDone = FeatureTestCase(
        description = "Test that non passwordless confirmSignUp returns Done",
        preConditions = PreConditions(
            "authconfiguration_userauth.json",
            "SignedOut_SessionEstablished_AwaitingUserConfirmation.json",
            mockedResponses = listOf(
                MockResponse(
                    CognitoType.CognitoIdentityProvider,
                    "confirmSignUp",
                    ResponseType.Success,
                    emptyMap<Any, Any>().toJsonElement()
                )
            )
        ),
        api = API(
            AuthAPI.confirmSignUp,
            params = mapOf(
                "username" to username,
                "confirmationCode" to confirmationCode
            ).toJsonElement(),
            options = JsonObject(emptyMap())
        ),
        validations = listOf(
            expectedNonPasswordlessCognitoConfirmSignUpRequest,
            ExpectationShapes.Amplify(
                apiName = AuthAPI.confirmSignUp,
                responseType = ResponseType.Success,
                response = AuthSignUpResult(
                    true,
                    AuthNextSignUpStep(
                        AuthSignUpStep.DONE,
                        emptyMap(),
                        null
                    ),
                    "" // set to userId stored in previous state
                ).toJsonElement()
            )
        )
    )

    override val serializables: List<Any> = listOf(
        passwordlessConfirmSignUpReturnsCompleteAutoSignIn,
        passwordlessConfirmSignUpWithUnregisteredUserReturnsException,
        passwordlessConfirmSignUpWithInvalidUsernameReturnsException,
        passwordlessConfirmSignUpWithInvalidConfirmationCodeReturnsException,
        nonpasswordlessConfirmSignUpReturnsDone
    )
}
