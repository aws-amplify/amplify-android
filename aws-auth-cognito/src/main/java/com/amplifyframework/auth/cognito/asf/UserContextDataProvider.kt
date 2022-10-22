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

package com.amplifyframework.auth.cognito.asf

import android.content.Context
import android.util.Base64
import android.util.Log
import org.json.JSONException
import org.json.JSONObject

/**
 * Provides the user context data that is sent to the server.
 * @param context android application context
 * @param poolId cognito userPoolId
 * @param clientId cognito appClientId used as secret key while generating signature
 */
class UserContextDataProvider(private val context: Context, private val poolId: String, private val clientId: String) {
    companion object {
        private val TAG = UserContextDataProvider::class.java.simpleName
        private const val VERSION_VALUE = "ANDROID20171114"

        private const val CONTEXT_DATA = "contextData"
        private const val USERNAME = "username"
        private const val USER_POOL_ID = "userPoolId"
        private const val TIMESTAMP_MILLI_SEC = "timestamp"
        private const val DATA_PAYLOAD = "payload"
        private const val VERSION_KEY = "version"
        private const val SIGNATURE = "signature"
    }

    private val timestamp = System.currentTimeMillis().toString()

    private lateinit var aggregator: ContextDataAggregator

    @Throws(JSONException::class)
    private fun getJsonPayload(contextData: Map<String, String?>, username: String, userPoolId: String): JSONObject {
        val payload = JSONObject()
        payload.put(CONTEXT_DATA, JSONObject(contextData))
        payload.put(USERNAME, username)
        payload.put(USER_POOL_ID, userPoolId)
        payload.put(TIMESTAMP_MILLI_SEC, timestamp)
        return payload
    }

    @Throws(JSONException::class)
    private fun getJsonResponse(payload: String, signature: String): JSONObject {
        val jsonResponse = JSONObject()
        jsonResponse.put(DATA_PAYLOAD, payload)
        jsonResponse.put(SIGNATURE, signature)
        jsonResponse.put(VERSION_KEY, VERSION_VALUE)
        return jsonResponse
    }

    private fun getEncodedResponse(jsonResponse: JSONObject): String {
        val bytes = jsonResponse.toString().toByteArray()
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }

    /**
     * Gets aggregated user context data, adds signature to it and provides
     * it in Base64 encoded form. Final data is JSON object with 'signature' and
     * 'payload'. Payload is a JSON object that contains 'username',
     * 'userPoolId', 'timestamp' and 'contextData'.
     * @param username username for the user
     * @param deviceId randomly generated deviceId
     * @return base64 encoded userContextData
     */
    fun getEncodedContextData(username: String, deviceId: String) = try {
        aggregator = lazy { ContextDataAggregator(deviceId) }.value
        val contextData = aggregator.getAggregatedData(context)
        val payload = getJsonPayload(contextData, username, poolId)
        val payloadString = payload.toString()
        val signature = SignatureGenerator.getSignature(payloadString, clientId, VERSION_VALUE)
        val jsonResponse = getJsonResponse(payloadString, signature)
        getEncodedResponse(jsonResponse)
    } catch (e: Exception) {
        Log.e(TAG, "Exception in creating JSON from context data")
        null
    }
}
