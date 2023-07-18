/*
 *  Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.pinpoint.core.endpointProfile

import android.content.Context
import aws.sdk.kotlin.services.pinpoint.model.ChannelType
import com.amplifyframework.annotations.InternalAmplifyApi
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.category.CategoryType
import com.amplifyframework.pinpoint.core.data.AndroidAppDetails
import com.amplifyframework.pinpoint.core.data.AndroidDeviceDetails
import com.amplifyframework.pinpoint.core.util.millisToIsoDate
import java.util.Collections
import java.util.MissingResourceException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@InternalAmplifyApi
class EndpointProfile(
    uniqueId: String,
    appDetails: AndroidAppDetails,
    deviceDetails: AndroidDeviceDetails,
    applicationContext: Context
) {
    private val attributes: MutableMap<String, List<String>> = ConcurrentHashMap()
    private val metrics: MutableMap<String, Double> = ConcurrentHashMap()
    private val currentNumOfAttributesAndMetrics = AtomicInteger(0)

    var channelType: ChannelType? = null
    var address: String = ""

    private val country: String = try {
        applicationContext.resources.configuration.locales[0].isO3Country
    } catch (exception: MissingResourceException) {
        LOG.debug("Locale getISO3Country failed, falling back to getCountry.")
        applicationContext.resources.configuration.locales[0].country
    }
    var location: EndpointProfileLocation = EndpointProfileLocation(country)
    var demographic: EndpointProfileDemographic = EndpointProfileDemographic(
        appDetails,
        deviceDetails,
        country
    )
    var effectiveDate: Long = System.currentTimeMillis()
    var user: EndpointProfileUser = EndpointProfileUser()
    val applicationId = appDetails.appId
    val endpointId = uniqueId

    /**
     * Adds a custom attribute to this [EndpointProfile] with the specified key.
     * Only 20 custom attributes/metrics are allowed to be added to a EndpointProfile. If 20
     * attributes already exist on this EndpointProfile, the call may be ignored.
     *
     * @param name   The name of the custom attribute. The name will be truncated if it
     * exceeds 50 characters.
     * @param values An array of values of the custom attribute. The values will be truncated if
     * it exceeds 100 characters.
     */
    fun addAttribute(name: String, values: List<String>?) {
        if (null != values) {
            if (currentNumOfAttributesAndMetrics.get() < MAX_NUM_OF_METRICS_AND_ATTRIBUTES) {
                val key = processAttributeMetricKey(name)
                if (!attributes.containsKey(key)) {
                    currentNumOfAttributesAndMetrics.incrementAndGet()
                }
                attributes[key] = processAttributeValues(values)
            } else {
                LOG.warn("Max number of attributes/metrics reached($MAX_NUM_OF_METRICS_AND_ATTRIBUTES).")
            }
        } else {
            if (attributes.remove(name) != null) {
                currentNumOfAttributesAndMetrics.decrementAndGet()
            }
        }
    }

    /**
     * Determines if this [EndpointProfile] contains a specific custom attribute
     *
     * @param attributeName The name of the custom attribute
     * @return true if this [EndpointProfile] has a custom attribute with the
     * specified name, false otherwise
     */
    fun hasAttribute(attributeName: String): Boolean {
        return attributes.containsKey(attributeName)
    }

    /**
     * Returns the array of values of the custom attribute with the specified name.
     *
     * @param name The name of the custom attribute to return
     * @return The array of custom attributes with the specified name, or null if attribute does
     * not exist
     */
    fun getAttribute(name: String): List<String>? {
        return attributes[name]
    }

    /**
     * Adds a custom attribute to this [EndpointProfile] with the specified key.
     * Only 20 custom attributes are allowed to be added to an
     * [EndpointProfile]. If 20 custom attributes/metrics already exist on this
     * [EndpointProfile], the call may be ignored.
     *
     * @param name   The name of the custom attribute. The name will be truncated if it
     * exceeds 50 characters.
     * @param values An array of values of the custom attribute. The values will be truncated if
     * it exceeds 100 characters.
     * @return The same [EndpointProfile] instance is returned to allow for
     * method chaining.
     */
    fun withAttribute(name: String, values: List<String>): EndpointProfile {
        addAttribute(name, values)
        return this
    }

    /**
     * Returns a map of all custom attributes contained within this
     * [EndpointProfile]
     *
     * @return a map of all custom attributes, where the attribute names are the keys
     * and the attribute values are the values
     */
    val allAttributes: Map<String, List<String>>
        get() = Collections.unmodifiableMap(attributes)

    /**
     * Adds a metric to this [EndpointProfile] with the specified key. Only
     * 250 attributes/metrics are allowed to be added to an Event. If 250
     * attribute/metrics already exist on this Event, the call may be ignored.
     *
     * @param name  The name of the metric. The name will be truncated if it
     * exceeds 50 characters.
     * @param value The value of the metric.
     */
    fun addMetric(name: String, value: Double?) {
        if (null != value) {
            if (currentNumOfAttributesAndMetrics.get() <
                MAX_NUM_OF_METRICS_AND_ATTRIBUTES
            ) {
                val key = processAttributeMetricKey(name)
                if (!metrics.containsKey(key)) {
                    currentNumOfAttributesAndMetrics.incrementAndGet()
                }
                metrics[key] = value
            } else {
                LOG.warn(
                    "Max number of attributes/metrics reached(" +
                        MAX_NUM_OF_METRICS_AND_ATTRIBUTES +
                        ")."
                )
            }
        } else {
            if (metrics.remove(name) != null) {
                currentNumOfAttributesAndMetrics.decrementAndGet()
            }
        }
    }

    /**
     * Determines if this [EndpointProfile] contains a specific metric.
     *
     * @param metricName The name of the metric
     * @return true if this [EndpointProfile] has a metric with the
     * specified name, false otherwise
     */
    fun hasMetric(metricName: String): Boolean {
        return metrics.containsKey(metricName)
    }

    /**
     * Returns the value of the metric with the specified name.
     *
     * @param name The name of the metric to return
     * @return The metric with the specified name, or null if metric does not
     * exist
     */
    fun getMetric(name: String): Double? {
        return metrics[name]
    }

    /**
     * Adds a metric to this [EndpointProfile] with the specified key. Only
     * 250 attributes/metrics are allowed to be added to an
     * [EndpointProfile]. If 250 attribute/metrics already exist on this
     * [EndpointProfile], the call may be ignored.
     *
     * @param name  The name of the metric. The name will be truncated if it
     * exceeds 50 characters.
     * @param value The value of the metric.
     * @return The same [EndpointProfile] instance is returned to allow for
     * method chaining.
     */
    fun withMetric(name: String, value: Double): EndpointProfile {
        addMetric(name, value)
        return this
    }

    /**
     * Returns a map of all metrics contained within this [EndpointProfile]
     *
     * @return a map of all metrics, where the metric names are the keys and the
     * metric values are the values
     */
    val allMetrics: Map<String, Double>
        get() = Collections.unmodifiableMap(metrics)

    override fun toString(): String {
        return toJSONObject().toString()
    }

    private fun toJSONObject(): JsonObject {
        return buildJsonObject {
            put("Address", address)
            put("ApplicationId", applicationId)
            put("EndpointId", endpointId)
            put("Location", Json.encodeToString(EndpointProfileLocation.serializer(), location))
            put("Demographic", Json.encodeToString(EndpointProfileDemographic.serializer(), demographic))
            put("EffectiveDate", effectiveDate.millisToIsoDate())
            val attributesJson = buildJsonObject {
                for ((key, value) in allAttributes) {
                    try {
                        put(key, JsonArray(value.map { JsonPrimitive(it) }))
                    } catch (e: Exception) {
                        // Do not log e due to potentially sensitive information
                        LOG.warn("Error serializing attributes.")
                    }
                }
            }
            // If there are any attributes put then add the attributes to the structure
            if (attributesJson.isNotEmpty()) {
                put("Attributes", attributesJson)
            }

            val metricsJson = buildJsonObject {
                for ((key, value) in allMetrics) {
                    try {
                        put(key, value)
                    } catch (e: Exception) {
                        // Do not log e due to potentially sensitive information
                        LOG.error("Error serializing metric.")
                    }
                }
            }

            // If there are any metrics put then add the attributes to the structure
            if (metricsJson.isNotEmpty()) {
                put("Metrics", metricsJson)
            }

            put("User", Json.encodeToString(EndpointProfileUser.serializer(), user))
        }
    }

    companion object {
        const val MAX_NUM_OF_METRICS_AND_ATTRIBUTES = 20
        private const val MAX_ENDPOINT_ATTRIBUTE_METRIC_KEY_LENGTH = 50
        private const val MAX_ENDPOINT_ATTRIBUTE_VALUE_LENGTH = 100
        private const val MAX_ENDPOINT_ATTRIBUTE_VALUES = 50
        private val LOG = Amplify.Logging.logger(CategoryType.ANALYTICS, "amplify:aws-analytics-pinpoint")
        private fun processAttributeMetricKey(key: String): String {
            val trimmedKey = key.take(MAX_ENDPOINT_ATTRIBUTE_METRIC_KEY_LENGTH)
            if (trimmedKey.length < key.length) {
                LOG.warn(
                    "The attribute key has been trimmed to a length of $MAX_ENDPOINT_ATTRIBUTE_METRIC_KEY_LENGTH " +
                        "characters."
                )
            }
            return trimmedKey
        }

        private fun processAttributeValues(values: List<String>): List<String> {
            val trimmedValues: MutableList<String> = ArrayList()
            for ((valuesCount, value) in values.withIndex()) {
                val trimmedValue = value.take(MAX_ENDPOINT_ATTRIBUTE_VALUE_LENGTH)
                if (trimmedValue.length < value.length) {
                    LOG.warn(
                        "The attribute value has been trimmed to a length of $MAX_ENDPOINT_ATTRIBUTE_VALUE_LENGTH " +
                            "characters."
                    )
                }
                trimmedValues.add(trimmedValue)
                if (valuesCount + 1 >= MAX_ENDPOINT_ATTRIBUTE_VALUES) {
                    LOG.warn("The attribute values has been reduced to$MAX_ENDPOINT_ATTRIBUTE_VALUES values.")
                    break
                }
            }
            return trimmedValues
        }
    }
}
