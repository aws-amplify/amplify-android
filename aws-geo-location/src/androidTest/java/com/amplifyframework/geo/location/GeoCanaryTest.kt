/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.geo.location

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import com.amplifyframework.AmplifyException
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.core.Amplify
import com.amplifyframework.geo.models.Coordinates
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert
import org.junit.Assert.fail
import org.junit.BeforeClass
import org.junit.Test

class GeoCanaryTest {
    companion object {
        private const val TIMEOUT_S = 20L
        private val TAG = GeoCanaryTest::class.simpleName

        @BeforeClass
        @JvmStatic
        fun setup() {
            try {
                Amplify.addPlugin(AWSCognitoAuthPlugin())
                Amplify.addPlugin(AWSLocationGeoPlugin())
                Amplify.configure(ApplicationProvider.getApplicationContext())
            } catch (error: AmplifyException) {
                Log.e(TAG, "Could not initialize Amplify", error)
            }
        }
    }

    @After
    fun tearDown() {
        signOutFromCognito()
    }

    @Test
    fun searchByText() {
        val latch = CountDownLatch(1)
        signInWithCognito()
        val searchQuery = "Amazon Go"
        try {
            Amplify.Geo.searchByText(
                searchQuery,
                {
                    for (place in it.places) {
                        Log.i(TAG, place.toString())
                    }
                    latch.countDown()
                },
                { fail("Failed to search for $searchQuery: $it") }
            )
        } catch (e: Exception) {
            fail(e.toString())
        }
        Assert.assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    @Test
    fun searchByCoordinates() {
        val latch = CountDownLatch(1)
        signInWithCognito()
        val position = Coordinates(47.6153, -122.3384)
        try {
            Amplify.Geo.searchByCoordinates(
                position,
                {
                    for (place in it.places) {
                        Log.i(TAG, place.toString())
                    }
                    latch.countDown()
                },
                { fail("Failed to reverse geocode $position: $it") }
            )
        } catch (e: Exception) {
            fail(e.toString())
        }
        Assert.assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
    }

    private fun signInWithCognito() {
        val latch = CountDownLatch(1)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val (username, password) = Credentials.load(context)
        Amplify.Auth.signIn(
            username,
            password,
            { latch.countDown() },
            { Log.e(TAG, "Failed to sign in", it) }
        )
        latch.await(TIMEOUT_S, TimeUnit.SECONDS)
    }

    private fun signOutFromCognito() {
        val latch = CountDownLatch(1)
        Amplify.Auth.signOut {
            latch.countDown()
        }
        latch.await(TIMEOUT_S, TimeUnit.SECONDS)
    }
}
