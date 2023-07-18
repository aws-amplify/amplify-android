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
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.datastore.DataStoreChannelEventName;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.logging.Logger;

/**
 * Event payload for the {@link DataStoreChannelEventName#OUTBOX_STATUS} event.
 */
public final class OutboxStatusEvent implements HubEvent.Data<OutboxStatusEvent> {
    private static final Logger LOG = Amplify.Logging.logger(CategoryType.DATASTORE, "amplify:aws-datastore");
    private final boolean isEmpty;

    /**
     * Constructs a {@link OutboxStatusEvent} object.
     * @param isEmpty True if the outbox does not have any mutations, false otherwise.
     */
    public OutboxStatusEvent(boolean isEmpty) {
        this.isEmpty = isEmpty;
    }

    /**
     * Returns true if the outbox is empty.
     * @return The value of the isEmpty field.
     */
    public boolean isEmpty() {
        return isEmpty;
    }

    @Override
    public int hashCode() {
        return Boolean.valueOf(isEmpty).hashCode();
    }

    @Override
    public boolean equals(@Nullable Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        OutboxStatusEvent that = (OutboxStatusEvent) thatObject;
        return ObjectsCompat.equals(isEmpty, that.isEmpty);
    }

    @NonNull
    @Override
    public String toString() {
        return "OutboxStatus{isEmpty=" + isEmpty + "}";
    }

    @Override
    public HubEvent<OutboxStatusEvent> toHubEvent() {
        return HubEvent.create(DataStoreChannelEventName.OUTBOX_STATUS, this);
    }

    /**
     * Factory method that attempts to cast the data field of the
     * {@link HubEvent} object as an instance of {@link OutboxStatusEvent}.
     * @param hubEvent An instance of {@link HubEvent}
     * @return An instance of {@link OutboxStatusEvent}.
     * @throws AmplifyException If unable to cast to the target type.
     */
    public static OutboxStatusEvent from(HubEvent<?> hubEvent) throws AmplifyException {
        if (hubEvent.getData() instanceof OutboxStatusEvent) {
            return (OutboxStatusEvent) hubEvent.getData();
        }
        String expectedClassName = OutboxStatusEvent.class.getName();
        throw new AmplifyException("Unable to cast event data from " + expectedClassName,
                                   "Ensure that the event payload is of type " + expectedClassName);
    }
}
