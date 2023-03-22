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

package com.amplifyframework.analytics.pinpoint.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.analytics.AnalyticsProperties;
import com.amplifyframework.analytics.UserProfile;
import com.amplifyframework.pinpoint.core.models.AWSPinpointUserProfileBehavior;

import java.util.Objects;

/**
 * Extends the category-defined UserProfile class to include features supported
 * relevant to Pinpoint only.
 */
public final class AWSPinpointUserProfile extends UserProfile implements AWSPinpointUserProfileBehavior {
    private final AnalyticsProperties userAttributes;

    /**
     * Constructor that mirrors the parent class.
     * @param builder An instance of the builder with the desired properties set.
     */
    protected AWSPinpointUserProfile(@NonNull Builder builder) {
        super(builder);
        this.userAttributes = builder.userAttributes;
    }

    /**
     * Gets all the available user attributes.
     * @return The user's attributes.
     */
    @Nullable
    public AnalyticsProperties getUserAttributes() {
        return userAttributes;
    }

    /**
     * Begins construction of an {@link AWSPinpointUserProfile} using a builder pattern.
     * @return An {@link AWSPinpointUserProfile.Builder} instance
     */
    @NonNull
    public static Builder builder() {
        return new AWSPinpointUserProfile.Builder();
    }

    @Override
    public boolean equals(@Nullable Object object) {
        boolean isEquals = super.equals(object);
        AWSPinpointUserProfile that = (AWSPinpointUserProfile) object;
        return isEquals && ObjectsCompat.equals(userAttributes, that.userAttributes);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + (userAttributes != null ? userAttributes.hashCode() : 0);
    }

    @NonNull
    @Override
    public String toString() {
        return "UserProfile{" +
                "name='" + getName() + '\'' +
                ", email='" + getEmail() + '\'' +
                ", plan='" + getPlan() + '\'' +
                ", location=" + getLocation() +
                ", customProperties=" + getCustomProperties() +
                ", userProperties=" + userAttributes +
                '}';
    }

    /**
     * Builder for the {@link AWSPinpointUserProfile} class.
     */
    public static final class Builder extends UserProfile.Builder<Builder, AWSPinpointUserProfile> {
        private AnalyticsProperties userAttributes;

        /**
         * Sets the user's attributes of the builder instance.
         * @param userAttributes The collection of attributes.
         * @return Current builder instance, for method chaining.
         */
        @NonNull
        public Builder userAttributes(@NonNull final AnalyticsProperties userAttributes) {
            Objects.requireNonNull(userAttributes);
            this.userAttributes = userAttributes;
            return this;
        }

        /**
         * Builds an instance of {@link UserProfile}, using the provided values.
         * @return An {@link UserProfile}
         */
        @NonNull
        public AWSPinpointUserProfile build() {
            return new AWSPinpointUserProfile(this);
        }
    }
}
