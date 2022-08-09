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
package com.amplifyframework.analytics.pinpoint.targeting

import android.content.Context
import android.content.SharedPreferences
import aws.sdk.kotlin.services.pinpoint.PinpointClient
import aws.sdk.kotlin.services.pinpoint.model.EndpointDemographic
import aws.sdk.kotlin.services.pinpoint.model.EndpointLocation
import aws.sdk.kotlin.services.pinpoint.model.EndpointRequest
import aws.sdk.kotlin.services.pinpoint.model.EndpointUser
import aws.sdk.kotlin.services.pinpoint.model.PinpointException
import aws.sdk.kotlin.services.pinpoint.model.UpdateEndpointRequest
import com.amplifyframework.analytics.pinpoint.internal.core.idresolver.SharedPrefsUniqueIdService
import com.amplifyframework.analytics.pinpoint.internal.core.util.DateUtil.formatISO8601Date
import com.amplifyframework.analytics.pinpoint.internal.core.util.putString
import com.amplifyframework.analytics.pinpoint.models.AndroidAppDetails
import com.amplifyframework.analytics.pinpoint.models.AndroidDeviceDetails
import com.amplifyframework.analytics.pinpoint.targeting.endpointProfile.EndpointProfile
import com.amplifyframework.analytics.pinpoint.targeting.notification.PinpointNotificationClient
import com.amplifyframework.core.Amplify
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.collections.ArrayList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject

