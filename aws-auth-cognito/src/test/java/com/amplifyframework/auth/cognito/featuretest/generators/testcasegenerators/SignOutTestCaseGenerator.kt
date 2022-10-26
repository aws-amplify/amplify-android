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

import com.amplifyframework.auth.cognito.featuretest.API
import com.amplifyframework.auth.cognito.featuretest.AuthAPI
import com.amplifyframework.auth.cognito.featuretest.ExpectationShapes
import com.amplifyframework.auth.cognito.featuretest.FeatureTestCase
import com.amplifyframework.auth.cognito.featuretest.MockResponse
import com.amplifyframework.auth.cognito.featuretest.PreConditions
import com.amplifyframework.auth.cognito.featuretest.ResponseType
import com.amplifyframework.auth.cognito.featuretest.generators.SerializableProvider
import com.amplifyframework.auth.cognito.featuretest.generators.toJsonElement
import com.amplifyframework.auth.cognito.result.AWSCognitoAuthSignOutResult
import kotlinx.serialization.json.JsonObject

object SignOutTestCaseGenerator : SerializableProvider {

    private val mockedGlobalSignOutSuccessResponse = MockResponse(
        "cognito",
        "globalSignOut",
        ResponseType.Success,
        JsonObject(emptyMap())
    )

    private val mockedRevokeTokenSignOutSuccessResponse = MockResponse(
        "cognito",
        "revokeToken",
        ResponseType.Success,
        JsonObject(emptyMap())
    )

    private val signedOutSuccessCase = FeatureTestCase(
        description = "Test that signOut while already signed out returns complete with success",
        preConditions = PreConditions(
            "authconfiguration.json",
            "SignedOut_Configured.json",
            mockedResponses = emptyList()
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

    private val signedInSuccessCase = FeatureTestCase(
        description = "Test that signOut while signed in returns complete with success",
        preConditions = PreConditions(
            "authconfiguration.json",
            "SignedIn_SessionEstablished.json",
            mockedResponses = listOf(
                mockedRevokeTokenSignOutSuccessResponse
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

    private val globalSignedInSuccessCase = signedInSuccessCase.copy(
        description = "Test that global signOut while signed in returns complete with success",
        preConditions = signedInSuccessCase.preConditions.copy(
            mockedResponses = listOf(
                mockedGlobalSignOutSuccessResponse,
                mockedRevokeTokenSignOutSuccessResponse
            )
        ),
        api = signedInSuccessCase.api.copy(
            options = mapOf("globalSignOut" to true).toJsonElement()
        )
    )

    override val serializables: List<Any> = listOf(signedOutSuccessCase, signedInSuccessCase, globalSignedInSuccessCase)
}
