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
import com.amplifyframework.core.stream.AmplifyObservable;
import com.amplifyframework.core.stream.Observer;

/**
 * Base observable type for the API category.
 * At the time of implementation, Amplify API category
 * only supports one type of operation that returns an
 * observable object.
 * See {@link ApiCategoryBehavior#subscribe(String, String, Class, Observer)}.
 * @param <T> data type of emitted items or events
 */
public abstract class ApiObservable<T> extends AmplifyObservable<T> {
    /**
     * Constructs a new instance of API Observable for subscription.
     */
    public ApiObservable() {
        super(CategoryType.API);
    }
}
