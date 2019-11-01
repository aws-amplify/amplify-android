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
 * Every implementation of an {@link AmplifyOperation}
 * should be associated with a request object that
 * encapsulates the input to an operation.
 *
 * The implementation of an {@link AmplifyOperation} can
 * decide the type of the request object.
 * @param <R> the parameter type of the request. The implementation
 *           can define the type of the request object.
 */
public interface AmplifyOperationRequest<R> {
    /**
     * Return the request object.
     * 
     * @return the request object associated with
     *         the implementation of an
     *         {@link AmplifyOperation}.
     */
    R getRequest();
}