internal class TargetingClient(
    private val pinpointClient: PinpointClient,
    pinpointNotificationClient: PinpointNotificationClient,
    idService: SharedPrefsUniqueIdService,
    private val prefs: SharedPreferences,
    appDetails: AndroidAppDetails,
    deviceDetails: AndroidDeviceDetails,
    applicationContext: Context,
) {
    private val endpointProfile: EndpointProfile =
        EndpointProfile(pinpointNotificationClient, idService, appDetails, deviceDetails, applicationContext)
    private val globalAttributes: MutableMap<String, List<String>>
    private val globalMetrics: MutableMap<String, Double>

    init {
        globalAttributes = loadAttributes()
        globalMetrics = loadMetrics()
    }

    /**
     * Returns the device endpoint profile.
     * TargetingClient attributes and Metrics are added to the endpoint profile.
     *
     * @return The current device endpoint profile
     */
    fun currentEndpoint(): EndpointProfile {
        // Add global attributes.
        if (globalAttributes.isNotEmpty()) {
            for ((key, value) in globalAttributes) {
                endpointProfile.addAttribute(key, value)
            }
        }
        if (globalMetrics.isNotEmpty()) {
            for ((key, value) in globalMetrics) {
                endpointProfile.addMetric(key, value)
            }
        }
        return endpointProfile
    }

    /**
     * Register the current endpoint with the Pinpoint service.
     * TargetingClient attributes and Metrics are added to the endpoint profile.
     */
    fun updateEndpointProfile() {
        executeUpdate(currentEndpoint())
    }

    /**
     * Register the provided endpoint with the Pinpoint service.
     * TargetingClient attributes and Metrics are added to the endpoint profile.
     *
     * @param endpointProfile An instance of an EndpointProfile to be updated
     */
    fun updateEndpointProfile(endpointProfile: EndpointProfile) {
        // Add global  attributes.
        for ((key, value) in globalAttributes) {
            endpointProfile.addAttribute(key, value)
        }
        for ((key, value) in globalMetrics) {
            endpointProfile.addMetric(key, value)
        }
        executeUpdate(endpointProfile)
    }

    private fun executeUpdate(endpointProfile: EndpointProfile?) {
        if (endpointProfile == null) {
            LOG.error("EndpointProfile is null, failed to update profile.")
            return
        }
        val demographic = EndpointDemographic.invoke {
            this.appVersion = endpointProfile.demographic.getAppVersion()
            this.locale = endpointProfile.demographic.getLocale().toString()
            this.timezone = endpointProfile.demographic.timezone
            this.make = endpointProfile.demographic.getMake()
            this.model = endpointProfile.demographic.model
            this.platform = endpointProfile.demographic.platform
            this.platformVersion = endpointProfile.demographic.platformVersion
        }
        val location = EndpointLocation.invoke {
            this.latitude = endpointProfile.location.latitude
            this.longitude = endpointProfile.location.longitude
            this.postalCode = endpointProfile.location.postalCode
            this.city = endpointProfile.location.city
            this.region = endpointProfile.location.region
            this.country = endpointProfile.location.getCountry()
        }
        val user: EndpointUser?
        if (endpointProfile.user.getUserId() == null) {
            user = null
        } else {
            user = EndpointUser.invoke {
                this.userId = endpointProfile.user.getUserId()
                this.userAttributes = endpointProfile.user.getUserAttributes()
            }
        }
        val endpointRequest = EndpointRequest.invoke {
            this.channelType = endpointProfile.channelType
            this.address = endpointProfile.address
            this.location = location
            this.demographic = demographic
            this.effectiveDate = formatISO8601Date(
                Date(endpointProfile.effectiveDate)
            )
            this.optOut = endpointProfile.optOut
            this.attributes = endpointProfile.allAttributes
            this.metrics = endpointProfile.allMetrics
            this.user = user
        }
        val updateEndpointRequest =
            UpdateEndpointRequest.invoke {
                this.applicationId = endpointProfile.applicationId
                this.endpointId = endpointProfile.endpointId
                this.endpointRequest = endpointRequest
            }

        val coroutineDispatcher = Dispatchers.Default
        val coroutineScope = CoroutineScope(coroutineDispatcher)
        val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        coroutineScope.launch(dispatcher) {
            try {
                LOG.info("Updating EndpointProfile.")
                coroutineScope.launch {
                    pinpointClient.updateEndpoint(updateEndpointRequest)
                }
                LOG.info("EndpointProfile updated successfully.")
            } catch (e: PinpointException) {
                LOG.error("PinpointException occurred during endpoint update:", e)
            }
        }
    }

    private fun saveAttributes() {
        val jsonObject = JSONObject(globalAttributes as MutableMap<*, *>)
        val jsonString = jsonObject.toString()
        prefs.putString(CUSTOM_ATTRIBUTES_KEY, jsonString)
    }

    private fun loadAttributes(): MutableMap<String, List<String>> {
        val outputMap: MutableMap<String, List<String>> = ConcurrentHashMap()
        val jsonString: String? = prefs.getString(CUSTOM_ATTRIBUTES_KEY, null)
        if (jsonString.isNullOrBlank()) {
            return outputMap
        }
        try {
            val jsonObject = JSONObject(jsonString)
            val keysItr = jsonObject.keys()
            while (keysItr.hasNext()) {
                val key = keysItr.next()
                val jsonArray = jsonObject.getJSONArray(key)
                val listValues: MutableList<String> = ArrayList()
                for (i in 0 until jsonArray.length()) {
                    listValues.add(jsonArray.getString(i))
                }
                outputMap[key] = listValues
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return outputMap
    }

    private fun saveMetrics() {
        val jsonObject = JSONObject(globalMetrics as MutableMap<*, *>)
        val jsonString = jsonObject.toString()
        prefs.putString(CUSTOM_METRICS_KEY, jsonString)
    }

    private fun loadMetrics(): MutableMap<String, Double> {
        val outputMap: MutableMap<String, Double> = ConcurrentHashMap()
        val jsonString: String? = prefs.getString(CUSTOM_METRICS_KEY, null)
        if (jsonString.isNullOrBlank()) {
            return outputMap
        }
        try {
            val jsonObject = JSONObject(jsonString)
            val keysItr = jsonObject.keys()
            while (keysItr.hasNext()) {
                val key = keysItr.next()
                val value = jsonObject.getDouble(key)
                outputMap[key] = value
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return outputMap
    }

    /**
     * Adds the specified attribute to the current endpoint profile generated by this client.
     * Note: The maximum allowed attributes/metrics is 250. Attempts to add more may be ignored
     *
     * @param attributeName   the name of the  attribute to add
     * @param attributeValues the value of the  attribute
     */
    fun addAttribute(attributeName: String?, attributeValues: List<String>?) {
        if (attributeName == null) {
            LOG.debug("Null attribute name provided to addGlobalAttribute.")
            return
        }
        if (attributeValues == null) {
            LOG.debug("Null attribute values provided to addGlobalAttribute.")
            return
        }
        globalAttributes[attributeName] = attributeValues
        saveAttributes()
    }

    /**
     * Removes the specified attribute. All subsequently created events will no
     * longer have this global attribute. from the current endpoint profile generated by this client.
     *
     * @param attributeName the name of the attribute to remove
     */
    fun removeAttribute(attributeName: String?) {
        if (attributeName == null) {
            LOG.warn("Null attribute name provided to removeGlobalAttribute.")
            return
        }
        endpointProfile.addAttribute(attributeName, null)
        globalAttributes.remove(attributeName)
        saveAttributes()
    }

    /**
     * Adds the specified metric to the current endpoint profile generated by this client. Note: The
     * maximum allowed attributes and metrics on an endpoint update is 250. Attempts
     * to add more may be ignored
     *
     * @param metricName  the name of the metric to add
     * @param metricValue the value of the metric
     */
    fun addMetric(metricName: String?, metricValue: Double?) {
        if (metricName == null) {
            LOG.warn("Null metric name provided to addGlobalMetric.")
            return
        }
        if (metricValue == null) {
            LOG.warn("Null metric value provided to addGlobalMetric.")
            return
        }
        globalMetrics[metricName] = metricValue
        saveMetrics()
    }

    /**
     * Removes the specified metric from the current endpoint profile generated by this client.
     *
     * @param metricName the name of the metric to remove
     */
    fun removeMetric(metricName: String?) {
        if (metricName == null) {
            LOG.warn("Null metric name provided to removeGlobalMetric.")
            return
        }
        endpointProfile.addMetric(metricName, null)
        globalMetrics.remove(metricName)
        saveMetrics()
    }

    companion object {
        private val LOG = Amplify.Logging.forNamespace("amplify:aws-analytics-pinpoint")
        private const val MAX_EVENT_OPERATIONS = 1000
        private const val CUSTOM_ATTRIBUTES_KEY = "ENDPOINT_PROFILE_CUSTOM_ATTRIBUTES"
        private const val CUSTOM_METRICS_KEY = "ENDPOINT_PROFILE_CUSTOM_METRICS"
    }
}
