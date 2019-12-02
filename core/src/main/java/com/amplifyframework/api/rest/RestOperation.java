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

package com.amplifyframework.api.rest;

import com.amplifyframework.api.ApiOperation;

/**
 * A RestOperation is an API operation which returns a REST results.
 */
public abstract class RestOperation extends ApiOperation<RestOperationRequest> {

    /**
     * Constructs a new REST ApiOperation.
     *
     * @param request An operation request
     */
    public RestOperation(RestOperationRequest request) {
        super(request);
    }
}
