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

package com.amplifyframework.util;

/**
 * An almost-worthless wrapper to avoid writing {@link System#currentTimeMillis()} all
 * over the place. Instead, what we mean is "the current time," or Time.now().
 */
public final class Time {
    private Time() {}

    /**
     * Gets the current time, expressed in a duration of milliseconds since the epoch.
     * @return Current time in ms since epoch
     */
    public static long now() {
        return System.currentTimeMillis();
    }
}
