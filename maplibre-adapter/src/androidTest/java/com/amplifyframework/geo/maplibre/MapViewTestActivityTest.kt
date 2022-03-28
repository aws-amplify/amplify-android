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

package com.amplifyframework.geo.maplibre

import android.content.Context
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.launchActivity
import com.amplifyframework.geo.maplibre.view.ClusteringOptions
import com.amplifyframework.geo.maplibre.view.MapLibreView
import com.amplifyframework.testutils.sync.SynchronousAuth

import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Test

class MapViewTestActivityTest {

    /**
     * Tests that activity can successfully load a map instance.
     */
    @Test
    fun loadsMapSuccessfully() = runBlocking {
        val scenario = launchActivity<MapViewTestActivity>()
        val map = suspendCoroutine<MapboxMap> { continuation ->
            scenario.onActivity { activity ->
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
    fun enablesClusteringByDefault() = runBlocking {
        val scenario = launchActivity<MapViewTestActivity>()
        val mapStyle = suspendCoroutine<Style> { continuation ->
            scenario.onActivity { activity ->
                activity.mapView.addOnDidFailLoadingMapListener { error ->
                    continuation.resumeWithException(RuntimeException(error))
                }
                signInWithCognito(activity.auth)
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
    fun clusteringCanBeEnabledWithOptions() = runBlocking {
        val clusteringOptions = ClusteringOptions.builder().clusterColor(Color.RED).build()
        val scenario = launchActivity<MapViewTestActivity>()
        val mapStyle = suspendCoroutine<Style> { continuation ->
            scenario.onActivity { activity ->
                activity.mapView.addOnDidFailLoadingMapListener { error ->
                    continuation.resumeWithException(RuntimeException(error))
                }
                signInWithCognito(activity.auth)
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
    fun clusteringCanBeEnabledWithoutOptions() = runBlocking {
        val scenario = launchActivity<MapViewTestActivity>()
        val mapStyle = suspendCoroutine<Style> { continuation ->
            scenario.onActivity { activity ->
                activity.mapView.addOnDidFailLoadingMapListener { error ->
                    continuation.resumeWithException(RuntimeException(error))
                }
                signInWithCognito(activity.auth)
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
    fun clusteringCanBeDisabled(): Unit = runBlocking {
        val scenario = launchActivity<MapViewTestActivity>()
        val mapStyle = suspendCoroutine<Style> { continuation ->
            scenario.onActivity { activity ->
                activity.mapView.addOnDidFailLoadingMapListener { error ->
                    continuation.resumeWithException(RuntimeException(error))
                }
                signInWithCognito(activity.auth)
                activity.mapView.setClusterBehavior(false, null) {
                    activity.mapView.getStyle { _, style ->
                        continuation.resume(style)
                    }
                }
            }
        }
        this.launch(Dispatchers.Main) {
            assertNotNull(mapStyle)
            assertNull(mapStyle.getLayer(MapLibreView.CLUSTER_CIRCLE_LAYER_ID))
            assertNull(mapStyle.getLayer(MapLibreView.CLUSTER_NUMBER_LAYER_ID))
        }
    }

    private fun signInWithCognito(auth: SynchronousAuth?) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val (username, password) = Credentials.load(context)
        auth?.signIn(username, password)
    }
}
