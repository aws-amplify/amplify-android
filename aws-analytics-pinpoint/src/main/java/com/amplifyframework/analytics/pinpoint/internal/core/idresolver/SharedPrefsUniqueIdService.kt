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
import com.amplifyframework.analytics.pinpoint.internal.core.system.AndroidPreferences
import com.amplifyframework.analytics.pinpoint.internal.core.system.AndroidSystem
import com.amplifyframework.core.Amplify
import java.util.*

/**
 * Uses Shared prefs to recall and store the unique ID
 *
 * @param appId              used as the shared preferences file name
 * @param applicationContext the application pinpointContext.
 */
internal class SharedPrefsUniqueIdService(
    appId: String?,
    applicationContext: Context?
) {
    private val appId: String? = appId
    private val applicationContext: Context? = applicationContext

    /**
     * Get the Id based on the passed in pinpointContext
     *
     * @param context The Analytics pinpointContext to use when looking up the id
     * @return the Id of Analytics pinpointContext
     */
    fun getUniqueId(system: AndroidSystem): String {
        val prefs = system.getPreferences()
        if (prefs == null) {
            LOG.debug("Unable to generate unique id, AndroidSystem has not been fully initialized.")
            return ""
        }
        var uniqueId = getIdFromPreferences(prefs)
        if (uniqueId.isNullOrBlank()) {
            // an id doesn't exist for this pinpointContext, create one and persist it
            uniqueId = UUID.randomUUID().toString()
            storeUniqueId(prefs, uniqueId)
        }
        return uniqueId
    }

    private fun getIdFromPreferences(preferences: AndroidPreferences): String? {
        return if (legacyId !== "") {
            legacyId
        } else preferences.getString(UNIQUE_ID_KEY, null)
    }

    private val legacyId: String
        private get() {
            if (appId == null || applicationContext == null) {
                return ""
            }
            val legacyPreferences = applicationContext
                .getSharedPreferences(
                    appId,
                    Context.MODE_PRIVATE
                )
            val legacyId = legacyPreferences.getString(UNIQUE_ID_KEY, null)
            return legacyId ?: ""
        }

    private fun storeUniqueId(
        preferences: AndroidPreferences,
        uniqueId: String
    ) {
        try {
            preferences.putString(UNIQUE_ID_KEY, uniqueId)
        } catch (ex: Exception) {
            // Do not log ex due to potentially sensitive information
            LOG.error("There was an exception when trying to store the unique id into the Preferences.")
        }
    }

    companion object {
        protected const val UNIQUE_ID_KEY = "UniqueId"
        private val LOG = Amplify.Logging.forNamespace("amplify:aws-analytics-pinpoint")
    }
}