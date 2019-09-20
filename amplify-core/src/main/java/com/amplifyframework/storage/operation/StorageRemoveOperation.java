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

import android.support.annotation.NonNull;

import com.amplifyframework.core.async.AsyncOperation;
import com.amplifyframework.core.async.Callback;
import com.amplifyframework.core.plugin.Plugin;
import com.amplifyframework.core.task.Options;
import com.amplifyframework.core.task.Result;

public class StorageRemoveOperation implements AsyncOperation {
    @Override
    public StorageRemoveOperation options(@NonNull Options options) {
        return this;
    }

    @Override
    public StorageRemoveOperation plugin(Class<? extends Plugin> pluginClass) {
        return null;
    }

    @Override
    public StorageRemoveOperation start() {
        return this;
    }

    @Override
    public StorageRemoveOperation pause() {
        return this;
    }

    @Override
    public StorageRemoveOperation resume() {
        return this;
    }

    @Override
    public StorageRemoveOperation cancel() {
        return this;
    }
}
