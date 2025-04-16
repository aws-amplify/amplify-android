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

import aws.sdk.kotlin.services.cognitoidentityprovider.model.InvalidParameterException
import aws.sdk.kotlin.services.cognitoidentityprovider.model.UsernameExistsException
import com.amplifyframework.auth.AuthCodeDeliveryDetails
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.cognito.featuretest.API
import com.amplifyframework.auth.cognito.featuretest.AuthAPI
import com.amplifyframework.auth.cognito.featuretest.CognitoType
import com.amplifyframework.auth.cognito.featuretest.ExpectationShapes
import com.amplifyframework.auth.cognito.featuretest.FeatureTestCase
import com.amplifyframework.auth.cognito.featuretest.MockResponse
import com.amplifyframework.auth.cognito.featuretest.PreConditions
import com.amplifyframework.auth.cognito.featuretest.ResponseType
import com.amplifyframework.auth.cognito.featuretest.generators.SerializableProvider
import com.amplifyframework.auth.cognito.featuretest.generators.toJsonElement
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.auth.result.step.AuthNextSignUpStep
import com.amplifyframework.auth.result.step.AuthSignUpStep

object SignUpTestCaseGenerator : SerializableProvider {
    private val username = "user"
    private val existingUsername = "anExistingUsername"
    private val invalidUsername = "anInvalidUsername"
    private val password = "password"
    private val email = "user@domain.com"
    private val session = "session-id"

    private val codeDeliveryDetails = mapOf(
        "destination" to email,
        "deliveryMedium" to "EMAIL",
        "attributeName" to "attributeName"
    )

    private val emptyCodeDeliveryDetails = mapOf(
        "destination" to "",
        "deliveryMedium" to "",
        "attributeName" to ""
    )

// mock responses for non-passwordless flow region starts
    private val mockedUnconfirmedSignUpResponse = MockResponse(
        CognitoType.CognitoIdentityProvider,
        "signUp",
        ResponseType.Success,
        mapOf(
            "codeDeliveryDetails" to codeDeliveryDetails
        ).toJsonElement()
    )

    private val mockedConfirmedSignUpResponse = MockResponse(
        CognitoType.CognitoIdentityProvider,
        "signUp",
        ResponseType.Success,
        mapOf(
            "codeDeliveryDetails" to emptyCodeDeliveryDetails,
            "userConfirmed" to true
        ).toJsonElement()
    )
// mock responses for non-passwordless flow region starts

// mock responses for passwordless flow region starts
    private val mockedPasswordlessUnconfirmedSignUpResponse = MockResponse(
        CognitoType.CognitoIdentityProvider,
        "signUp",
        ResponseType.Success,
        mapOf(
            "codeDeliveryDetails" to codeDeliveryDetails,
            "session" to session
        ).toJsonElement()
    )

    private val mockedPasswordlessConfirmedSignUpResponse = MockResponse(
        CognitoType.CognitoIdentityProvider,
        "signUp",
        ResponseType.Success,
        mapOf(
            "codeDeliveryDetails" to emptyCodeDeliveryDetails,
            "session" to session,
            "userConfirmed" to true
        ).toJsonElement()
    )
// mock responses for passwordless flow region starts

// mock error responses flow region starts
    private val usernameExistsException = UsernameExistsException.invoke {}
    private val usernameInvalidException = InvalidParameterException.invoke {}

    private val mockedSignUpWithExistingUsernameResponse = MockResponse(
        CognitoType.CognitoIdentityProvider,
        "signUp",
        ResponseType.Failure,
        usernameExistsException.toJsonElement()
    )

    private val mockedSignUpWithInvalidUsernameResponse = MockResponse(
        CognitoType.CognitoIdentityProvider,
        "signUp",
        ResponseType.Failure,
        usernameInvalidException.toJsonElement()
    )
// mock error responses flow region starts

