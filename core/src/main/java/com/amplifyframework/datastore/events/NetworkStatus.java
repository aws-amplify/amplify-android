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

/**
 * Event payload for the {@link com.amplifyframework.datastore.DataStoreChannelEventName#NETWORK_STATUS} event.
 */
public final class NetworkStatus {
    private final boolean active;

    /**
     * Constructs a {@link NetworkStatus} object.
     * @param isActive True if connection is active, false otherwise.
     */
    public NetworkStatus(boolean isActive) {
        this.active = isActive;
    }
}
