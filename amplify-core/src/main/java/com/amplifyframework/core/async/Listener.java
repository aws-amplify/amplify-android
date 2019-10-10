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
 * Listener for async operations. This listener can be
 * used in cases where a result (for success) and error
 * (for failure) need to be reported for an asynchronous
 * operation.
 *
 * @param <R> the parameter type of the result object.
 */
public interface Listener<R> {

    /**
     * Listener method for reporting success
     * of an operation.
     * @param result represents the object for success
     */
    void onResult(R result);

    /**
     * Listener method for reporting failure
     * of an operation.
     * @param error The error that occurred
     */
    void onError(Exception error);
}
