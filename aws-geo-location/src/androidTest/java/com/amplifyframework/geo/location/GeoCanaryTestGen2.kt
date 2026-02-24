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
import com.amplifyframework.core.configuration.AmplifyOutputs
import com.amplifyframework.geo.location.test.R
import com.amplifyframework.geo.models.Coordinates
import com.amplifyframework.testutils.rules.CanaryTestRule
import com.amplifyframework.testutils.sync.SynchronousAuth
import com.amplifyframework.testutils.sync.SynchronousGeo
import org.junit.After
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test

class GeoCanaryTestGen2 {
    companion object {
        private const val TIMEOUT_S = 20L
        private val TAG = GeoCanaryTestGen2::class.simpleName

        @BeforeClass
        @JvmStatic
        fun setup() {
            try {
                Amplify.addPlugin(AWSCognitoAuthPlugin())
                Amplify.addPlugin(AWSLocationGeoPlugin())
                Amplify.configure(AmplifyOutputs(R.raw.amplify_outputs), ApplicationProvider.getApplicationContext())
            } catch (error: AmplifyException) {
                Log.e(TAG, "Could not initialize Amplify", error)
            }
        }
    }

    private val syncAuth = SynchronousAuth.delegatingToAmplify()
    private val syncGeo = SynchronousGeo.delegatingToAmplify()

    @After
    fun tearDown() {
        signOutFromCognito()
    }

    @get:Rule
    val testRule = CanaryTestRule()

    @Test
    fun searchByText() {
        signInWithCognito()
        val searchQuery = "Amazon Go"
        val result = syncGeo.searchByText(searchQuery)
        for (place in result.places) {
            Log.i(TAG, place.toString())
        }
    }

    @Test
    fun searchByCoordinates() {
        signInWithCognito()
        val position = Coordinates(47.6153, -122.3384)
        val result = syncGeo.searchByCoordinates(position)
        for (place in result.places) {
            Log.i(TAG, place.toString())
        }
    }

    private fun signInWithCognito() {
        // Ensure we're not already signed in
        signOutFromCognito()

        val context = ApplicationProvider.getApplicationContext<Context>()
        val (username, password) = Credentials.load(context)
        syncAuth.signIn(username, password)
    }

    private fun signOutFromCognito() {
        syncAuth.signOut()
    }
}
