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
import com.amplifyframework.auth.cognito.featuretest.PreConditions
import com.amplifyframework.auth.cognito.featuretest.ResponseType
import com.amplifyframework.auth.cognito.featuretest.generators.SerializableProvider
import com.amplifyframework.auth.cognito.featuretest.generators.toJsonElement
import com.amplifyframework.auth.cognito.result.AWSCognitoAuthSignOutResult

object SignOutTestCaseGenerator : SerializableProvider {

    val baseCase = FeatureTestCase(
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

    override val serializables: List<Any> = listOf(baseCase)
}
