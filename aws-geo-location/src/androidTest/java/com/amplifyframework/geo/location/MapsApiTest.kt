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

package com.amplifyframework.geo.location

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.amplifyframework.auth.AuthCategory
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.geo.GeoCategory
import com.amplifyframework.geo.GeoException
import com.amplifyframework.geo.options.GetMapStyleDescriptorOptions
import com.amplifyframework.testutils.sync.SynchronousAuth
import com.amplifyframework.testutils.sync.SynchronousGeo
import com.amplifyframework.testutils.sync.TestCategory
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests various functionalities related to Maps API in [AWSLocationGeoPlugin].
 */
class MapsApiTest {
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
        val geoCategory = TestCategory.forPlugin(geoPlugin) as GeoCategory
        auth = SynchronousAuth.delegatingTo(authPlugin)
        geo = SynchronousGeo.delegatingTo(geoCategory)
    }

    @After
    fun tearDown() {
        signOutFromCognito()
    }

    /**
     * Tests that default map resource's style document can be fetched from
     * Amazon Location Service using [AWSLocationGeoPlugin.getMapStyleDescriptor].
     *
     * Fetched document must follow the specifications for Mapbox Style format.
     * Both "layers" and "sources" are critical information required for rendering
     * a map, so assert that both fields exist in the document.
     */
    @Test
    fun styleDescriptorLoadsProperly() {
        signInWithCognito()
        val style = geo.getMapStyleDescriptor(GetMapStyleDescriptorOptions.defaults())
        assertNotNull(style)
        assertNotNull(style?.json)

        // assert that style document is aligned with specs
        // https://docs.mapbox.com/mapbox-gl-js/style-spec/
        val json = JSONObject(style!!.json)
        assertTrue(json.has("layers"))
        assertTrue(json.has("sources"))
    }

    /**
     * Tests that user must be authenticated in order to fetch map resource from
     * Amazon Location Service.
     *
     * @throws GeoException will be thrown due to service exception.
     */
    @Test(expected = GeoException::class)
    fun cannotFetchStyleWithoutAuth() {
        // should not be authorized to fetch map resource from Amazon Location Service
        geo.getMapStyleDescriptor(GetMapStyleDescriptorOptions.defaults())
    }

    private fun signInWithCognito() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val (username, password) = Credentials.load(context)
        signOutFromCognito() // this ensures a previous test failure doesn't impact current test
        auth.signIn(username, password)
    }

    private fun signOutFromCognito() {
        auth.signOut()
    }
}
