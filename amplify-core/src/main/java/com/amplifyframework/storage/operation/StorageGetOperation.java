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
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.async.EventListener;
import com.amplifyframework.core.async.Resumable;
import com.amplifyframework.core.category.CategoryType;

/**
 * An operation to get an item from storage.
 */
public class StorageGetOperation extends AmplifyOperation<AmplifyOperationRequest<?>>
        implements Resumable, Cancelable {

    /**
     * Constructs a new StorageGetOperation.
     * @param categoryType The category type to which this operation is bound (Storage, duh!)
     * @param request A bundling of the parameters that were passed to the Storage get API
     * @param eventListener A listener that will be notified when events occur during
     *                      the execution of this operation
     */
    public StorageGetOperation(@NonNull CategoryType categoryType,
                               @NonNull AmplifyOperationRequest<?> request,
                               @Nullable EventListener<?> eventListener) {
        super(categoryType, request, eventListener);
    }

    @Override
    public void start() {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void cancel() {
    }
}
