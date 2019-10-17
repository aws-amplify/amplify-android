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
 * A filter which can be used to control which {@link HubPayload}s a
 * subscribed {@link HubListener} will receive from the Hub. The
 * {@link HubFilters} utility constants factories for some commonly used
 * types of {@link HubPayloadFilter}s. Alternately, you are free to
 * implement your own custom, more complex, logic.
 */
public interface HubPayloadFilter {

    /**
     * Filter the HubPayload based on your criteria.
     * An implementation of a HubPayloadFilter can be passed to
     * the {@link HubCategory#subscribe(HubChannel, HubListener)}
     * filter listening to events from HubChannel.
     *
     * @param payload the payload that is part of the event
     *                that is transported by the Hub.
     * @return true if the payload meets your criteria,
     *         false otherwise.
     */
    boolean filter(@NonNull HubPayload payload);
}

