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

package com.amplifyframework.testutils

import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Base class for test classes that should run each test for Gen1 and Gen2 configurations.
 *
 * Usage:
 * Extend this class and add a parameter to your test class constructor. This will run
 * each test in your class twice, once for each config type.
 *
 * class ConfigTest(private val configType: ConfigType) : DualConfigTestBase() {
 *     @Test
 *     fun myTest() {
 *        val configuration = when(configType) {
 *            Gen1 -> ...
 *            Gen2 -> ...
 *        }
 *     }
 * }
 */
@RunWith(Parameterized::class)
abstract class DualConfigTestBase {

    enum class ConfigType {
        Gen1,
        Gen2
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(ConfigType.Gen1),
                arrayOf(ConfigType.Gen2)
            )
        }
    }
}
