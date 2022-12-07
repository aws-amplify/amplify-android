/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.kotlin.core

import android.content.Context
import com.amplifyframework.AmplifyException
import com.amplifyframework.analytics.AnalyticsCategory
import com.amplifyframework.core.Amplify as delegate
import com.amplifyframework.core.AmplifyConfiguration
import com.amplifyframework.core.plugin.Plugin
import com.amplifyframework.kotlin.api.KotlinApiFacade
import com.amplifyframework.kotlin.auth.KotlinAuthFacade
import com.amplifyframework.kotlin.datastore.KotlinDataStoreFacade
import com.amplifyframework.kotlin.geo.KotlinGeoFacade
import com.amplifyframework.kotlin.hub.KotlinHubFacade
import com.amplifyframework.kotlin.predictions.KotlinPredictionsFacade
import com.amplifyframework.kotlin.storage.KotlinStorageFacade
import com.amplifyframework.logging.LoggingCategory

/**
 * A Kotlin-language facade to the Amplify framework.
 * This is not to be confused with the Java-language facade of the same name.
 * If you are using Kotlin, be sure to import com.amplifyframework.kotlin.core.Amplify
 * instead of com.amplifyframework.core.Amplify.
 */
@Suppress("unused")
class Amplify {
    companion object {
        val Analytics = AnalyticsCategory()
        val API = KotlinApiFacade()
        val Auth = KotlinAuthFacade()
        val Geo = KotlinGeoFacade()
        val Logging = LoggingCategory()
        val Storage = KotlinStorageFacade()
        val Hub = KotlinHubFacade()
        val DataStore = KotlinDataStoreFacade()
        val Predictions = KotlinPredictionsFacade()

        /**
         * Configures the Amplify framework, considering the configuration
         * values in your application's app/src/main/res/raw/amplifyconfiguration.json.
         * That configuration file is automatically added to your project by the
         * Amplify CLI.
         * You must call one of the configure() methods before using any Amplify category.
         * You must add plugins to the framework before calling configure().
         * configure() may only be called once per application process, and there is
         * currently no way to reconfigure Amplify once it has been called.
         * Subsequent attempts to configure Amplify will generate an AmplifyException.
         * @param context An Android Context
         */
        @Throws(AmplifyException::class)
        fun configure(context: Context) {
            delegate.configure(context)
        }

        /**
         * Configures the Amplify framework, using a provided configuration, and
         * *ignoring* any configuration that may existing in your project's
         * app/src/main/res/raw/amplifyconfiguration.json.
         * You must call one of the configure() methods before using any Amplify category.
         * You must add plugins to the framework before calling configure().
         * configure() may only be called once per application process, and there is
         * currently no way to reconfigure Amplify once it has been called.
         * Subsequent attempts to configure Amplify will generate an AmplifyException.
         * @param configuration Configuration for Amplify
         * @param context An Android context
         */
        @Throws(AmplifyException::class)
        fun configure(configuration: AmplifyConfiguration, context: Context) {
            delegate.configure(configuration, context)
        }

        /**
         * Add a plugin to the Amplify framework.
         * You must add or remove plugins before calling configure();
         * it is an error to try to add a plugin after calling configure().
         * @param plugin Plugin to add
         */
        @Throws(AmplifyException::class)
        fun <P : Plugin<*>> addPlugin(plugin: P) {
            delegate.addPlugin(plugin)
        }

        /**
         * Removes a plugin from the Amplify framework.
         * You may add and remove plugins before calling configure().
         * Once configure() has been called, it is an error to attempt removal of a plugin.
         * @param plugin A plugin to remove
         */
        @Throws(AmplifyException::class)
        fun <P : Plugin<*>> removePlugin(plugin: P) {
            delegate.removePlugin(plugin)
        }
    }
}
