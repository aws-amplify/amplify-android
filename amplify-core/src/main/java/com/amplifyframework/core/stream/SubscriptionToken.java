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

package com.amplifyframework.core.stream;

import java.util.UUID;

/**
 * The token can be used to cancel the subscription.
 */
public final class SubscriptionToken {
    /**
     * This is used to identify the subscription instance.
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
        if (!(thatObject instanceof SubscriptionToken)) {
            return false;
        }
        final SubscriptionToken that = (SubscriptionToken) thatObject;
        return uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }
}