    private fun expectedCognitoSignUpRequest(username: String, password: String?) =
        ExpectationShapes.Cognito.CognitoIdentityProvider(
            apiName = "signUp",
            // see [https://docs.aws.amazon.com/cognito-user-identity-pools/latest/APIReference/API_SignUp.html]
            request = mapOf(
                "clientId" to "testAppClientId", // This should be pulled from configuration
                "username" to username,
                "password" to password,
                "userAttributes" to listOf(mapOf("name" to "email", "value" to email))
            ).toJsonElement()
        )

    val baseCase = FeatureTestCase(
        description = "Test that signup invokes proper cognito request and returns success",
        preConditions = PreConditions(
            "authconfiguration.json",
            "SignedOut_Configured.json",
            mockedResponses = listOf(
                mockedUnconfirmedSignUpResponse
            )
        ),
        api = API(
            AuthAPI.signUp,
            params = mapOf(
                "username" to username,
                "password" to password
            ).toJsonElement(),
            options = mapOf(
                "userAttributes" to mapOf(AuthUserAttributeKey.email().keyString to email)
            ).toJsonElement()
        ),
        validations = listOf(
            expectedCognitoSignUpRequest(username, password),
            ExpectationShapes.Amplify(
                apiName = AuthAPI.signUp,
                responseType = ResponseType.Success,
                response = AuthSignUpResult(
                    false,
                    AuthNextSignUpStep(
                        AuthSignUpStep.CONFIRM_SIGN_UP_STEP,
                        emptyMap(),
                        AuthCodeDeliveryDetails(
                            email,
                            AuthCodeDeliveryDetails.DeliveryMedium.EMAIL,
                            "attributeName"
                        )
                    ),
                    "" // aligned with mock in CognitoMockFactory
                ).toJsonElement()
            )
        )
    )

    val signupSuccessCase = baseCase.copy(
        description = "Sign up finishes if user is confirmed in the first step",
        preConditions = baseCase.preConditions.copy(
            mockedResponses = listOf(
                mockedConfirmedSignUpResponse
            )
        ),
        validations = listOf(
            expectedCognitoSignUpRequest(username, password),
            ExpectationShapes.Amplify(
                apiName = AuthAPI.signUp,
                responseType = ResponseType.Success,
                response =
                AuthSignUpResult(
                    true,
                    AuthNextSignUpStep(
                        AuthSignUpStep.DONE,
                        emptyMap(),
                        AuthCodeDeliveryDetails(
                            "",
                            AuthCodeDeliveryDetails.DeliveryMedium.UNKNOWN,
                            ""
                        )
                    ),
                    "" // aligned with mock in CognitoMockFactory
                ).toJsonElement()
            )
        )
    )

    private val passwordlessUnconfirmedSignUpWithValidUsernameReturnsConfirmSignUpStep = FeatureTestCase(
        description = "Test that passwordless uncofirmed signUp with valid username returns ConfirmSignUpStep",
        preConditions = PreConditions(
            "authconfiguration_userauth.json",
            "SignedOut_Configured.json",
            mockedResponses = listOf(
                mockedPasswordlessUnconfirmedSignUpResponse
            )
        ),
        api = API(
            AuthAPI.signUp,
            params = mapOf(
                "username" to username,
                "password" to ""
            ).toJsonElement(),
            options = mapOf(
                "userAttributes" to mapOf(AuthUserAttributeKey.email().keyString to email)
            ).toJsonElement()
        ),
        validations = listOf(
            expectedCognitoSignUpRequest(username, ""),
            ExpectationShapes.Amplify(
                apiName = AuthAPI.signUp,
                responseType = ResponseType.Success,
                response = AuthSignUpResult(
                    false,
                    AuthNextSignUpStep(
                        AuthSignUpStep.CONFIRM_SIGN_UP_STEP,
                        emptyMap(),
                        AuthCodeDeliveryDetails(
                            email,
                            AuthCodeDeliveryDetails.DeliveryMedium.EMAIL,
                            "attributeName"
                        )
                    ),
                    "" // aligned with mock in CognitoMockFactory
                ).toJsonElement()
            )
        )
    )

