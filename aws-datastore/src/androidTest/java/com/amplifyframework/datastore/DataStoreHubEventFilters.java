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

package com.amplifyframework.datastore;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.datastore.appsync.ModelWithMetadata;
import com.amplifyframework.datastore.syncengine.OutboxMutationEvent;
import com.amplifyframework.hub.HubEventFilter;

/**
 * Utility to create some common filters that can be applied to hub subscriptions.
 */
public final class DataStoreHubEventFilters {
    private DataStoreHubEventFilters() {}

    /**
     * Watches for publication (out of mutation queue) of a given model.
     * Creates a filter that catches events from the mutation processor.
     * Events will pass if they mention the provided model by its name and ID,
     * and state that it has successfully been published off of the mutation queue.
     * @param model A model to watch for on the Hub
     * @param <T> The type of the model
     * @return A filter that watches for publication of the provided model.
     */
    public static <T extends Model> HubEventFilter publicationOf(T model) {
        return event -> {
            if (!DataStoreChannelEventName.OUTBOX_MUTATION_PROCESSED.toString().equals(event.getName())) {
                return false;
            }
            return DataStoreHubEventFilters.hasModelData(model, (OutboxMutationEvent<? extends Model>) event.getData());
        };
    }

    /**
     * Watches for the receipt of a given model, from the cloud.
     * Creates a filter that catches events from the subscription processor.
     * Events will pass if they mention the provided model by its name and ID,
     * and state that it has successfully been received over a subscription.
     * @param model The model to watch for
     * @param <T> Type of that model
     * @return A filter that watches for receive of the provided model
     */
    public static <T extends Model> HubEventFilter receiptOf(T model) {
        return event -> {
            if (!DataStoreChannelEventName.SUBSCRIPTION_DATA_PROCESSED.toString().equals(event.getName())) {
                return false;
            }
            return DataStoreHubEventFilters.hasModelData(model, (ModelWithMetadata<? extends Model>) event.getData());
        };
    }

    private static <T extends Model> boolean hasModelData(
        T model, OutboxMutationEvent<? extends Model> mutationEvent) {
        if (mutationEvent == null) {
            return false;
        }
        if (!model.getClass().isAssignableFrom(mutationEvent.getModel())) {
            return false;
        }
        String actualId = mutationEvent.getElement().getModel().getId();
        return model.getId().equals(actualId);
    }

    private static <T extends Model> boolean hasModelData(
        T model, ModelWithMetadata<? extends Model> modelWithMetadata) {
        if (modelWithMetadata == null) {
            return false;
        } else if (!model.getClass().isAssignableFrom(modelWithMetadata.getModel().getClass())) {
            return false;
        }
        return model.getId().equals(modelWithMetadata.getModel().getId());
    }
}
