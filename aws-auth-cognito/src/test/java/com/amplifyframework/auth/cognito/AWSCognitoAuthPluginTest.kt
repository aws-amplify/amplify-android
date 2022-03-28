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

package com.amplifyframework.auth.cognito

import android.content.Context
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.amplifyframework.AmplifyException
import com.amplifyframework.auth.AuthCategory
import com.amplifyframework.auth.AuthCategoryConfiguration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertTrue
import org.json.JSONException
import org.json.JSONObject
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AWSCognitoAuthPluginTest {

    private val PLUGIN_KEY = "awsCognitoAuthPlugin"

    private lateinit var authCategory: AuthCategory

    @Before
    @Throws(AmplifyException::class, JSONException::class)
    fun setup() {
        authCategory = AuthCategory()
        authCategory.addPlugin(AWSCognitoAuthPlugin())

        val context = getApplicationContext<Context>()
        val userPoolJSON = JSONObject(
            "{\n" +
                "   \"Default\": {\n" +
                "       \"PoolId\": \"us-east-2_xxxxxxxxx\",\n" +
                "       \"AppClientId\": \"xxxxxxxxxxxxxxxxxxxxxxxxxx\",\n" +
                "       \"Region\": \"us-east-2\"\n" +
                "   }\n" +
                "}"
        )
        val pluginConfig = JSONObject().put("CognitoUserPool", userPoolJSON)
        val json = JSONObject().put(
            "plugins",
            JSONObject().put(PLUGIN_KEY, pluginConfig)
        )

        val authConfig = AuthCategoryConfiguration()
        authConfig.populateFromJSON(json)
        authCategory.configure(authConfig, context)
        authCategory.initialize(context)
    }

    @Test
    @Ignore("fails, unable to load CRT")
    fun signIn() {
        val testLatch = CountDownLatch(1)
        authCategory.signIn(
            "username",
            "Password123",
            { testLatch.countDown() },
            {}
        )
        assertTrue { testLatch.await(5, TimeUnit.SECONDS) }
    }
}
