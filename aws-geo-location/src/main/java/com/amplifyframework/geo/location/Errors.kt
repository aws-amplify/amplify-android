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

package com.amplifyframework.geo.location

import com.amplifyframework.geo.GeoException
import java.lang.NullPointerException

/**
 * Utility object to provide helpful error messages for Geo operations.
 */
internal object Errors {
    fun mapsError(exception: Exception): GeoException {
        val message = when (exception) {
            is UninitializedPropertyAccessException -> "AWSLocationGeoPlugin is not configured."
            is NullPointerException -> "Plugin configuration is missing \"maps\" configuration."
            else -> "Unexpected error. Failed to get available maps."
        }
        return GeoException(message, exception, "Ensure that Geo plugin has been properly configured.")
    }
}