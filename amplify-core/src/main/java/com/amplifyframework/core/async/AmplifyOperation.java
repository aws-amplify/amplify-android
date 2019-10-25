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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.core.category.CategoryType;

import java.util.UUID;

/**
 * An abstract representation of an Amplify unit of work. Subclasses may aggregate multiple work items
 * to fulfill a single "AmplifyOperation", such as an "extract text operation" which might include
 * uploading an image to cloud storage, processing it via a Predictions engine, and translating the results.
 *
 * AmplifyOperations are used by plugin developers to perform tasks on behalf of the calling app. They have a default
 * implementation of a `publish` method that sends a contextualized payload to the Hub.
 *
 * Pausable/resumable tasks that do not require Hub dispatching should use {@link AsyncOperation} instead.
 */
public abstract class AmplifyOperation<R> implements AsyncOperation {

    // The unique ID of the operation. In categories where operations are persisted for future
    // processing, this id can be used to identify previously-scheduled work for progress tracking
    // or other functions.
    private final UUID operationId;

    // Required by Hub to find the HubChannel mapped to the
    // CategoryType.
    private final CategoryType categoryType;

    // Reference to the request object of the operation. The
    // request object encapsulates the input parameters to an
    // operation.
    private final R amplifyOperationRequest;

    /**
     * Constructs a new AmplifyOperation.
     * @param categoryType The category in which this operation is
     *                     fulfilling a request
     * @param amplifyOperationRequest The request object of the operation
     */
    public AmplifyOperation(@NonNull final CategoryType categoryType,
                            @Nullable final R amplifyOperationRequest) {
        this.categoryType = categoryType;
        this.operationId = UUID.randomUUID();
        this.amplifyOperationRequest = amplifyOperationRequest;
    }

    /**
     * Gets the ID of the operation.
     * @return Operation unique ID
     */
    public final UUID getOperationId() {
        return operationId;
    }

    /**
     * Gets the category type.
     * @return Category type
     */
    public final CategoryType getCategoryType() {
        return categoryType;
    }
}

