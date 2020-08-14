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

package com.amplifyframework.datastore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.hub.HubCategory;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;

import java.util.Objects;

/**
 * An enumeration of the names of events relating the the {@link DataStoreCategory},
 * that are published via {@link HubCategory#publish(HubChannel, HubEvent)} on the
 * {@link HubChannel#DATASTORE} channel.
 */
public enum DataStoreChannelEventName {
    /**
     * An item in the local storage has been published to the cloud.
     * An event that uses this value for {@link HubEvent#getName()} will contain a model object
     * in the {@link HubEvent#getData()}.
     */
    PUBLISHED_TO_CLOUD("published_to_cloud"),

    /**
     * A model was updated locally, from a model version that was received from the cloud.
     * An event that uses this value for {@link HubEvent#getName()} will contain a model object
     * in the {@link HubEvent#getData()}.
     */
    RECEIVED_FROM_CLOUD("received_from_cloud"),

    /**
     * The remote synchonization processes started.
     */
    REMOTE_SYNC_STARTED("remote_sync_started"),

    /**
     * The remote synchonization processes stopped.
     */
    REMOTE_SYNC_STOPPED("remote_sync_stopped"),

    /**
     * Indicates that the network is active or not.
     * It is triggered on DataStore start and also every time the network status changes.
     */
    NETWORK_STATUS("networkStatus"),

    /**
     * The websocket connection has been established and all the graphql subscriptions too.
     */
    SUBSCRIPTIONS_ESTABLISHED("subscriptionsEstablished"),

    /**
     * The DataStore as a whole (not just the sync piece) is ready. At this point all data is available.
     */
    READY("ready");

    private final String hubEventName;

    /**
     * Enumerate the name of an event that is published on Hub, on the {@link HubChannel#DATASTORE} channel.
     * @param hubEventName The name of an event to use when creating an {@link HubEvent}
     */
    DataStoreChannelEventName(@NonNull String hubEventName) {
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
     * DataStore category.
     * @param possibleEventName Possibly, the name of a Hub event published by DataStore
     * @return The enumerated event name, if found
     * @throws IllegalArgumentException If the provided string is not a known event name
     */
    @NonNull
    public static DataStoreChannelEventName fromString(@Nullable String possibleEventName) {
        for (DataStoreChannelEventName value : values()) {
            if (value.toString().equals(possibleEventName)) {
                return value;
            }
        }
        final String errorMessage =
            "DataStore category does not publish any Hub event with name = " + possibleEventName;
        throw new IllegalArgumentException(errorMessage);
    }
}
