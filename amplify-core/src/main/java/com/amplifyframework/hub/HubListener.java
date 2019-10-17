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
 * Listener for {@link HubCategory} subscriptions. This listener can be
 * used to notify the subscribers of the subscriptions.
 *
 * An instance of the {@link HubListener} is passed to the
 * {@link HubCategory#subscribe(HubChannel, HubListener)} or the
 * {@link HubCategory#subscribe(HubChannel, HubPayloadFilter, HubListener)}
 * method to get notified of subscriptions.
 */
public interface HubListener {
    /**
     * The onEvent method is triggered with the payload of
     * the event.
     *
     * @param hubPayload payload of the Hub subscription.
     *                   See {@link HubPayload} for details.
     */
    void onEvent(@NonNull HubPayload hubPayload);
}

