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
package com.amplifyframework.datastore

import com.amplifyframework.core.model.Model
import com.amplifyframework.datastore.appsync.ModelWithMetadata
import com.amplifyframework.datastore.events.NetworkStatusEvent
import com.amplifyframework.datastore.syncengine.OutboxMutationEvent
import com.amplifyframework.hub.HubEvent
import com.amplifyframework.hub.HubEventFilter

/**
 * Utility to create some common filters that can be applied to hub subscriptions.
 */
object DataStoreHubEventFilters {
    /**
     * Watches for publication (out of mutation queue) of a given model.
     * Creates a filter that catches events from the mutation processor.
     * Events will pass if they mention the provided model by its name and ID,
     * and state that it has successfully been published off of the mutation queue.
     * @param modelName Model name, e.g. "Post"
     * @param modelId The ID of a model instance that might be published
     * @return A filter that watches for publication of the provided model.
     */
    @JvmStatic
    fun publicationOf(modelName: String, modelId: String): HubEventFilter {
        return outboxEventOf(
            DataStoreChannelEventName.OUTBOX_MUTATION_PROCESSED,
            modelName,
            modelId
        )
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
    fun enqueueOf(modelName: String, modelId: String): HubEventFilter {
        return outboxEventOf(
            DataStoreChannelEventName.OUTBOX_MUTATION_ENQUEUED,
            modelName,
            modelId
        )
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
    private fun outboxEventOf(
        eventType: DataStoreChannelEventName,
        modelName: String,
        modelId: String
    ): HubEventFilter {
        return HubEventFilter { event: HubEvent<*> ->
            if (eventType.toString() != event.name) {
                return@HubEventFilter false
            }
            if (event.data !is OutboxMutationEvent<*>) {
                return@HubEventFilter false
            }
            val outboxMutationEvent = event.data as OutboxMutationEvent<out Model>
            modelId == outboxMutationEvent.element.model
                .primaryKeyString && modelName == outboxMutationEvent.modelName
        }
    }

    @JvmStatic
    fun <T: Model>filterOutboxEvent(
        eventType: DataStoreChannelEventName,
        filter: (model: Model) -> Boolean): HubEventFilter {
        return HubEventFilter { event: HubEvent<*> ->
            if (eventType.toString() != event.name) {
                return@HubEventFilter false
            }
            if (event.data !is OutboxMutationEvent<*>) {
                return@HubEventFilter false
            }
            val outboxMutationEvent = event.data as OutboxMutationEvent<out Model>
            val model = outboxMutationEvent.element.model
            return@HubEventFilter (model)?.let {
                filter(it)
            } ?: false

        }
    }

    /**
     * Watches for the receipt of a given model, from the cloud.
     * Creates a filter that catches events from the subscription processor.
     * Events will pass if they mention the provided model by its name and ID,
     * and state that it has successfully been received over a subscription.
     * @param modelId ID of the model instance that may be received
     * @return A filter that watches for receive of the provided model
     */
    @JvmStatic
    fun receiptOf(modelId: String): HubEventFilter {
        return HubEventFilter { event: HubEvent<*> ->
            if (DataStoreChannelEventName.SUBSCRIPTION_DATA_PROCESSED.toString() != event.name) {
                return@HubEventFilter false
            }
            if (event.data !is ModelWithMetadata<*>) {
                return@HubEventFilter false
            }
            val modelWithMetadata = event.data as ModelWithMetadata<out Model>
            modelId == modelWithMetadata.model.resolveIdentifier()
        }
    }

    /**
     * Expect a network status failure event to be emitted by the sync engione.
     * @return A filter that checks for network failure messages.
     */
    @JvmStatic
    fun networkStatusFailure(): HubEventFilter {
        return HubEventFilter { event: HubEvent<*> ->
            if (DataStoreChannelEventName.NETWORK_STATUS.toString() != event.name) {
                return@HubEventFilter false
            }
            if (event.data !is NetworkStatusEvent) {
                return@HubEventFilter false
            }
            val outboxMutationEvent = event.data as NetworkStatusEvent?
            !outboxMutationEvent!!.active
        }
    }
}