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
import com.amplifyframework.auth.AuthCategory
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.geo.GeoCategory
import com.amplifyframework.geo.location.AWSLocationGeoPlugin
import com.amplifyframework.testutils.sync.SynchronousAuth
import com.amplifyframework.testutils.sync.SynchronousGeo
import com.amplifyframework.testutils.sync.TestCategory
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MapViewStressTest {
    @get:Rule
    var rule = ActivityScenarioRule(MapViewTestActivity::class.java)

    private lateinit var geoCategory: GeoCategory
    private lateinit var geo: SynchronousGeo
    private lateinit var auth: SynchronousAuth

    /**
     * Set up test categories to be used for testing.
     */
    @Before
    fun setUpBeforeTest() {
        val authPlugin = AWSCognitoAuthPlugin()
        val authCategory = TestCategory.forPlugin(authPlugin) as AuthCategory
        val geoPlugin = AWSLocationGeoPlugin(authCategory = authCategory)
        geoCategory = TestCategory.forPlugin(geoPlugin) as GeoCategory
        auth = SynchronousAuth.delegatingTo(authPlugin)
        geo = SynchronousGeo.delegatingTo(geoCategory)
        signInWithCognito()
    }

    @After
    fun tearDown() {
        signOutFromCognito()
    }

    /**
     * Calls mapView.setStyle 50 times
     */
    @Test
    fun testMultipleSetStyle() = runBlockingWithConfiguredMapActivity(rule) {
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

    private fun <T> runBlockingWithConfiguredMapActivity(
        rule: ActivityScenarioRule<MapViewTestActivity>,
        block: suspend CoroutineScope.() -> T
    ): T {
        return runBlocking(Dispatchers.Main) {
            rule.scenario.onActivity {
                it.setMapView(geoCategory)
            }
            block()
        }
    }

    private fun signInWithCognito() {
        signOutFromCognito() // this ensures a previous test failure doesn't impact current test
        val (username, password) = Credentials.load(ApplicationProvider.getApplicationContext())
        val result = auth.signIn(username, password)
        println("SignIn complete: ${result.isSignedIn}")
    }

    private fun signOutFromCognito() {
        auth.signOut()
    }
}
