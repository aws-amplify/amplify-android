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

import org.json.JSONException;
import org.json.JSONObject;

final class FakeCategoryConfiguration extends CategoryConfiguration {
    private final CategoryType categoryType;

    private FakeCategoryConfiguration(CategoryType categoryType) {
        this.categoryType = categoryType;
    }

    static FakeCategoryConfiguration instance(CategoryType categoryType, JSONObject json) {
        FakeCategoryConfiguration config = new FakeCategoryConfiguration(categoryType);
        try {
            config.populateFromJSON(json);
        } catch (JSONException jsonException) {
            throw new RuntimeException(jsonException);
        }
        return config;
    }

    static FakeCategoryConfiguration instance(CategoryType categoryType) {
        return instance(categoryType, new JSONObject());
    }

    @NonNull
    @Override
    public CategoryType getCategoryType() {
        return categoryType;
    }
}
