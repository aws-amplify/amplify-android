/*
 *  Copyright 2016-2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */

package com.amplifyframework.analytics.pinpoint.internal.core.idresolver

import android.content.Context
import android.content.SharedPreferences
import com.amplifyframework.analytics.pinpoint.internal.core.util.putString
import com.amplifyframework.core.Amplify
import java.util.*

/**
 * Uses Shared prefs to recall and store the unique ID
 *
 * @param preferences the shared preferences implementation
 */
internal class SharedPrefsUniqueIdService(
    preferences: SharedPreferences
) {
    private val preferences: SharedPreferences = preferences

    /**
     * Get the Id that may be stored in SharedPreferences
     *
     * @return the Id of Analytics
     */
    fun getUniqueId(): String {
        var uniqueId = getIdFromPreferences()
        if (uniqueId.isNullOrBlank()) {
            // an id doesn't exist for this pinpointContext, create one and persist it
            uniqueId = UUID.randomUUID().toString()
            storeUniqueId(uniqueId)
        }
        return uniqueId
    }

    private fun getIdFromPreferences(): String? {
        return if (legacyId !== "") {
            legacyId
        } else preferences.getString(UNIQUE_ID_KEY, null)
    }

    private val legacyId: String
        get() {
            val legacyPreferences = preferences
            val legacyId = legacyPreferences.getString(UNIQUE_ID_KEY, null)
            return legacyId ?: ""
        }

    private fun storeUniqueId(
        uniqueId: String
    ) {
        preferences.putString(SharedPrefsUniqueIdService.UNIQUE_ID_KEY, uniqueId);
    }

    companion object {
        private const val UNIQUE_ID_KEY = "UniqueId"
        private val LOG = Amplify.Logging.forNamespace("amplify:aws-analytics-pinpoint")
    }
}