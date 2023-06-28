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

import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.amplifyframework.geo.maplibre.view.ClusteringOptions
import com.amplifyframework.geo.maplibre.view.MapLibreView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class MapViewTestActivityTest {

    @get:Rule
    var rule = ActivityScenarioRule(MapViewTestActivity::class.java)

    /**
     * Tests that activity can successfully load a map instance.
     */
    @Test
    fun loadsMapSuccessfully() = runBlocking {
        val map = suspendCoroutine<MapboxMap> { continuation ->
            rule.scenario.onActivity { activity ->
                activity.mapView.addOnDidFailLoadingMapListener { error ->
                    continuation.resumeWithException(RuntimeException(error))
                }
                activity.mapView.getMapAsync { map ->
                    continuation.resume(map)
                }
            }
        }
        assertNotNull(map)
    }

    /**
     * Tests that clustering is enabled by default when setting the style for a map.
     */
    @Test
    fun enablesClusteringByDefault() = runBlockingSignedIn(rule) {
        val mapStyle = suspendCoroutine<Style> { continuation ->
            rule.scenario.onActivity { activity ->
                activity.mapView.addOnDidFailLoadingMapListener { error ->
                    continuation.resumeWithException(RuntimeException(error))
                }
                activity.mapView.setStyle { style ->
                    continuation.resume(style)
                }
            }
        }
        assertNotNull(mapStyle)
        assertNotNull(mapStyle.getLayer(MapLibreView.CLUSTER_CIRCLE_LAYER_ID))
        assertNotNull(mapStyle.getLayer(MapLibreView.CLUSTER_NUMBER_LAYER_ID))
    }

    /**
     * Tests that clustering can be enabled and clustering options passed in for the map.
     */
    @Test
    fun clusteringCanBeEnabledWithOptions() = runBlockingSignedIn(rule) {
        val clusteringOptions = ClusteringOptions.builder().clusterColor(Color.RED).build()
        val mapStyle = suspendCoroutine<Style> { continuation ->
            rule.scenario.onActivity { activity ->
                activity.mapView.addOnDidFailLoadingMapListener { error ->
                    continuation.resumeWithException(RuntimeException(error))
                }
                activity.mapView.setClusterBehavior(true, clusteringOptions) {
                    activity.mapView.getStyle { _, style ->
                        continuation.resume(style)
                    }
                }
            }
        }
        assertNotNull(mapStyle)
        assertNotNull(mapStyle.getLayer(MapLibreView.CLUSTER_CIRCLE_LAYER_ID))
        assertNotNull(mapStyle.getLayer(MapLibreView.CLUSTER_NUMBER_LAYER_ID))
    }

    /**
     * Tests that clustering can be enabled for the map without passing in clustering options.
     */
    @Test
    fun clusteringCanBeEnabledWithoutOptions() = runBlockingSignedIn(rule) {
        val mapStyle = suspendCoroutine<Style> { continuation ->
            rule.scenario.onActivity { activity ->
                activity.mapView.addOnDidFailLoadingMapListener { error ->
                    continuation.resumeWithException(RuntimeException(error))
                }
                activity.mapView.setClusterBehavior(true, null) {
                    activity.mapView.getStyle { _, style ->
                        continuation.resume(style)
                    }
                }
            }
        }
        assertNotNull(mapStyle)
        assertNotNull(mapStyle.getLayer(MapLibreView.CLUSTER_CIRCLE_LAYER_ID))
        assertNotNull(mapStyle.getLayer(MapLibreView.CLUSTER_NUMBER_LAYER_ID))
    }

    /**
     * Tests that clustering can be disabled for the map.
     */
    @Test
    fun clusteringCanBeDisabled() = runBlockingSignedIn(rule) {
        val mapStyle = suspendCoroutine<Style> { continuation ->
            rule.scenario.onActivity { activity ->
                activity.mapView.addOnDidFailLoadingMapListener { error ->
                    continuation.resumeWithException(RuntimeException(error))
                }
                activity.mapView.setClusterBehavior(false, null) {
                    activity.mapView.getStyle { _, style ->
                        continuation.resume(style)
                    }
                }
            }
        }
        assertNotNull(mapStyle)
        assertNull(mapStyle.getLayer(MapLibreView.CLUSTER_CIRCLE_LAYER_ID))
        assertNull(mapStyle.getLayer(MapLibreView.CLUSTER_NUMBER_LAYER_ID))
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
        println("SignIn complete: $result")
    }

    private fun signOutFromCognito() {
        AmplifyWrapper.auth.signOut()
    }
}
