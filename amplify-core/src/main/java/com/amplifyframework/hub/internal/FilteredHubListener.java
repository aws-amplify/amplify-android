/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.hub.internal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubListener;
import com.amplifyframework.hub.HubPayloadFilter;

import java.util.UUID;

/**
 * This encapsulation is to combine all the metadata of a subscriber
 * that will be useful in the implementation of the Hub methods.
 */
public final class FilteredHubListener {
    private final HubChannel channel;
    private final UUID listenerId;
    private final HubPayloadFilter hubPayloadFilter;
    private final HubListener hubListener;

    /**
     * Constructs a new FilteredHubListener.
     * @param channel The channel on which this listener is listening
     * @param listenerId TODO: document this field
     * @param hubPayloadFilter A filter to apply when hubListener receives a payload,
     *                         if this field is null, it means "don't apply a filter"
     * @param hubListener A listener that listens to the provided channel
     */
    public FilteredHubListener(@NonNull final HubChannel channel,
                               @NonNull final UUID listenerId,
                               @Nullable final HubPayloadFilter hubPayloadFilter,
                               @Nullable final HubListener hubListener) {
        this.channel = channel;
        this.listenerId = listenerId;
        this.hubPayloadFilter = hubPayloadFilter;
        this.hubListener = hubListener;
    }

    /**
     * Gets the channel on which the listener is listening.
     * @return Channel on which the listener is listening
     */
    public HubChannel getHubChannel() {
        return channel;
    }

    /**
     * An ID for one of the listeners. TODO: document which one.
     * @return ID for one of the listeners
     */
    public UUID getListenerId() {
        return listenerId;
    }

    /**
     * A filter that is applied when the listener receives a payload.
     * @return Filter applied when listener receives payload
     */
    public HubPayloadFilter getHubPayloadFilter() {
        return hubPayloadFilter;
    }

    /**
     * A listener, before filter is applied.
     * @return The listener before the filter is applied
     */
    public HubListener getHubListener() {
        return hubListener;
    }
}
