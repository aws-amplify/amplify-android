/*
 *
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
 *
 *
 */

package com.amplifyframework.geo.location

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import aws.sdk.kotlin.services.location.LocationClient
import aws.sdk.kotlin.services.location.model.BatchDeleteDevicePositionHistoryRequest
import aws.sdk.kotlin.services.location.model.ListDevicePositionsRequest
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.geo.GeoCategory
import com.amplifyframework.geo.GeoException
import com.amplifyframework.geo.models.GeoDevice
import com.amplifyframework.geo.models.GeoLocation
import com.amplifyframework.geo.options.GeoUpdateLocationOptions
import com.amplifyframework.testutils.sync.SynchronousAuth
import com.amplifyframework.testutils.sync.SynchronousGeo
import com.amplifyframework.testutils.sync.TestCategory
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random.Default.nextDouble

/**
 * Tests various functionalities related to Device Tracking API in [AWSLocationGeoPlugin].
 */
class DeviceTrackingApiTest {
    private lateinit var geo: SynchronousGeo
    private lateinit var escapeHatch: LocationClient

    @Before
    fun setup() {
        val geoPlugin = AWSLocationGeoPlugin()
        val geoCategory = TestCategory.forPlugin(geoPlugin) as GeoCategory
        geo = SynchronousGeo.delegatingTo(geoCategory)
        escapeHatch = geoPlugin.escapeHatch

        signOutFromCognito()
    }

    @Test
    fun testUpdateLocationWithoutTracker() {
        signInWithCognito()

        runBlocking {
            escapeHatch.batchDeleteDevicePositionHistory (
                BatchDeleteDevicePositionHistoryRequest.invoke {
                    deviceIds = listOf("id")
                    trackerName = "android_default_tracker"
                }
            )
        }

        val latitude = nextDouble()
        val longitude = nextDouble()
        geo.updateLocation(
            GeoDevice.createUncheckedId("id"),
            GeoLocation(latitude, longitude)
        )

        val positions = runBlocking {
            escapeHatch.listDevicePositions(
                ListDevicePositionsRequest.invoke {
                    trackerName = "android_default_tracker"
                }
            )
        }
        assert(positions.entries != null && positions.entries!!.size == 1)
        val position = positions.entries!![0].position!!
        assert(abs(latitude - position[1]) < .001)
        assert(abs(longitude - position[0]) < .001)
    }

    @Test(expected = GeoException::class)
    fun testUpdateLocationWithInvalidTracker() {
        signInWithCognito()

        // should throw an error because the tracker does not exist
        geo.updateLocation(
            GeoDevice.createUncheckedId("id"),
            GeoLocation(0.0, 0.0),
            GeoUpdateLocationOptions.builder().withTracker("invalid_tracker").build()
        )
    }

    @Test
    fun testUpdateLocationWithTracker() {
        signInWithCognito()

        geo.updateLocation(
            GeoDevice.createUncheckedId("id"),
            GeoLocation(0.0, 0.0),
            GeoUpdateLocationOptions.Builder().withTracker("android_tracker").build()
        )
    }

    @Test
    fun testUpdateLocationWithUserId() {
        signInWithCognito()

        // TODO: doesn't work due to ALS not supporting non-colon characters
//        geo.updateLocation(
//            GeoDevice.createIdTiedToUser(),
//            GeoLocation(0.0, 0.0),
//            GeoUpdateLocationOptions.Builder().withTracker("android_tracker").build()
//        )
    }

    @Test
    fun testUpdateLocationWithDeviceId() {
        signInWithCognito()

        geo.updateLocation(
            GeoDevice.createIdTiedToDevice(),
            GeoLocation(0.0, 0.0),
            GeoUpdateLocationOptions.Builder().withTracker("android_tracker").build()
        )
    }

    @Test
    fun testUpdateLocationWithUserAndDevice() {
        signInWithCognito()

        // TODO: doesn't work due to ALS not supporting non-colon characters
//        geo.updateLocation(
//            GeoDevice.createIdTiedToUserAndDevice(),
//            GeoLocation(0.0, 0.0),
//            GeoUpdateLocationOptions.Builder().withTracker("android_tracker").build()
//        )
    }

    private fun signInWithCognito() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val (username, password) = Credentials.load(context)
        auth.signIn(username, password)
    }

    private fun signOutFromCognito() {
        auth.signOut()
    }

    companion object {
        lateinit var auth: SynchronousAuth

        /**
         * Set up test categories to be used for testing.
         */
        @BeforeClass
        @JvmStatic
        fun setUp() {
            // Auth plugin uses default configuration
            auth =
                SynchronousAuth.delegatingToCognito(ApplicationProvider.getApplicationContext(), AWSCognitoAuthPlugin())
        }
    }
}
