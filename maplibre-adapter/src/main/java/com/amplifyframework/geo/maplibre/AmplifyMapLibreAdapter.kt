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
import androidx.annotation.VisibleForTesting

import com.amazonaws.auth.AWSCredentialsProvider
import com.amplifyframework.auth.AuthCategory
import com.amplifyframework.core.Amplify
import com.amplifyframework.geo.maplibre.http.AWS4SigningInterceptor

import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.WellKnownTileServer
import com.mapbox.mapboxsdk.module.http.HttpRequestUtil
import okhttp3.OkHttpClient

/**
 * Entry point to initialize MapLibre instance and configure it to be
 * authorized to make requests directly to Amazon Location Service.
 */
object AmplifyMapLibreAdapter {
    private const val COGNITO_AUTH_PLUGIN_KEY = "awsCognitoAuthPlugin"
    private const val GEO_SERVICE_NAME = "geo"

    @VisibleForTesting internal var auth: AuthCategory = Amplify.Auth

    /**
     * Instantiates MapLibre SDK and attaches AWS Signature V4 signer.
     *
     * @param context Android context which holds or is an application context
     * @return the single instance of Mapbox
     */
    fun getInstance(context: Context): Mapbox {
        return synchronized(this) {
            val instance = Mapbox.getInstance(context, null, WellKnownTileServer.Mapbox)
            val interceptor = AWS4SigningInterceptor(credentialsProvider(), GEO_SERVICE_NAME)
            val client = OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build()
            HttpRequestUtil.setOkHttpClient(client)
            instance
        }
    }

    private fun credentialsProvider(): AWSCredentialsProvider {
        val authPlugin = auth.getPlugin(COGNITO_AUTH_PLUGIN_KEY)
        return authPlugin.escapeHatch as AWSCredentialsProvider
    }
}