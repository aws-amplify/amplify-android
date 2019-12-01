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

import androidx.core.util.ObjectsCompat;

import java.util.UUID;

/**
 * A SubscriptionToken is returned by the Hub when creating a new
 * subscription for a {@link HubSubscriber}. This token can be used to
 * cancel a subscriber's subscription.
 */
public final class SubscriptionToken {
    /**
     * This is used to identify an instance of the
     * subscriber {@link HubSubscriber} subscribed with Hub.
     */
    private final UUID uuid;

    private SubscriptionToken(final UUID uuid) {
        this.uuid = uuid;
    }

    /**
     * Creates a new SubscriptionToken.
     * @return A new subscription token
     */
    public static SubscriptionToken create() {
        return new SubscriptionToken(UUID.randomUUID());
    }

    @Override
    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        SubscriptionToken that = (SubscriptionToken) thatObject;

        return ObjectsCompat.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return uuid != null ? uuid.hashCode() : 0;
    }
}
