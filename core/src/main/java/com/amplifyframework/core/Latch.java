/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
 * Executes a latched task and awaits its completion.
 * @param <E> Type of exception thrown upon encountering error
 */
public interface Latch<E extends Exception> {
    /**
     * Start the latched task.
     */
    void start();

    /**
     * Await the completion of latched task.
     * @throws E When task fails or is interrupted
     */
    void await() throws E;
}
