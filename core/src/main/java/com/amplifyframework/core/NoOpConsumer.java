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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * An {@link Consumer} that does nothing when accepting a value.
 * @param <T> Type of value accepted by the consumer
 */
public final class NoOpConsumer<T> implements Consumer<T> {
    private NoOpConsumer() {}

    /**
     * Creates an instance of an {@link Consumer} which does nothing
     * when accepting value.
     * @param <T> Type of value that the consumer will accept
     * @return A No-operation consumer
     */
    @NonNull
    public static <T> NoOpConsumer<T> create() {
        return new NoOpConsumer<>();
    }

    @Override
    public void accept(@NonNull T value) {}

    @Override
    public int hashCode() {
        return NoOpConsumer.class.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof NoOpConsumer;
    }

    @NonNull
    @Override
    public String toString() {
        return "NoOpConsumer {}";
    }
}
