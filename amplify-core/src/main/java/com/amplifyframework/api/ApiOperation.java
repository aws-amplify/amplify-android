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

import com.amplifyframework.core.async.AmplifyOperation;
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.category.CategoryType;

/**
 * Base operation type for the API category.
 * @param <R> Type of request being made
 */
public abstract class ApiOperation<R> extends AmplifyOperation<R> implements Cancelable {

    /**
     * Constructs a new ApiOperation.
     * @param request An operation request
     */
    public ApiOperation(final R request) {
        super(CategoryType.API, request);
    }
}

