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

package com.amplifyframework.testutils;

/**
 * A test utility to sleep the thread of execution.
 * This exists so that we don't have to catch {@link InterruptedException} all over the place,
 * cluttering our test code.
 */
public final class Sleep {
    @SuppressWarnings("checkstyle:all") private Sleep() {}

    /**
     * Sleeps the thread of execution for a duration of milliseconds.
     * @param duration Duration of time to sleep
     * @throws RuntimeException If thread is interrupted while sleeping
     */
    public static void milliseconds(long duration) throws RuntimeException {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException interruptedException) {
            throw new RuntimeException(interruptedException);
        }
    }
}
