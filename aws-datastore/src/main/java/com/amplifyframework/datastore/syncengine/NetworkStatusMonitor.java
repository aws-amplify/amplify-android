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

package com.amplifyframework.datastore.syncengine;

import com.amplifyframework.api.events.ApiChannelEventName;
import com.amplifyframework.api.events.ApiEndpointStatusChangeEvent;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.datastore.events.NetworkStatusEvent;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubSubscriber;
import com.amplifyframework.logging.Logger;

/**
 * This class is responsible for monitoring the reachability of the
 * remote API by listening to certain events emitted by the API plugin.
 */
public final class NetworkStatusMonitor {
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-datastore");

    /**
     * Private constructor for utility class.
     */
    private NetworkStatusMonitor() {}

    /**
     * Start listening for API_ENDPOINT_STATUS_CHANGED to be emitted by the API and trigger
     * the DataStore NETWORK_STATUS event accordingly.
     */
    public static void start() {
        Amplify.Hub.subscribe(HubChannel.API, hubEvent -> {
            return ApiChannelEventName.API_ENDPOINT_STATUS_CHANGED.name().equals(hubEvent.getName());
        }, HubSubscriber.<ApiEndpointStatusChangeEvent>create(eventData -> {
            NetworkStatusEvent.from(eventData).toHubEvent().publish(HubChannel.DATASTORE, Amplify.Hub);
        }));
    }
}
