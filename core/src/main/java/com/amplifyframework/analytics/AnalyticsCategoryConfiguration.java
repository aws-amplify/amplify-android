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

package com.amplifyframework.analytics;

import com.amplifyframework.core.category.CategoryConfiguration;
import com.amplifyframework.core.category.CategoryType;

/**
 * Configuration for Analytics category that also
 * contains configuration for individual plugins.
 */
public final class AnalyticsCategoryConfiguration extends CategoryConfiguration {
    //TODO: Place global (category-wise) configuration options here

    /**
     * Constructs a new AnalyticsCategoryConfiguration.
     */
    public AnalyticsCategoryConfiguration() {
        super();
    }

    /**
     * Gets the category type associated with the current object.
     *
     * @return The category type to which the current object is affiliated
     */
    @Override
    public CategoryType getCategoryType() {
        return CategoryType.ANALYTICS;
    }
}
