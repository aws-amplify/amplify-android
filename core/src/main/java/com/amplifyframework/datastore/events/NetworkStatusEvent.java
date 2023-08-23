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

package com.amplifyframework.datastore.events;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.events.ApiEndpointStatusChangeEvent;
import com.amplifyframework.api.events.ApiEndpointStatusChangeEvent.ApiEndpointStatus;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.datastore.DataStoreChannelEventName;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.logging.Logger;

/**
 * Event payload for the {@link DataStoreChannelEventName#NETWORK_STATUS} event.
 */
public final class NetworkStatusEvent implements HubEvent.Data<NetworkStatusEvent> {
    private static final Logger LOG = Amplify.Logging.logger(CategoryType.DATASTORE, "amplify:aws-datastore");
    private final boolean active;

    /**
     * Constructs a {@link NetworkStatusEvent} object.
     * @param isActive True if connection is active, false otherwise.
     */
    public NetworkStatusEvent(boolean isActive) {
        this.active = isActive;
    }

    /**
     * Getter for the active field.
     * @return The value of the active field.
     */
    public boolean getActive() {
        return active;
    }

    @Override
    public int hashCode() {
        return Boolean.valueOf(active).hashCode();
    }

    @Override
    public boolean equals(@Nullable Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        NetworkStatusEvent that = (NetworkStatusEvent) thatObject;
        return ObjectsCompat.equals(active, that.active);
    }

    @NonNull
    @Override
    public String toString() {
        return "NetworkStatus{active=" + active + "}";
    }

    @Override
    public HubEvent<NetworkStatusEvent> toHubEvent() {
        return HubEvent.create(DataStoreChannelEventName.NETWORK_STATUS, this);
    }

    /**
     * Factory method that attempts to cast the data field of the
     * {@link HubEvent} object as an instance of {@link NetworkStatusEvent}.
     * @param hubEvent An instance of {@link HubEvent}
     * @return An instance of {@link NetworkStatusEvent}.
     * @throws AmplifyException If unable to cast to the target type.
     */
    public static NetworkStatusEvent from(HubEvent<?> hubEvent) throws AmplifyException {
        if (hubEvent.getData() instanceof NetworkStatusEvent) {
            return (NetworkStatusEvent) hubEvent.getData();
        }
        String expectedClassName = NetworkStatusEvent.class.getName();
        throw new AmplifyException("Unable to cast event data from " + expectedClassName,
                                   "Ensure that the event payload is of type " + expectedClassName);
    }

    /**
     * Factory method that transforms an instance of
     * {@link ApiEndpointStatusChangeEvent} into an instance of {@link NetworkStatusEvent}.
     * @param apiEvent An instance of {@link ApiEndpointStatusChangeEvent}
     * @return An instance of {@link NetworkStatusEvent}.
     */
    public static NetworkStatusEvent from(ApiEndpointStatusChangeEvent apiEvent) {
        boolean isActive = ApiEndpointStatus.REACHABLE.equals(apiEvent.getCurrentStatus());
        return new NetworkStatusEvent(isActive);
    }
}
