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

import aws.sdk.kotlin.services.location.model.BatchItemError
import aws.smithy.kotlin.runtime.ClientException
import aws.smithy.kotlin.runtime.ServiceException
import com.amplifyframework.AmplifyException
import com.amplifyframework.geo.GeoException
import java.io.IOException

/**
 * Utility object to provide helpful error messages for Geo operations.
 */
internal object Errors {
    fun mapsError(error: Throwable): GeoException {
        if (error is GeoException) {
            return error
        }
        val (message, suggestion) = when (error) {
            is UninitializedPropertyAccessException ->
                "AWSLocationGeoPlugin is not configured." to
                    "Please verify that Geo plugin has been properly configured."
            is NullPointerException ->
                "Plugin configuration is missing \"maps\" configuration." to
                    "Please verify that Geo plugin has been properly configured."
            is ServiceException ->
                "There was a problem with the data in the request." to
                    "Please verify that a valid map resource exists."
            is ClientException ->
                "Amplify failed to send a request to Amazon Location Service." to
                    "Please ensure that you have a stable internet connection."
            is IOException ->
                "Failed to read map style from the server response." to
                    AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION
            else ->
                "Unexpected error. Failed to get available maps." to
                    AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION
        }
        return GeoException(message, error, suggestion)
    }

    fun searchError(error: Throwable): GeoException {
        if (error is GeoException) {
            return error
        }
        val (message, suggestion) = when (error) {
            is UninitializedPropertyAccessException ->
                "AWSLocationGeoPlugin is not configured." to
                    "Please verify that Geo plugin has been properly configured."
            is NullPointerException ->
                "Plugin configuration is missing \"searchIndices\" configuration." to
                    "Please verify that Geo plugin has been properly configured."
            is ServiceException ->
                "There was a problem with the data in the request." to
                    "Please verify that a valid place index resource exists."
            is ClientException ->
                "Amplify failed to send a request to Amazon Location Service." to
                    "Please ensure that you have a stable internet connection."
            else ->
                "Unexpected error. Failed to get search place index." to
                    AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION
        }
        return GeoException(message, error, suggestion)
    }

    fun deviceTrackingError(error: Throwable): GeoException {
        if (error is GeoException) {
            return error
        }
        val (message, suggestion) = when (error) {
            is UninitializedPropertyAccessException ->
                "AWSLocationGeoPlugin is not configured." to
                        "Please verify that Geo plugin has been properly configured."
            is ServiceException ->
                "There was a problem with the data in the request." to
                        "Please verify that your provided location, tracker and id are correct."
            is ClientException ->
                "Amplify failed to send a request to Amazon Location Service." to
                        "Please ensure that you have a stable internet connection."
            else ->
                "Unexpected error. Failed to process location request." to
                        AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION
        }
        return GeoException(message, error, suggestion)
    }
}
