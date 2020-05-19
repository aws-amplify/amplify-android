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

package com.amplifyframework.auth;

import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;

/**
 * An enumeration of the names of events relating the the {@link AuthCategory},
 * that are published via {@link com.amplifyframework.hub.HubCategory#publish(HubChannel, HubEvent)} on the
 * {@link HubChannel#AUTH} channel.
 */
public enum AuthChannelEventName {
    /**
     * Auth has transitioned from a Signed In state to a Signed Out state.
     */
    SIGNED_OUT,

    /**
     * Auth has transitioned from a Signed Out state to a Signed In state.
     */
    SIGNED_IN,

    /**
     * The authorization credentials have expired and require the user to re-sign in to be refreshed. Auth remains in a
     * Signed In authentication state but the user is no longer authorized to perform any operations.
     */
    SESSION_EXPIRED;
}
