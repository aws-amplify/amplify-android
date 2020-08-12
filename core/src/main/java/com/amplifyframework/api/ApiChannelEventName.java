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

package com.amplifyframework.api;

import com.amplifyframework.hub.HubCategory;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;

/**
 * An enumeration of the names of events relating the the {@link ApiCategory},
 * that are published via {@link HubCategory#publish(HubChannel, HubEvent)} on the
 * {@link HubChannel#API} channel.
 */
public enum ApiChannelEventName {
    /**
     * Indicates that the HTTP client's ability to reach the backend API has changed. This
     * event will be triggered when/if the HTTP client used to communicate with the
     * API endpoint receives an event that affects its current status. For example,
     * if the backend API becomes unreachable due to network error OR if it's currently
     * unreachable, and the client is suddenly able to communicate with the backend.
     * @see com.amplifyframework.api.events.ApiEndpointStatusChangeEvent
     * @see com.amplifyframework.api.events.ApiEndpointStatusChangeEvent.ApiEndpointStatus
     */
    API_ENDPOINT_STATUS_CHANGED
}
