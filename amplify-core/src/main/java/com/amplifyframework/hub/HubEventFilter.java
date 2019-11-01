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
 * A filter which can be used to control which {@link HubEvent}s a
 * subscribed {@link HubSubscriber} will receive from the Hub. The
 * {@link HubEventFilters} utility contains factories for some commonly used
 * types of {@link HubEventFilter}s. Alternately, you are free to
 * implement your own custom, more complex, logic.
 */
public interface HubEventFilter {

    /**
     * Filter the HubEvent based on your criteria.  An implementation of
     * a HubEventFilter can be passed to the {@link
     * HubCategory#subscribe(HubChannel, HubSubscriber)} to further
     * filter events on a given HubChannel.
     *
     * @param hubEvent An event as received on a hub channel
     * @return true if the event meets your criteria,
     *         false otherwise.
     */
    boolean filter(@NonNull HubEvent hubEvent);
}

