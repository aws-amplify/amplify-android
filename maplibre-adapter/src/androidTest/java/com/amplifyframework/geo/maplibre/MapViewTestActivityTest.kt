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

import androidx.test.core.app.ActivityScenario.launch

import com.amplifyframework.auth.AuthCategory
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.testutils.sync.TestCategory

import com.mapbox.mapboxsdk.maps.MapboxMap
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.lang.RuntimeException
import kotlin.coroutines.*
import kotlinx.coroutines.runBlocking

class MapViewTestActivityTest {
    /**
     * Configure MapLibre Adapter's auth provider.
     */
    @Before
    fun setUpAuth() {
        MapLibreAdapter.auth = TestCategory.forPlugin(AWSCognitoAuthPlugin()) as AuthCategory
    }

    /**
     * Tests that activity can successfully load a map instance.
     */
    @Test
    fun loadsMapSuccessfully() = runBlocking {
        val scenario = launch(MapViewTestActivity::class.java)
        val map = suspendCoroutine<MapboxMap> { continuation ->
            scenario.onActivity { activity ->
                activity.mapView?.addOnDidFailLoadingMapListener { error ->
                    continuation.resumeWithException(RuntimeException(error))
                }
                activity.mapView?.getMapAsync { map ->
                    continuation.resume(map)
                }
            }
        }
        assertNotNull(map)
    }
}