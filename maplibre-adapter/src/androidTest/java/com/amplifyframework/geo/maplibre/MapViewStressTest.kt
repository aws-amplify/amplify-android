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

package com.amplifyframework.geo.maplibre

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.amplifyframework.geo.GeoCategory
import com.amplifyframework.geo.location.AWSLocationGeoPlugin
import com.amplifyframework.testutils.sync.SynchronousGeo
import com.amplifyframework.testutils.sync.TestCategory
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MapViewStressTest {
    @get:Rule
    var rule = ActivityScenarioRule(MapViewTestActivity::class.java)
    private var geo: SynchronousGeo? = null

    /**
     * Set up test categories to be used for testing.
     */
    @Before
    fun setUpBeforeTest() {
        val geoPlugin = AWSLocationGeoPlugin()
        val geoCategory = TestCategory.forPlugin(geoPlugin) as GeoCategory
        geo = SynchronousGeo.delegatingTo(geoCategory)
    }

    /**
     * Calls mapView.setStyle 50 times
     */
    @Test
    fun testMultipleSetStyle() = runBlockingSignedIn(rule) {
        repeat(50) {
            val mapStyle = suspendCoroutine { continuation ->
                rule.scenario.onActivity { activity ->
                    activity.mapView.addOnDidFailLoadingMapListener { error ->
                        continuation.resumeWithException(RuntimeException(error))
                    }
                    activity.mapView.setStyle { style ->
                        continuation.resume(style)
                    }
                }
            }
            Assert.assertNotNull(mapStyle)
        }
    }

    private fun <T> runBlockingSignedIn(
        rule: ActivityScenarioRule<MapViewTestActivity>,
        block: suspend CoroutineScope.() -> T
    ): T {
        return runBlocking(Dispatchers.Main) {
            rule.scenario.onActivity {
                signOutFromCognito() // first sign out to ensure we are in clean state
                signInWithCognito()
            }
            block()
        }
    }

    private fun signInWithCognito() {
        val (username, password) = Credentials.load(ApplicationProvider.getApplicationContext())
        val result = AmplifyWrapper.auth.signIn(username, password)
        println("SignIn complete: ${result.isSignedIn}")
    }

    private fun signOutFromCognito() {
        AmplifyWrapper.auth.signOut()
    }
}
