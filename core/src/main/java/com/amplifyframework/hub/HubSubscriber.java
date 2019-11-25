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

/**
 * An instance of the {@link HubSubscriber} may be passed to the
 * {@link HubCategory#subscribe(HubChannel, HubSubscriber)} or the
 * {@link HubCategory#subscribe(HubChannel, HubEventFilter, HubSubscriber)}
 * methods to subscribe to various types of Hub events.
 */
public interface HubSubscriber {

    /**
     * Called to notify that there is a new event available in the Hub.
     * @param hubEvent A hub event
     */
    void onEvent(@NonNull HubEvent hubEvent);
}
