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

package com.amplifyframework.api.events;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiChannelEventName;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;

/**
 * This class represents the hub event payload for
 * {@link com.amplifyframework.api.ApiChannelEventName#API_ENDPOINT_STATUS_CHANGED}.
 */
public final class ApiEndpointStatusChangeEvent implements HubEvent.Data<ApiEndpointStatusChangeEvent> {
    private final ApiEndpointStatus currentStatus;
    private final ApiEndpointStatus previousStatus;

    /**
     * Constructs a new {@link ApiEndpointStatusChangeEvent} object.
     * @param currentStatus The current status of the endpoint.
     * @param previousStatus the previous status of the endpoint.
     */
    public ApiEndpointStatusChangeEvent(ApiEndpointStatus currentStatus, ApiEndpointStatus previousStatus) {
        this.currentStatus = currentStatus;
        this.previousStatus = previousStatus;
    }

    /**
     * Getter that returns the value of the {@link ApiEndpointStatusChangeEvent#currentStatus} field.
     * @return The value of {@link ApiEndpointStatusChangeEvent#currentStatus} field.
     */
    public ApiEndpointStatus getCurrentStatus() {
        return currentStatus;
    }

    /**
     * Getter that returns the value of the {@link ApiEndpointStatusChangeEvent#previousStatus} field.
     * @return The value of {@link ApiEndpointStatusChangeEvent#previousStatus} field.
     */
    public ApiEndpointStatus getPreviousStatus() {
        return previousStatus;
    }

    @Override
    public int hashCode() {
        int result = currentStatus != null ? currentStatus.hashCode() : 0;
        result = 31 * result + (previousStatus != null ? previousStatus.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(@Nullable Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        ApiEndpointStatusChangeEvent that = (ApiEndpointStatusChangeEvent) thatObject;

        if (!ObjectsCompat.equals(currentStatus, that.currentStatus)) {
            return false;
        }
        return ObjectsCompat.equals(previousStatus, that.previousStatus);
    }

    @NonNull
    @Override
    public String toString() {
        return "ApiEndpointStatusChangeEvent{" +
            "currentStatus=" + currentStatus +
            ", previousStatus=" + previousStatus +
            "}";
    }

    @Override
    public HubEvent<ApiEndpointStatusChangeEvent> toHubEvent() {
        return HubEvent.create(ApiChannelEventName.API_ENDPOINT_STATUS_CHANGED, this);
    }

    /**
     * Factory method that attempts to cast the data field of the
     * {@link HubEvent} object as an instance of {@link ApiEndpointStatusChangeEvent}.
     * @param hubEvent An instance of {@link HubEvent}
     * @return An instance of {@link ApiEndpointStatusChangeEvent}.
     * @throws AmplifyException If unable to cast to the target type.
     */
    public static ApiEndpointStatusChangeEvent from(HubEvent<?> hubEvent) throws AmplifyException {
        if (hubEvent.getData() instanceof ApiEndpointStatusChangeEvent) {
            return (ApiEndpointStatusChangeEvent) hubEvent.getData();
        }
        String expectedClassName = ApiEndpointStatusChangeEvent.class.getName();
        throw new AmplifyException("Unable to cast event data from " + expectedClassName,
                                   "Ensure that the event payload is of type " + expectedClassName);
    }

    /**
     * Enum that describes the reachability status of an API endpoint.
     */
    public enum ApiEndpointStatus {
        /**
         * The status of the network is unknown. Usually occurs on startup.
         */
        UNKOWN,

        /**
         * API endpoint is reachable.
         */
        REACHABLE,

        /**
         * API endpoint is not reachable.
         */
        NOT_REACHABLE;

        /**
         * Publishes an event to the Hub with the instance as the current value
         * and the previous status passed in as a parameter.
         * @param previousStatus The value to be sent as the previous status.
         */
        public void announceTransitionFrom(ApiEndpointStatus previousStatus) {
            ApiEndpointStatusChangeEvent eventData = new ApiEndpointStatusChangeEvent(this, previousStatus);
            eventData.toHubEvent().publish(HubChannel.API);
        }
    }
}
