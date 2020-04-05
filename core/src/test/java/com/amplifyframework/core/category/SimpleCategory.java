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

import com.amplifyframework.core.plugin.Plugin;

final class SimpleCategory<T> extends Category<Plugin<T>> {
    private final CategoryType categoryType;

    private SimpleCategory(@NonNull CategoryType categoryType) {
        this.categoryType = categoryType;
    }

    public static Category<Plugin<Void>> type(CategoryType categoryType) {
        return new SimpleCategory<>(categoryType);
    }

    @NonNull
    @Override
    public CategoryType getCategoryType() {
        return categoryType;
    }
}
