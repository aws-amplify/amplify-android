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
 * An enumeration of the names of events relating the {@link DataStoreCategory},
 * that are published via {@link HubCategory#publish(HubChannel, HubEvent)} on the
 * {@link HubChannel#DATASTORE} channel.
 */
public enum DataStoreChannelEventName {
    /**
     * The DataStore as a whole (not just the sync piece) is ready. At this point all data is available.
     */
    READY("ready"),

    /**
     * Indicates that the network is active or not.
     * It is triggered on DataStore start and also every time the network status changes.
     */
    NETWORK_STATUS("networkStatus"),

    /**
     * The WebSocket connection has been established, in addition to all of the GraphQL
     * subscriptions it hosts.
     */
    SUBSCRIPTIONS_ESTABLISHED("subscriptionsEstablished"),

    /**
     * The server sent the client data over the WebSocket subscription. The data was
     * successfully melded back into the local store.
     */
    SUBSCRIPTION_DATA_PROCESSED("subscriptionDataProcessed"),

    /**
     * Notifies if there are mutations in the outbox.
     */
    OUTBOX_STATUS("outboxStatus"),

    /**
     * A local mutation was placed on the outbox.
     */
    OUTBOX_MUTATION_ENQUEUED("outboxMutationEnqueued"),

    /**
     * A mutation from the outbox has been successfully sent and merged to the backend.
     */
    OUTBOX_MUTATION_PROCESSED("outboxMutationProcessed"),

    /**
     * A mutation from the outbox was sent but was met with an error response.
     */
    OUTBOX_MUTATION_FAILED("outboxMutationFailed"),

    /**
     * The DataStore is about to start the Sync Queries.
     */
    SYNC_QUERIES_STARTED("syncQueriesStarted"),

    /**
     * All models have been synced.
     */
    SYNC_QUERIES_READY("syncQueriesReady"),

    /**
     * The sync process for one of the models has completed. This
     * event is emitted with metrics related to the latest sync
     * for the model.
     */
    MODEL_SYNCED("modelSynced"),

    /**
     * Non applicable data was received from the backend.
     */
    NON_APPLICABLE_DATA_RECEIVED("nonApplicableDataReceived");

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
