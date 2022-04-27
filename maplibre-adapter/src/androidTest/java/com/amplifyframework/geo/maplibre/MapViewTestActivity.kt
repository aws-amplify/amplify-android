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

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.amplifyframework.auth.AuthCategory
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.geo.GeoCategory
import com.amplifyframework.geo.location.AWSLocationGeoPlugin
import com.amplifyframework.geo.maplibre.view.MapLibreView
import com.amplifyframework.testutils.sync.SynchronousAuth
import com.amplifyframework.testutils.sync.TestCategory

/**
 * Activity that initializes MapLibre SDK with adapter on create.
 */
class MapViewTestActivity : AppCompatActivity() {
    internal var auth: SynchronousAuth? = null

    private val geo: GeoCategory by lazy {
        val authCategory = TestCategory.forPlugin(AWSCognitoAuthPlugin()) as AuthCategory
        auth = SynchronousAuth.delegatingTo(authCategory)
        TestCategory.forPlugin(AWSLocationGeoPlugin(authProvider = authCategory)) as GeoCategory
    }

    internal val mapView: MapLibreView by lazy {
        MapLibreView(context = applicationContext, geo = geo)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mapView)
    }
}
