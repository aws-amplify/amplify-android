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
package com.amplifyframework.analytics.pinpoint.targeting.endpointProfile

import com.amplifyframework.analytics.pinpoint.internal.core.util.JSONBuilder
import com.amplifyframework.analytics.pinpoint.internal.core.util.JSONSerializable
import com.amplifyframework.core.Amplify
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

class EndpointProfileUser : JSONSerializable {
    private var userId: String? = null
    private var userAttributes: MutableMap<String, List<String>>? = null
    fun getUserId(): String? {
        return userId
    }

    fun setUserId(userId: String?) {
        this.userId = userId
    }

    fun getUserAttributes(): Map<String, List<String>>? {
        return userAttributes
    }

    fun setUserAttributes(userAttributes: MutableMap<String, List<String>>?) {
        this.userAttributes = userAttributes
    }

    fun addUserAttribute(key: String, value: List<String>): EndpointProfileUser {
        if (userAttributes == null) {
            userAttributes = ConcurrentHashMap()
        }
        userAttributes!![key] = value
        return this
    }

    override fun toJSONObject(): JSONObject {
        val builder = JSONBuilder(null)
        builder.withAttribute("UserId", userId)
        if (getUserAttributes() != null) {
            val attributesJson = JSONObject()
            for ((key, value) in getUserAttributes()!!) {
                try {
                    val array = JSONArray(value)
                    attributesJson.put(key, array)
                } catch (e: JSONException) {
                    // Do not log e due to potentially sensitive information
                    LOG.warn("Error serializing user attributes.")
                }
            }

            // If there are any attributes put then add the attributes to the structure
            if (attributesJson.length() > 0) {
                builder.withAttribute("UserAttributes", attributesJson)
            }
        }
        return builder.toJSONObject()
    }

    companion object {
        private val LOG = Amplify.Logging.forNamespace("amplify:aws-analytics-pinpoint")
    }
}