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
 * A plugin which implements the required behavior of the API category.
 * @param <E> The class type of the escape hatch which a subclass shall
 *            make available, to perform low-level
 *            implementation-specific operations using API domain
 *            objects. As an example, this might be an HTTP client
 *            interface, such as OkHttpClient.
 */
public abstract class ApiPlugin<E> implements ApiCategoryBehavior, Plugin<E> {
    @Override
    public final CategoryType getCategoryType() {
        return CategoryType.API;
    }
}

