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

package com.amplifyframework.storage.s3;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.RawRes;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.AmplifyConfiguration;
import com.amplifyframework.core.category.CategoryConfiguration;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.storage.StorageCategory;

import java.util.Objects;

/**
 * Factory for creating {@link StorageCategory} instance suitable for test.
 */
final class TestStorageCategory {
    private TestStorageCategory() {}

    /**
     * Creates an instance of {@link StorageCategory} using the provided configuration resource.
     * @param context Android Context
     * @param resourceId Android resource ID for a configuration file
     * @return A StorageCategory instance using the provided configuration
     */
    static StorageCategory create(@NonNull Context context, @RawRes int resourceId) {
        Objects.requireNonNull(context);

        final StorageCategory storageCategory = new StorageCategory();
        try {
            storageCategory.addPlugin(new AWSS3StoragePlugin());
            CategoryConfiguration storageConfiguration = AmplifyConfiguration.fromConfigFile(context, resourceId)
                .forCategoryType(CategoryType.STORAGE);
            storageCategory.configure(storageConfiguration, context);
            // storageCategory.initialize(context); // Doesn't do anything right now.
        } catch (AmplifyException initializationFailure) {
            throw new RuntimeException(initializationFailure);
        }
        return storageCategory;
    }
}
