/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.pinpoint.core

import android.content.Context
import aws.sdk.kotlin.services.pinpoint.PinpointClient
import aws.sdk.kotlin.services.pinpoint.model.ChannelType
import aws.sdk.kotlin.services.pinpoint.model.EndpointDemographic
import aws.sdk.kotlin.services.pinpoint.model.EndpointLocation
import aws.sdk.kotlin.services.pinpoint.model.EndpointRequest
import aws.sdk.kotlin.services.pinpoint.model.EndpointUser
import aws.sdk.kotlin.services.pinpoint.model.UpdateEndpointRequest
import com.amplifyframework.analytics.AnalyticsBooleanProperty
import com.amplifyframework.analytics.AnalyticsDoubleProperty
import com.amplifyframework.analytics.AnalyticsIntegerProperty
import com.amplifyframework.analytics.AnalyticsPropertyBehavior
import com.amplifyframework.analytics.AnalyticsStringProperty
import com.amplifyframework.analytics.UserProfile
import com.amplifyframework.annotations.InternalAmplifyApi
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.category.CategoryType
import com.amplifyframework.core.store.KeyValueRepository
import com.amplifyframework.pinpoint.core.data.AndroidAppDetails
import com.amplifyframework.pinpoint.core.data.AndroidDeviceDetails
import com.amplifyframework.pinpoint.core.endpointProfile.EndpointProfile
import com.amplifyframework.pinpoint.core.endpointProfile.EndpointProfileLocation
import com.amplifyframework.pinpoint.core.endpointProfile.EndpointProfileUser
import com.amplifyframework.pinpoint.core.models.AWSPinpointUserProfileBehavior
import com.amplifyframework.pinpoint.core.util.millisToIsoDate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@InternalAmplifyApi
class TargetingClient(
    context: Context,
    private val pinpointClient: PinpointClient,
    store: KeyValueRepository,
    uniqueId: String,
    appDetails: AndroidAppDetails,
    deviceDetails: AndroidDeviceDetails,
    coroutineDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val endpointProfile = EndpointProfile(uniqueId, appDetails, deviceDetails, context, store)
    private val coroutineScope = CoroutineScope(coroutineDispatcher)


    /**
     * Allows you to tie a user to their actions and record traits about them. It includes
     * an unique User ID and any optional traits you know about them like their email, name, etc.
     *
     * @param userId  The unique identifier for the user
     * @param profile User specific data (e.g. plan, accountType, email, age, location, etc).
     *                If profile is null, no user data other than id will be attached to the endpoint.
     */
    fun identifyUser(userId: String, profile: UserProfile?) {
        val endpointProfile = currentEndpoint().apply {
            addAttribute(USER_NAME_KEY, profile?.name?.let { listOf(it) } ?: emptyList())
            addAttribute(USER_EMAIL_KEY, profile?.email?.let { listOf(it) } ?: emptyList())
            addAttribute(USER_PLAN_KEY, profile?.plan?.let { listOf(it) } ?: emptyList())
            profile?.location?.let { userLocation ->
                val country = userLocation.country
                if (null != country) {
                    location = userLocation.let {
                        EndpointProfileLocation(
                            country,
                            it.latitude,
                            it.longitude,
                            it.postalCode ?: "",
                            it.city ?: "",
                            it.region ?: ""
                        )
                    }
                }
            }
        }
        val endpointUser = EndpointProfileUser(userId).apply {
            if (profile is AWSPinpointUserProfileBehavior) {
                profile.userAttributes?.let {
                    it.forEach { entry ->
                        when (val attribute = entry.value) {
                            is AnalyticsPropertyBehavior -> {
                                addUserAttribute(entry.key, listOf(attribute.value.toString()))
                            }
                        }
                    }
                }
                profile.customProperties?.let {
                    it.forEach { entry ->
                        when (val property = entry.value) {
                            is AnalyticsStringProperty, is AnalyticsBooleanProperty -> {
                                endpointProfile.addAttribute(entry.key, listOf(property.value.toString()))
                            }
                            is AnalyticsIntegerProperty, is AnalyticsDoubleProperty -> {
                                endpointProfile.addMetric(entry.key, property.value.toString().toDouble())
                            }
                            else -> {
                                throw IllegalArgumentException("Invalid property type")
                            }
                        }
                    }
                }
            }
        }
        endpointProfile.user = endpointUser
        updateEndpointProfile(endpointProfile)
    }

    /**
     * Returns the device endpoint profile.
     * TargetingClient attributes and Metrics are added to the endpoint profile.
     *
     * @return The current device endpoint profile
     */
    fun currentEndpoint(): EndpointProfile {
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
        executeUpdate(endpointProfile)
    }

    private fun executeUpdate(endpointProfile: EndpointProfile?) {
        if (endpointProfile == null) {
            LOG.error("EndpointProfile is null, failed to update profile.")
            return
        }
        val demographic = EndpointDemographic {
            appVersion = endpointProfile.demographic.appVersion
            locale = endpointProfile.demographic.locale.toString()
            timezone = endpointProfile.demographic.timezone
            make = endpointProfile.demographic.make
            model = endpointProfile.demographic.model
            platform = endpointProfile.demographic.platform
            platformVersion = endpointProfile.demographic.platformVersion
        }
        val location = EndpointLocation {
            endpointProfile.location.latitude?.let { latitude = it }
            endpointProfile.location.longitude?.let { longitude = it }
            postalCode = endpointProfile.location.postalCode
            city = endpointProfile.location.city
            region = endpointProfile.location.region
            country = endpointProfile.location.country
        }
        val user: EndpointUser?
        if (endpointProfile.user.userId == null) {
            user = null
        } else {
            user = EndpointUser {
                userId = endpointProfile.user.userId
                userAttributes = endpointProfile.user.userAttributes
            }
        }
        val endpointRequest = EndpointRequest {
            channelType = endpointProfile.channelType
            address = endpointProfile.address
            this.location = location
            this.demographic = demographic
            effectiveDate = endpointProfile.effectiveDate.millisToIsoDate()
            if (endpointProfile.address == "" && endpointProfile.channelType == ChannelType.Gcm) {
                optOut = "ALL" // opt out from all notifications if we have a push channel type but no token
            } else if (endpointProfile.address != "" && endpointProfile.channelType != null) {
                optOut = "NONE" // no opt out, send notifications
                address = endpointProfile.address
                channelType = endpointProfile.channelType
            }

            attributes = endpointProfile.allAttributes
            metrics = endpointProfile.allMetrics
            this.user = user
        }
        val updateEndpointRequest =
            UpdateEndpointRequest.invoke {
                applicationId = endpointProfile.applicationId
                endpointId = endpointProfile.endpointId
                this.endpointRequest = endpointRequest
            }

        coroutineScope.launch {
            try {
                LOG.info("Updating EndpointProfile.")
                // This could fail if credentials are no longer stored due to sign out before this call is processed
                pinpointClient.updateEndpoint(updateEndpointRequest)
                LOG.info("EndpointProfile updated successfully.")
            } catch (e: Exception) {
                LOG.error("PinpointException occurred during endpoint update:", e)
            }
        }
    }

    companion object {
        @InternalAmplifyApi
        const val AWS_PINPOINT_PUSHNOTIFICATIONS_DEVICE_TOKEN_KEY = "FCMDeviceToken"

        private val LOG = Amplify.Logging.logger(CategoryType.ANALYTICS, "amplify:aws-analytics-pinpoint")

        private const val USER_NAME_KEY = "name"
        private const val USER_PLAN_KEY = "plan"
        private const val USER_EMAIL_KEY = "email"
    }
}
