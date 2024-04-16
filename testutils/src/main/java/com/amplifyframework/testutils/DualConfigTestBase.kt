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

import android.content.Context
import androidx.annotation.RawRes
import androidx.test.core.app.ApplicationProvider
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.AmplifyConfiguration
import com.amplifyframework.core.configuration.AmplifyOutputs
import com.amplifyframework.core.configuration.AmplifyOutputsData
import com.amplifyframework.core.plugin.Plugin
import org.junit.Assume.assumeTrue
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Base class for test classes that should run each test for Gen1 and Gen2 configurations.
 *
 * Usage:
 * Extend this class and add a parameter to your test class constructor. This will run
 * each test in your class twice, once for each config type.
 *
 * class ConfigTest(configType: ConfigType) : DualConfigTestBase(configType) {
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
abstract class DualConfigTestBase(protected val configType: ConfigType) {

    enum class ConfigType {
        Gen1,
        Gen2
    }

    // Skips a test if it's not running for Gen1
    protected fun assumeGen1() = assumeTrue(configType == ConfigType.Gen1)

    // Skips a test if it's not running for Gen2
    protected fun assumeGen2() = assumeTrue(configType == ConfigType.Gen2)

    // Configures Amplify with either the Gen1 or Gen2 config
    protected fun configureAmplify(
        @RawRes gen1ResourceId: Int? = null,
        @RawRes gen2ResourceId: Int? = null
    ) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        requireGen1 {
            val id = gen1ResourceId ?: Resources.getRawResourceId(context, "amplifyconfiguration")
            val configuration = AmplifyConfiguration.fromConfigFile(context, id)
            Amplify.configure(configuration, context)
        }
        requireGen2 {
            val id = gen2ResourceId ?: Resources.getRawResourceId(context, "amplify_outputs")
            Amplify.configure(AmplifyOutputs(id), context)
        }
    }

    protected fun configurePlugin(
        plugin: Plugin<*>,
        @RawRes gen1ResourceId: Int? = null,
        @RawRes gen2ResourceId: Int? = null
    ) {
        val context = ApplicationProvider.getApplicationContext<Context>()

        requireGen1 {
            val id = gen1ResourceId ?: Resources.getRawResourceId(context, "amplifyconfiguration")
            val configuration = AmplifyConfiguration.fromConfigFile(context, id)
            val json = configuration.forCategoryType(plugin.categoryType).getPluginConfig(plugin.pluginKey)
            plugin.configure(json, context)
        }

        requireGen2 {
            val id = gen2ResourceId ?: Resources.getRawResourceId(context, "amplify_outputs")
            val data = AmplifyOutputsData.deserialize(context, AmplifyOutputs(id))
            plugin.configure(data, context)
        }

        // Initialize the plugin
        plugin.initialize(context)
    }

    // Runs the given block if this test is for gen1 config
    protected fun requireGen1(block: () -> Unit) {
        if (configType == ConfigType.Gen1) block()
    }

    // Runs the given block if this test is for gen2 config
    protected fun requireGen2(block: () -> Unit) {
        if (configType == ConfigType.Gen2) block()
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
