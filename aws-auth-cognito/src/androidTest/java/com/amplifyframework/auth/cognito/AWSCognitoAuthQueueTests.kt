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
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amplifyframework.auth.exceptions.InvalidStateException
import com.amplifyframework.core.AmplifyConfiguration
import com.amplifyframework.core.category.CategoryConfiguration
import com.amplifyframework.core.category.CategoryType
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AWSCognitoAuthQueueTests {

    /**
     * This test was written to verify that any call made directly after a configure call will wait until the
     * auth plugin's state machine is in a settled state
     */
    @Test
    fun customer_auth_calls_wait_until_configured() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = AmplifyConfiguration.fromConfigFile(context)
        val authConfig: CategoryConfiguration = config.forCategoryType(CategoryType.AUTH)
        val authConfigJson = authConfig.getPluginConfig("awsCognitoAuthPlugin")
        val latch = CountDownLatch(1)
        var callSuccess = false

        val authPlugin = AWSCognitoAuthPlugin()
        authPlugin.configure(authConfigJson, context)
        authPlugin.fetchAuthSession(
            {
                callSuccess = true
                latch.countDown()
            },
            {
                callSuccess = it !is InvalidStateException
                latch.countDown()
            }
        )

        latch.await(10, TimeUnit.SECONDS)

        assertTrue(
            "Auth call returned InvalidStateException, indicating it likely didn't wait for a settled state",
            callSuccess
        )
    }
}
