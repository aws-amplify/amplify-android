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
package com.amplifyframework.preference

import android.content.Context
import com.amplifyframework.AmplifyException
import com.amplifyframework.core.BuildConfig
import com.amplifyframework.core.configuration.AmplifyOutputsData
import com.amplifyframework.core.store.AmplifyV2KeyValueRepositoryProvider
import com.amplifyframework.core.store.KeyValueRepositoryProvider
import org.json.JSONObject

class AndroidPreferencePlugin(
    private val context: Context,
    private val keyValueRepositoryProvider: KeyValueRepositoryProvider = AmplifyV2KeyValueRepositoryProvider(context)
) : PreferencePlugin<Void?>() {

    override fun getKeyValueRepositoryProvider(): KeyValueRepositoryProvider {
        return keyValueRepositoryProvider
    }

    override fun getPluginKey(): String {
        return "AndroidPreferencePlugin"
    }

    @Throws(AmplifyException::class)
    override fun configure(pluginConfiguration: JSONObject, context: Context) {
    }

    @Throws(AmplifyException::class)
    override fun configure(configuration: AmplifyOutputsData, context: Context) {
        super.configure(configuration, context)
    }

    override fun getEscapeHatch(): Void? {
        return null
    }

    override fun getVersion(): String {
        return BuildConfig.VERSION_NAME
    }
}