    private val passwordlessConfirmedSignUpWithValidUsernameReturnsCompleteAutoSignIn = FeatureTestCase(
        description = "Test that passwordless confirmed signUp with valid username returns CompleteAutoSignIn",
        preConditions = PreConditions(
            "authconfiguration_userauth.json",
            "SignedOut_Configured.json",
            mockedResponses = listOf(
                mockedPasswordlessConfirmedSignUpResponse
            )
        ),
        api = API(
            AuthAPI.signUp,
            params = mapOf(
                "username" to username,
                "password" to ""
            ).toJsonElement(),
            options = mapOf(
                "userAttributes" to mapOf(AuthUserAttributeKey.email().keyString to email)
            ).toJsonElement()
        ),
        validations = listOf(
            expectedCognitoSignUpRequest(username, ""),
            ExpectationShapes.Amplify(
                apiName = AuthAPI.signUp,
                responseType = ResponseType.Success,
                response = AuthSignUpResult(
                    true,
                    AuthNextSignUpStep(
                        AuthSignUpStep.COMPLETE_AUTO_SIGN_IN,
                        emptyMap(),
                        AuthCodeDeliveryDetails(
                            "",
                            AuthCodeDeliveryDetails.DeliveryMedium.UNKNOWN,
                            ""
                        )
                    ),
                    "" // aligned with mock in CognitoMockFactory
                ).toJsonElement()
            )
        )
    )

    private val passwordlessSignUpWithExistingUsernameFails = FeatureTestCase(
        description = "Test that passwordless signUp with an existing username fails",
        preConditions = PreConditions(
            "authconfiguration_userauth.json",
            "SignedOut_Configured.json",
            mockedResponses = listOf(
                mockedSignUpWithExistingUsernameResponse
            )
        ),
        api = API(
            AuthAPI.signUp,
            params = mapOf(
                "username" to existingUsername,
                "password" to ""
            ).toJsonElement(),
            options = mapOf(
                "userAttributes" to mapOf(AuthUserAttributeKey.email().keyString to email)
            ).toJsonElement()
        ),
        validations = listOf(
            expectedCognitoSignUpRequest(existingUsername, ""),
            ExpectationShapes.Amplify(
                AuthAPI.signUp,
                ResponseType.Failure,
                com.amplifyframework.auth.cognito.exceptions.service.UsernameExistsException(
                    usernameExistsException
                ).toJsonElement()
            )
        )
    )

    private val passwordlessSignUpWithInvalidUsernameFails = FeatureTestCase(
        description = "Test that passwordless signUp with an invalid username fails",
        preConditions = PreConditions(
            "authconfiguration_userauth.json",
            "SignedOut_Configured.json",
            mockedResponses = listOf(
                mockedSignUpWithInvalidUsernameResponse
            )
        ),
        api = API(
            AuthAPI.signUp,
            params = mapOf(
                "username" to invalidUsername,
                "password" to ""
            ).toJsonElement(),
            options = mapOf(
                "userAttributes" to mapOf(AuthUserAttributeKey.email().keyString to email)
            ).toJsonElement()
        ),
        validations = listOf(
            expectedCognitoSignUpRequest(invalidUsername, ""),
            ExpectationShapes.Amplify(
                AuthAPI.signUp,
                ResponseType.Failure,
                com.amplifyframework.auth.cognito.exceptions.service.InvalidParameterException(
                    cause = usernameInvalidException
                ).toJsonElement()
            )
        )
    )

    override val serializables: List<Any> = listOf(
        baseCase,
        signupSuccessCase,
        passwordlessUnconfirmedSignUpWithValidUsernameReturnsConfirmSignUpStep,
        passwordlessConfirmedSignUpWithValidUsernameReturnsCompleteAutoSignIn,
        passwordlessSignUpWithExistingUsernameFails,
        passwordlessSignUpWithInvalidUsernameFails
    )
}
