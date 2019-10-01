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

package com.amplifyframework.api;

import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.plugin.Plugin;

/**
 * Abstract class to group Plugins for API subcategories.
 *
 * @param <C> Configuration for API category
 * @param <E> Escape Hatch for API category's low level client
 */
public abstract class ApiPlugin<C, E> implements Plugin<C, E> {
    @Override
    public final CategoryType getCategoryType() {
        return CategoryType.API;
    }

    /**
     * Returns whether this plugin implements REST API or GRAPHQL API
     *
     * @return API Type enum
     */
    abstract ApiType getApiType();
}
