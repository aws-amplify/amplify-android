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
import com.amplifyframework.datastore.events.OutboxStatusEvent;
import com.amplifyframework.hub.HubEventFilter;

final class TestHubEventFilters {
    private TestHubEventFilters() {}

    static <T extends Model> HubEventFilter isProcessed(T model) {
        return event -> {
            DataStoreChannelEventName eventName = DataStoreChannelEventName.fromString(event.getName());
            if (!DataStoreChannelEventName.OUTBOX_MUTATION_PROCESSED.equals(eventName)) {
                return false;
            }
            return hasModelData(model, (OutboxMutationEvent<? extends Model>) event.getData());
        };
    }

    static <T extends Model> HubEventFilter isEnqueued(T model) {
        return event -> {
            DataStoreChannelEventName eventName = DataStoreChannelEventName.fromString(event.getName());
            if (!DataStoreChannelEventName.OUTBOX_MUTATION_ENQUEUED.equals(eventName)) {
                return false;
            }
            return hasModelData(model, (OutboxMutationEvent<? extends Model>) event.getData());
        };
    }

    private static <T extends Model> boolean hasModelData(T model, OutboxMutationEvent<? extends Model> mutationEvent) {
        if (mutationEvent == null) {
            return false;
        }
        if (!model.getClass().getSimpleName().equals(mutationEvent.getModelName())) {
            return false;
        }
        String actualId = mutationEvent.getElement().getModel().getId();
        return model.getId().equals(actualId);
    }

    static HubEventFilter isOutboxEmpty(boolean isEmpty) {
        return event -> {
            DataStoreChannelEventName eventName = DataStoreChannelEventName.fromString(event.getName());
            if (!DataStoreChannelEventName.OUTBOX_STATUS.equals(eventName)) {
                return false;
            }
            OutboxStatusEvent status = (OutboxStatusEvent) event.getData();
            if (status == null) {
                return false;
            }
            return isEmpty == status.isEmpty();
        };
    }
}
