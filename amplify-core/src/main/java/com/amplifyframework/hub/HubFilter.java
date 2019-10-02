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

import android.support.annotation.NonNull;

public interface HubFilter {
    /**
     * Filter the HubPayload based on your criteria.
     * An implementation of a HubFilter can be passed to
     * the {@link HubCategory#listen(HubChannel, HubListener)}
     * filter listening to events from HubChannel.
     *
     * @param payload the payload that is part of the event
     *                that is transported by the Hub.
     * @return true if the payload meets your criteria,
     *         false otherwise.
     */
    boolean filter(@NonNull final HubPayload payload);
}
