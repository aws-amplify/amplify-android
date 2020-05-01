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

package com.amplifyframework.core.category;

import androidx.annotation.NonNull;

/**
 * Convenience class to allow AmplifyConfiguration to return a
 * shell configuration for categories not present in the Amplify
 * config file.
 */
public final class EmptyCategoryConfiguration extends CategoryConfiguration {
    private final CategoryType categoryType;

    private EmptyCategoryConfiguration(CategoryType categoryType) {
        this.categoryType = categoryType;
    }

    public static EmptyCategoryConfiguration forCategoryType(CategoryType categoryType) {
        return new EmptyCategoryConfiguration(categoryType);
    }

    @NonNull
    @Override
    public CategoryType getCategoryType() {
        return categoryType;
    }
}
