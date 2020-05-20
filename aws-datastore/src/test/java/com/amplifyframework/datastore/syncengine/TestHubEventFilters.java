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

import com.amplifyframework.core.model.Model;
import com.amplifyframework.datastore.DataStoreChannelEventName;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.hub.HubEventFilter;

/**
 * A utility to build {@link HubEventFilter}s that useful when looking for test outcomes.
 */
public final class TestHubEventFilters {
    private TestHubEventFilters() {}

    /**
     * Creates an {@link HubEventFilter} that will look for an
     * {@link DataStoreChannelEventName#PUBLISHED_TO_CLOUD} event on the
     * {@link com.amplifyframework.hub.HubChannel#DATASTORE} channel.
     * The model that is mentioned in the {@link HubEvent#getData()} must be of the same type,
     * and have the same ID, as one of the provided models.
     * @param models A list of models that may be matched by this filter
     * @param <T> Type of model mentioned in HubEvent
     * @return A filter that returns true if one of the provided models is matched.
     */
    @SuppressWarnings("varargs")
    @SafeVarargs
    public static <T extends Model> HubEventFilter publicationOf(T... models) {
        return event -> {
            if (!DataStoreChannelEventName.PUBLISHED_TO_CLOUD.toString().equals(event.getName())) {
                return false;
            }
            PendingMutation<? extends Model> pendingMutation = (PendingMutation<? extends Model>) event.getData();
            if (pendingMutation == null) {
                return false;
            }
            for (Model model : models) {
                if (model.getClass().isAssignableFrom(pendingMutation.getClassOfMutatedItem()) &&
                        model.getId().equals(pendingMutation.getMutatedItem().getId())) {
                    return true;
                }
            }
            return false;
        };
    }
}
