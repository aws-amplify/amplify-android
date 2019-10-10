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

package com.amplifyframework.core.async;

/**
 * Describes the parameters that are passed during the creation of an AmplifyOperation.
 * @param <R> The concrete type that adjusts the behavior of the request type.
 *                        Any operation request object that derives from AmplifyOperationRequest
 *                        will define the type and structure of the options. The options is used
 *                        to specify the details of a request.
 */
public abstract class AmplifyOperationRequest<R> {
    private final R requestOptions;

    /**
     * Constructs a new AmplifyOperationRequest.
     * @param requestOptions A bundle describing the paramters that were passed
     *                       when an operation was requested. This may be a POJO
     *                       which bundles arguments passed to an amplify Java method.
     */
    protected AmplifyOperationRequest(R requestOptions) {
        this.requestOptions = requestOptions;
    }

    /**
     * Options to adjust the behavior of this request, including plugin options.
     * @return the options object
     */
    abstract R getRequestOptions();
}
