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

package com.amplifyframework.datastore.storage;

import androidx.annotation.NonNull;

import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.datastore.SimpleModelProvider;
import com.amplifyframework.datastore.storage.sqlite.PersistentModelVersion;

/**
 * Creates a provide of system models, that are used internally by the DataStore.
 */
public final class SystemModelsProviderFactory {
    // CHANGE this models version whenever any system models are added/removed/updated.
    private static final String SYSTEM_MODELS_VERSION = "2fe6d84d-4772-4089-be16-e06d8469c537";

    @SuppressWarnings("checkstyle:all") private SystemModelsProviderFactory() {}

    @NonNull
    public static ModelProvider create() {
        return SimpleModelProvider.instance(
            SYSTEM_MODELS_VERSION,

            // StorageItemChange.Record.class is an internal system event
            // it is used to stage local storage changes for upload to cloud
            StorageItemChange.Record.class,

            // PersistentModelVersion.class is an internal system event
            // it is used to store the version of the ModelProvider
            PersistentModelVersion.class
        );
    }
}
