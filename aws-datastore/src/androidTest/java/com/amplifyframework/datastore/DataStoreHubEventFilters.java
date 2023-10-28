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
import com.amplifyframework.datastore.events.NetworkStatusEvent;
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
     * @param modelName Model name, e.g. "Post"
     * @param modelId The ID of a model instance that might be published
     * @return A filter that watches for publication of the provided model.
     */
    public static HubEventFilter publicationOf(String modelName, String modelId) {
        return outboxEventOf(
                DataStoreChannelEventName.OUTBOX_MUTATION_PROCESSED,
                modelName,
                modelId
        );
    }

    /**
     * Watches for enqueue (out of mutation queue) of a given model.
     * Creates a filter that catches events from the mutation processor.
     * Events will pass if they mention the provided model by its name and ID,
     * and state that it has successfully been enqueued off of the mutation queue.
     * @param modelName Model name, e.g. "Post"
     * @param modelId The ID of a model instance that might be published
     * @return A filter that watches for publication of the provided model.
     */
    public static HubEventFilter enqueueOf(String modelName, String modelId) {
        return outboxEventOf(
                DataStoreChannelEventName.OUTBOX_MUTATION_ENQUEUED,
                modelName,
                modelId
        );
    }

    /**
     * Watches for the passed event (out of mutation queue) of a given model.
     * Creates a filter that catches events from the mutation processor.
     * Events will pass if they mention the provided model by its name and ID,
     * and state that it has successfully received passed event type off of the mutation queue.
     * @param eventType Either OUTBOX_MUTATION_ENQUEUED or OUTBOX_MUTATION_PROCESSED
     * @param modelName Model name, e.g. "Post"
     * @param modelId The ID of a model instance that might be published
     * @return A filter that watches for publication of the provided model.
     */
    private static HubEventFilter outboxEventOf(
            DataStoreChannelEventName eventType,
            String modelName,
            String modelId
    ) {
        return event -> {
            if (!eventType.toString().equals(event.getName())) {
                return false;
            }
            if (!(event.getData() instanceof OutboxMutationEvent)) {
                return false;
            }
            OutboxMutationEvent<? extends Model> outboxMutationEvent =
                    (OutboxMutationEvent<? extends Model>) event.getData();

            return modelId.equals(outboxMutationEvent.getElement().getModel().getPrimaryKeyString()) &&
                    modelName.equals(outboxMutationEvent.getModelName());
        };
    }

    /**
     * Watches for the receipt of a given model, from the cloud.
     * Creates a filter that catches events from the subscription processor.
     * Events will pass if they mention the provided model by its name and ID,
     * and state that it has successfully been received over a subscription.
     * @param modelId ID of the model instance that may be received
     * @return A filter that watches for receive of the provided model
     */
    public static HubEventFilter receiptOf(String modelId) {
        return event -> {
            if (!DataStoreChannelEventName.SUBSCRIPTION_DATA_PROCESSED.toString().equals(event.getName())) {
                return false;
            }
            if (!(event.getData() instanceof ModelWithMetadata)) {
                return false;
            }
            ModelWithMetadata<? extends Model> modelWithMetadata =
                (ModelWithMetadata<? extends Model>) event.getData();
            return modelId.equals(modelWithMetadata.getModel().resolveIdentifier());
        };
    }

    /**
     * Expect a network status failure event to be emitted by the sync engione.
     * @return A filter that checks for network failure messages.
     */
    public static HubEventFilter networkStatusFailure() {
        return event -> {
            if (!DataStoreChannelEventName.NETWORK_STATUS.toString().equals(event.getName())) {
                return false;
            }
            if (!(event.getData() instanceof NetworkStatusEvent)) {
                return false;
            }
            NetworkStatusEvent outboxMutationEvent = (NetworkStatusEvent) event.getData();

            return !outboxMutationEvent.getActive();
        };
    }
}
