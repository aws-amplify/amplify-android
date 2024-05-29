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

package com.amplifyframework.auth.cognito.featuretest.generators

import com.amplifyframework.auth.cognito.featuretest.FeatureTestCase
import com.amplifyframework.auth.cognito.featuretest.generators.authstategenerators.AuthStateJsonGenerator
import com.amplifyframework.auth.cognito.featuretest.generators.testcasegenerators.FetchAuthSessionTestCaseGenerator
import com.amplifyframework.statemachine.codegen.states.AuthState
import org.junit.Ignore
import org.junit.Test

interface SerializableProvider {
    val serializables: List<Any>
}

/**
 * Top level generator for generating Json and writing to the destination directory
 */
class JsonGenerator {
    private val providers: List<SerializableProvider> = listOf(
        AuthStateJsonGenerator,
//        ResetPasswordTestCaseGenerator,
//        SignUpTestCaseGenerator,
//        SignInTestCaseGenerator,
//        SignOutTestCaseGenerator,
//        ConfirmSignInTestCaseGenerator,
//        DeleteUserTestCaseGenerator,
        FetchAuthSessionTestCaseGenerator,
//        RememberDeviceTestCaseGenerator,
//        ForgetDeviceTestCaseGenerator,
//        FetchDevicesTestCaseGenerator,
//        FetchUserAttributesTestCaseGenerator,
    )

    @Ignore("Uncomment and run to clean feature test directory")
    @Test
    fun clean() {
        cleanDirectory()
    }

    @Ignore("Uncomment and run to clean feature test directory as well as generate json for feature tests")
    @Test
    fun cleanAndGenerate() {
        cleanDirectory()
        generateJson()
    }

    @Ignore("Uncomment and run to generate json for feature tests")
    @Test
    fun generate() {
        generateJson()
    }

    private fun generateJson() {
        providers.forEach { provider ->
            provider.serializables.forEach {
                when (it) {
                    is AuthState -> it.exportJson()
                    is FeatureTestCase -> it.exportJson()
                    else -> {
                        println("Generation of ${it.javaClass.kotlin} is not supported!")
                    }
                }
            }
        }
    }
}
