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

package com.amplifyframework.testutils.featuretest.auth.generators.testcasegenerators

import com.amplifyframework.auth.cognito.result.AWSCognitoAuthSignOutResult
import com.amplifyframework.testutils.featuretest.API
import com.amplifyframework.testutils.featuretest.ExpectationShapes
import com.amplifyframework.testutils.featuretest.FeatureTestCase
import com.amplifyframework.testutils.featuretest.PreConditions
import com.amplifyframework.testutils.featuretest.ResponseType
import com.amplifyframework.testutils.featuretest.auth.AuthAPI
import com.amplifyframework.testutils.featuretest.auth.generators.SerializableProvider
import com.amplifyframework.testutils.featuretest.auth.generators.toJsonElement

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
