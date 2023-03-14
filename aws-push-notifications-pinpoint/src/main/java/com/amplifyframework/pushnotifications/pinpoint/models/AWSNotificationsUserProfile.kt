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

package com.amplifyframework.pushnotifications.pinpoint.models

import com.amplifyframework.analytics.AnalyticsProperties
import com.amplifyframework.analytics.UserProfile
import com.amplifyframework.pinpoint.core.models.AWSPinpointUserProfileBehavior

/**
 * Extends the category-defined UserProfile class to include features supported relevant to Pinpoint Notifications only.
 */
data class AWSNotificationsUserProfile internal constructor(val builder: Builder) :
    UserProfile(builder),
    AWSPinpointUserProfileBehavior {
    override val userAttributes = builder.userAttributes

    companion object {
        /**
         * Begins construction of an {@link AWSNotificationsUserProfile} using a builder pattern.
         * @return An {@link AWSNotificationsUserProfile.Builder} instance
         */
        @JvmStatic
        fun builder() = Builder()

        inline operator fun invoke(block: Builder.() -> Unit) = Builder().apply(block).build()
    }

    /**
     * Builder for the {@link AWSNotificationsUserProfile} class.
     */
    class Builder : UserProfile.Builder<Builder, AWSNotificationsUserProfile>() {
        var userAttributes: AnalyticsProperties? = null
            private set

        /**
         * Sets the user's attributes of the builder instance.
         * @param userAttributes The collection of attributes.
         * @return Current builder instance, for method chaining.
         */
        fun userAttributes(userAttributes: AnalyticsProperties) = apply { this.userAttributes = userAttributes }

        /**
         * Builds an instance of [UserProfile], using the provided values.
         * @return An [UserProfile]
         */
        override fun build() = AWSNotificationsUserProfile(this)
    }
}
