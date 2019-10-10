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

package com.amplifyframework.hub;

import androidx.annotation.NonNull;

import java.util.Objects;
import java.util.UUID;

/**
 * SubscriptionToken can be used to unsubscribe a Hub listener. Although SubscriptionToken
 * conforms to Hashable, only the `id` property is considered for equality and hash value;
 * `channel` is used only for routing an unsubscribe request to the correct HubChannel.
 */
public final class SubscriptionToken {
    /**
     * This is used to identify an instance of the
     * subscriber {@link HubListener} subscribed with Hub.
     */
    private final UUID uuid;

    /**
     * The HubChannel is stored here in order to optimally
     * locate the channel of the subscriber. This is used to
     * optimally remove listeners in unsubscribe.
     */
    private final HubChannel hubChannel;

    /**
     * Construct the subscription token object.
     * @param uuid uniquely identifies the subscriber.
     * @param hubChannel the channel of the subscriber.
     */
    public SubscriptionToken(@NonNull final UUID uuid, @NonNull final HubChannel hubChannel) {
        this.uuid = Objects.requireNonNull(uuid);
        this.hubChannel = Objects.requireNonNull(hubChannel);
    }

    /**
     * Gets the UUID of the subscription.
     * @return the unique identifier of the subscriber.
     */
    public UUID getUuid() {
        return uuid;
    }

    /**
     * Gets the hub channel to which the subscriber is bound.
     * @return the hub channel of the subscriber.
     */
    public HubChannel getHubChannel() {
        return hubChannel;
    }

    @Override
    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (!(thatObject instanceof SubscriptionToken)) {
            return false;
        }
        final SubscriptionToken that = (SubscriptionToken) thatObject;
        return uuid.equals(that.getUuid());
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }
}
