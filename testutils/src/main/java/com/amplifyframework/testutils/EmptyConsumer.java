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

import androidx.annotation.NonNull;

import com.amplifyframework.core.Consumer;

/**
 * An {@link Consumer} which does nothing when a value is accepted.
 */
@SuppressWarnings("checkstyle:WhitespaceAround")
public final class EmptyConsumer {
    private EmptyConsumer() {}

    /**
     * Creates a consumer of a value of a given class.
     * @param clazz Class of value to consume
     * @param <T> Type of value to consume
     * @return A consumer of the value type, which does nothing on accept.
     */
    @NonNull
    public static <T> Consumer<T> of(@NonNull Class<T> clazz) {
        return value -> {};
    }
}
