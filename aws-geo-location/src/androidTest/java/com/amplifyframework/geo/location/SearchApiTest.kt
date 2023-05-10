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
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.geo.GeoCategory
import com.amplifyframework.geo.GeoException
import com.amplifyframework.geo.models.Coordinates
import com.amplifyframework.geo.options.GeoSearchByCoordinatesOptions
import com.amplifyframework.geo.options.GeoSearchByTextOptions
import com.amplifyframework.geo.result.GeoSearchResult
import com.amplifyframework.testutils.sync.SynchronousAuth
import com.amplifyframework.testutils.sync.SynchronousGeo
import com.amplifyframework.testutils.sync.TestCategory
import java.util.UUID
import kotlin.random.Random.Default.nextDouble
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

/**
 * Tests various functionalities related to Search API in [AWSLocationGeoPlugin].
 */
class SearchApiTest {
    lateinit var geo: SynchronousGeo
    @Before
    fun setup() {
        val geoPlugin = AWSLocationGeoPlugin()
        val geoCategory = TestCategory.forPlugin(geoPlugin) as GeoCategory
        geo = SynchronousGeo.delegatingTo(geoCategory)
    }

    @After
    fun tearDown() {
        signOutFromCognito()
    }

    /**
     * Tests that user must be authenticated in order to fetch geocode from
     * Amazon Location Service by string query.
     *
     * @throws GeoException will be thrown due to service exception.
     */
    @Test(expected = GeoException::class)
    fun cannotSearchByTextWithoutAuth() {
        signOutFromCognito()
        val query = UUID.randomUUID().toString()
        // should not be authorized to look up place from Amazon Location Service
        geo.searchByText(query, GeoSearchByTextOptions.defaults())
    }

    /**
     * Tests that user must be authenticated in order to reverse lookup geocode
     * from given coordinates on Amazon Location Service places index.
     *
     * @throws GeoException will be thrown due to service exception.
     */
    @Test(expected = GeoException::class)
    fun cannotSearchByCoordinatesWithoutAuth() {
        signOutFromCognito()
        val coordinates = Coordinates(
            nextDouble(-90.0, 90.0),
            nextDouble(-180.0, 180.0)
        )
        // should not be authorized to look up place from Amazon Location Service
        geo.searchByCoordinates(coordinates, GeoSearchByCoordinatesOptions.defaults())
    }

    /**
     * Tests that location geocode can be fetched using text query from Amazon
     * Location Service's place index using [AWSLocationGeoPlugin.searchByText].
     *
     * Both fetched [GeoSearchResult] and [GeoSearchResult.places] are guaranteed
     * to be non-null. There is no guarantee that result is not empty (no match).
     */
    @Test
    fun searchByTextReturnsResult() {
        signInWithCognito()
        val query = UUID.randomUUID().toString()
        val result = geo?.searchByText(query, GeoSearchByTextOptions.defaults())
        Assert.assertNotNull(result)
        Assert.assertNotNull(result!!.places)
    }

    /**
     * Tests that location geocode can be fetched using exact coordinates from Amazon
     * Location Service's place index using [AWSLocationGeoPlugin.searchByCoordinates].
     *
     * Both fetched [GeoSearchResult] and [GeoSearchResult.places] are guaranteed
     * to be non-null. Searching by coordinates will always return at least one place.
     */
    @Test
    fun searchByCoordinatesReturnsNonEmptyResult() {
        signInWithCognito()
        val coordinates = Coordinates(
            nextDouble(-90.0, 90.0),
            nextDouble(-180.0, 180.0)
        )
        val result = geo?.searchByCoordinates(coordinates, GeoSearchByCoordinatesOptions.defaults())
        Assert.assertNotNull(result)
        Assert.assertNotNull(result!!.places)

        // Reverse lookup will always return at least one result
        Assert.assertFalse(result.places.isEmpty())

        // First entry is on top of originally queried coordinates (within 1km)
        val queried = result.places[0].geometry as Coordinates
        assertTrue(coordinates.centralAngle(queried) < 0.0002)
    }

    private fun signInWithCognito() {
        // Sign out before signing in. If a user is already signed in, there will be a SignedInException.
        signOutFromCognito()
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
