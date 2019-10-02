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

package com.amplifyframework.core.async;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.category.CategoryTypeable;
import com.amplifyframework.core.exception.AmplifyRuntimeException;
import com.amplifyframework.hub.HubFilter;
import com.amplifyframework.hub.HubFilters;
import com.amplifyframework.hub.HubListener;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubPayload;
import com.amplifyframework.hub.UnsubscribeToken;

import java.util.UUID;

/**
 * An abstract representation of an Amplify unit of work. Subclasses may aggregate multiple work items
 * to fulfill a single "AmplifyOperation", such as an "extract text operation" which might include
 * uploading an image to cloud storage, processing it via a Predictions engine, and translating the results.
 *
 * AmplifyOperations are used by plugin developers to perform tasks on behalf of the calling app. They have a default
 * implementation of a `dispatch` method that sends a contextualized payload to the Hub.
 *
 * Pausable/resumable tasks that do not require Hub dispatching should use {@link AsyncOperation} instead.
 */
public abstract class AmplifyOperation
        <R extends AmplifyOperationRequest,
                InProcess,
                Completed,
                E extends AmplifyRuntimeException>
        implements AsyncOperation, CategoryTypeable {

    /// Incoming parameters of the original request
    private R request;

    /// The unique ID of the operation. In categories where operations are persisted for future processing, this id can
    /// be used to identify previously-scheduled work for progress tracking or other functions.
    private UUID operationId;

    /// All AmplifyOperations must be associated with an Amplify Category
    private CategoryType categoryType;

    /// All AmplifyOperations must declare a HubPayloadEventName
    private String eventName;

    /// Token used to unsubscribe the listener attached to the operation
    private UnsubscribeToken unsubscribeToken;

    @Override
    public CategoryType getCategoryType() {
        return categoryType;
    }

    public AmplifyOperation(@NonNull final CategoryType categoryType,
                            @NonNull final String eventName,
                            @NonNull final R request,
                            @Nullable final EventListener eventListener) {
        this.categoryType = categoryType;
        this.eventName = eventName;
        this.request = request;
        this.operationId = UUID.randomUUID();
        if (eventListener != null) {
            this.unsubscribeToken = listen(eventListener);
        }
    }

    public UnsubscribeToken listen(@NonNull final EventListener eventListener) {
        HubChannel channel = HubChannel.fromCategoryType(categoryType);
        HubFilter filterByOperationId = HubFilters.hubFilter(this);
        HubListener hubListener = new HubListener() {
            @Override
            public void onHubEvent(@NonNull HubPayload payload) {
                if (payload.data instanceof AsyncEvent) {
                    eventListener.onEvent((AsyncEvent) payload.data);
                }
            }
        };

        return Amplify.Hub.listen(channel, filterByOperationId, hubListener);
    }

    /**
     * Dispatches an event to the hub. Internally, creates an `AmplifyOperationContext` object
     * from the operation's `id`, and `request`
     * Parameter event: The AsyncEvent to dispatch to the hub as part of the HubPayload
     */
    public void dispatch(@NonNull final AsyncEvent event) {
        HubChannel channel = HubChannel.fromCategoryType(categoryType);
        AmplifyOperationContext context = new AmplifyOperationContext(operationId, request);
        HubPayload payload = new HubPayload(eventName, context, event);
        Amplify.Hub.dispatch(channel, payload);
    }

    public void removeListener() {
        Amplify.Hub.removeListener(this.unsubscribeToken);
    }

    public abstract void start();
}
