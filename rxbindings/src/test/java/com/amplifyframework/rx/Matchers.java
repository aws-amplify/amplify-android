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

package com.amplifyframework.rx;

import androidx.annotation.NonNull;

import com.amplifyframework.core.Action;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.model.Model;

import org.mockito.ArgumentMatchers;

/**
 * Factories to create Mockito argument matchers for common Amplify types.
 * This is basically just pure syntax fluff.
 * This test utility facilitates two things:
 *
 *   1. Writing anyConsumer() instead of any(Consumer.class) etc.
 *   2. Making reference to a Consumer matcher without a checked exception
 */
final class Matchers {
    private Matchers() {}

    /**
     * Match any {@link Model}.
     * @param <T> Type of model
     * @return A matched model
     */
    @SuppressWarnings("unused")
    @NonNull
    static <T extends Model> T anyModel() {
        return ArgumentMatchers.any();
    }

    /**
     * Match any {@link Consumer}.
     * @param <T> Type accepted by consumer
     * @return A matched consumer
     */
    @NonNull
    static <T> Consumer<T> anyConsumer() {
        return ArgumentMatchers.any();
    }

    /**
     * Match any {@link Action}.
     * @return A matched Action
     */
    @NonNull
    static Action anyAction() {
        return ArgumentMatchers.any(Action.class);
    }
}
