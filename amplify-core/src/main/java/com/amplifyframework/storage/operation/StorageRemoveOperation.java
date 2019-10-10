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

package com.amplifyframework.storage.operation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.core.async.AmplifyOperation;
import com.amplifyframework.core.async.AmplifyOperationRequest;
import com.amplifyframework.core.async.EventListener;
import com.amplifyframework.core.category.CategoryType;

/**
 * Desrcibes an operation being carried out to remove an item from storage.
 */
public class StorageRemoveOperation extends AmplifyOperation<AmplifyOperationRequest<?>> {

    /**
     * Constructs a new StorageRemoveOperation.
     * @param categoryType The category associated to this operation (Storage, duh?)
     * @param request Bundle of the parameters that were provided the remove API
     * @param eventListener A listener to be notified of events that occur during the
     *                      execution of this operation
     */
    public StorageRemoveOperation(@NonNull CategoryType categoryType,
                                  @NonNull AmplifyOperationRequest<?> request,
                                  @Nullable EventListener<?> eventListener) {
        super(categoryType, request, eventListener);
    }

    @Override
    public void start() {
    }
}

