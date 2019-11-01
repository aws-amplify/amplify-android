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

package com.amplifyframework.core;

/**
 * A listener which will be notified of a single result, or alternately,
 * a single error, if a result could not be obtained.  Exactly one of
 * {@see #onResult(R result)} or {@see onError(Throwable error)} is
 * expected to be called.
 *
 * See also the {@see StreamListener}, which expects to receive 0..n items
 * in its callback(s), instead of just a single result.
 *
 * @param <R> The type of result being returned to the listener
 */
public interface ResultListener<R> {

    /**
     * Called back when a result is available.
     * @param result A result object
     */
    void onResult(R result);

    /**
     * Called if a result cannot be obtained, because an
     * error has occurred.
     * @param error An error that prevents determination of a result.
     */
    void onError(Throwable error);
}
