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
package com.amplifyframework.analytics.pinpoint.internal.core.util

import com.amplifyframework.core.Amplify
import org.json.JSONException
import org.json.JSONObject

class JSONBuilder(component: Any?) : JSONSerializable {
    private val json = JSONObject()
    fun withAttribute(key: String?, value: Any?): JSONBuilder {
        val jsonValue = (if (value is JSONSerializable) value.toJSONObject() else value)!!
        try {
            json.putOpt(key, jsonValue)
        } catch (e: JSONException) {
            LOG.warn("error parsing json", e)
        }
        return this
    }

    override fun toJSONObject(): JSONObject {
        return json
    }

    override fun toString(): String {
        return try {
            json.toString(INDENTATION)
        } catch (e: JSONException) {
            json.toString()
        }
    }

    companion object {
        private val LOG = Amplify.Logging.forNamespace("amplify:aws-analytics-pinpoint")
        private const val INDENTATION = 4
    }

    init {
        if (null != component) {
            withAttribute("class", component.javaClass.name)
            withAttribute("hashCode", Integer.toHexString(component.hashCode()))
        }
    }
}