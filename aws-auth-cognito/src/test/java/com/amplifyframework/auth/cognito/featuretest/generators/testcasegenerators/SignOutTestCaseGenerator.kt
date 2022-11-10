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

import aws.sdk.kotlin.services.cognitoidentityprovider.model.NotAuthorizedException
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
import com.amplifyframework.auth.cognito.result.AWSCognitoAuthSignOutResult
import com.amplifyframework.auth.cognito.result.GlobalSignOutError
import com.amplifyframework.auth.cognito.result.RevokeTokenError
import com.amplifyframework.statemachine.codegen.data.GlobalSignOutErrorData
import com.amplifyframework.statemachine.codegen.data.RevokeTokenErrorData
import kotlinx.serialization.json.JsonObject

object SignOutTestCaseGenerator : SerializableProvider {

    private val mockedGlobalSignOutSuccessResponse = MockResponse(
        CognitoType.CognitoIdentityProvider,
        "globalSignOut",
        ResponseType.Success,
        JsonObject(emptyMap())
    )

    private val mockedGlobalSignOutFailureResponse = MockResponse(
        CognitoType.CognitoIdentityProvider,
        "globalSignOut",
        ResponseType.Failure,
        NotAuthorizedException.invoke {}.toJsonElement()
    )


    private val mockedRevokeTokenSuccessResponse = MockResponse(
        CognitoType.CognitoIdentityProvider,
        "revokeToken",
        ResponseType.Success,
        JsonObject(emptyMap())
    )

    private val mockedRevokeTokenFailureResponse = MockResponse(
        CognitoType.CognitoIdentityProvider,
        "revokeToken",
        ResponseType.Failure,
        NotAuthorizedException.invoke {}.toJsonElement()
    )

    private val successCase = FeatureTestCase(
        description = "Test that signOut while signed in returns complete with success",
        preConditions = PreConditions(
            "authconfiguration.json",
            "SignedIn_SessionEstablished.json",
            mockedResponses = listOf(
                mockedRevokeTokenSuccessResponse
            )
        ),
        api = API(
            AuthAPI.signOut,
            params = emptyMap<String, String>().toJsonElement(),
            options = mapOf(
                "globalSignOut" to false
            ).toJsonElement()
        ),
        validations = listOf(
            ExpectationShapes.Amplify(
                apiName = AuthAPI.signOut,
                responseType = ResponseType.Complete,
                response = AWSCognitoAuthSignOutResult.CompleteSignOut.toJsonElement()
            )
        )
    )

    private val signedOutSuccessCase = successCase.copy(
        description = "Test that signOut while already signed out returns complete with success",
        preConditions = successCase.preConditions.copy(
            state = "SignedOut_Configured.json",
            mockedResponses = emptyList()
        )
    )

    private val globalSuccessCase = successCase.copy(
        description = "Test that global signOut while signed in returns complete with success",
        preConditions = successCase.preConditions.copy(
            mockedResponses = listOf(
                mockedGlobalSignOutSuccessResponse,
                mockedRevokeTokenSuccessResponse
            )
        ),
        api = successCase.api.copy(
            options = mapOf("globalSignOut" to true).toJsonElement()
        )
    )

    private val revokeTokenErrorCase = successCase.copy(
        description = "Test that signOut returns partial success with revoke token error",
        preConditions = successCase.preConditions.copy(
            mockedResponses = listOf(
                mockedRevokeTokenFailureResponse
            )
        ),
        validations = listOf(
            ExpectationShapes.Amplify(
                apiName = AuthAPI.signOut,
                responseType = ResponseType.Complete,
                response = AWSCognitoAuthSignOutResult.PartialSignOut(
                    revokeTokenError = RevokeTokenError(
                        RevokeTokenErrorData(
                            refreshToken = AuthStateJsonGenerator.dummyToken,
                            error = NotAuthorizedException.invoke {  }
                        )
                    )
                ).toJsonElement()
            )
        )
    )

    private val revokeTokenWithGlobalSignOutErrorCase = successCase.copy(
        description = "Test that globalSignOut returns partial success with revoke token error",
        preConditions = successCase.preConditions.copy(
            mockedResponses = listOf(
                mockedGlobalSignOutSuccessResponse,
                mockedRevokeTokenFailureResponse
            )
        ),
        validations = listOf(
            ExpectationShapes.Amplify(
                apiName = AuthAPI.signOut,
                responseType = ResponseType.Complete,
                response = AWSCognitoAuthSignOutResult.PartialSignOut(
                    revokeTokenError = RevokeTokenError(
                        RevokeTokenErrorData(
                            refreshToken = AuthStateJsonGenerator.dummyToken,
                            error = NotAuthorizedException.invoke { }
                        )
                    )
                ).toJsonElement()
            )
        ),
        api = successCase.api.copy(
            options = mapOf("globalSignOut" to true).toJsonElement()
        )
    )

    private val globalErrorCase = successCase.copy(
        description = "Test that global signOut error returns partial success with global sign out error",
        preConditions = successCase.preConditions.copy(
            mockedResponses = listOf(mockedGlobalSignOutFailureResponse)
        ),
        validations = listOf(
            ExpectationShapes.Amplify(
                apiName = AuthAPI.signOut,
                responseType = ResponseType.Complete,
                response = AWSCognitoAuthSignOutResult.PartialSignOut(
                    globalSignOutError = GlobalSignOutError(
                        GlobalSignOutErrorData(
                            accessToken = AuthStateJsonGenerator.dummyToken,
                            error = NotAuthorizedException.invoke {  }
                        )
                    ),
                    revokeTokenError = RevokeTokenError(
                        RevokeTokenErrorData(
                            refreshToken = AuthStateJsonGenerator.dummyToken,
                            error = Exception("RevokeToken not attempted because GlobalSignOut failed.")
                        )
                    )
                ).toJsonElement()
            )
        ),
        api = successCase.api.copy(
            options = mapOf("globalSignOut" to true).toJsonElement()
        )
    )

    override val serializables: List<Any> = listOf(
        successCase,
        signedOutSuccessCase,
        revokeTokenErrorCase,
        revokeTokenWithGlobalSignOutErrorCase,
        globalSuccessCase,
        globalErrorCase
    )
}
