/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.analytics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.hub.HubCategory;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;

import java.util.Objects;

/**
 * An enumeration of the names of events relating the the {@link AnalyticsCategory},
 * that are published via {@link HubCategory#publish(HubChannel, HubEvent)} on the
 * {@link HubChannel#DATASTORE} channel.
 */
public enum AnalyticsChannelEventName {
    /**
     * An attempt was made to add a {@link AnalyticsPropertyBehavior} of type T that is not supported
     * by the plugin in use.
     */
    INVALID_PROPERTY_TYPE("invalid_property_type");

    private final String hubEventName;

    /**
     * Enumerate the name of an even that is published on Hub, on the {@link HubChannel#ANALYTICS} channel.
     * @param hubEventName The name of an event to use when creating an {@link HubEvent}
     */
    AnalyticsChannelEventName(@NonNull String hubEventName) {
        Objects.requireNonNull(hubEventName);
        this.hubEventName = hubEventName;
    }

    @NonNull
    @Override
    public String toString() {
        return hubEventName;
    }

    /**
     * Check if the provided string is one of the enumerated hub event names used by the
     * Analytics category.
     * @param possibleEventName Possibly, the name of a Hub event published by Analytics
     * @return The enumerated event name, if found
     * @throws IllegalArgumentException If the provided string is not a known event name
     */
    @NonNull
    public static AnalyticsChannelEventName fromString(@Nullable String possibleEventName) {
        for (AnalyticsChannelEventName value : values()) {
            if (value.toString().equals(possibleEventName)) {
                return value;
            }
        }
        final String errorMessage =
                "Analytics category does not publish any Hub event with name = " + possibleEventName;
        throw new IllegalArgumentException(errorMessage);
    }
}
