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

package com.amplifyframework.hub;

import androidx.annotation.NonNull;

import com.amplifyframework.core.category.CategoryConfiguration;
import com.amplifyframework.core.category.CategoryType;

/**
 * Configuration for the Hub category, that also
 * references configuration(s) for its managed plugins.
 */
public final class HubCategoryConfiguration extends CategoryConfiguration {
    //TODO: Place global (category-wise) configuration options here

    /**
     * Constructs a new HubCategoryConfiguration instance.
     */
    public HubCategoryConfiguration() {
        super();
    }

    /**
     * Gets the category type associated with the current object.
     *
     * @return The category type to which the current object is affiliated
     */
    @NonNull
    @Override
    public CategoryType getCategoryType() {
        return CategoryType.HUB;
    }
}
