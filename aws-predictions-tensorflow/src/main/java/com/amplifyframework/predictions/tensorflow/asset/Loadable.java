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

package com.amplifyframework.predictions.tensorflow.asset;

import androidx.annotation.Nullable;

import com.amplifyframework.core.Action;
import com.amplifyframework.core.Consumer;

/**
 * Defines the behavior of a loadable resource.
 * @param <V> the data type of the resource being loaded
 * @param <E> the error type for this resource
 */
public interface Loadable<V, E extends Exception> {
    /**
     * Begin loading the resources.
     */
    void load();

    /**
     * Free up the loaded resources.
     */
    void unload();

    /**
     * Return true if the resources are fully loaded.
     * @return true if the resources are fully loaded
     */
    boolean isLoaded();

    /**
     * Sets the consumer of the loaded value to be
     * triggered upon load completion.
     * @param onLoaded the consumer of the loaded value
     * @return this loadable instance for chaining
     */
    Loadable<V, E> onLoaded(Consumer<V> onLoaded);

    /**
     * Sets the consumer of an error which will be triggered
     * upon encountering an error while loading/unloading.
     * @param onError the consumer of the thrown exception
     * @return this loadable instance for chaining
     */
    Loadable<V, E> onError(Consumer<E> onError);

    /**
     * Sets the action item to be triggered upon unload
     * completion.
     * @param onUnloaded the action to invoke upon unload
     * @return this loadable instance for chaining
     */
    Loadable<V, E> onUnloaded(Action onUnloaded);

    /**
     * Gets the loaded value. Null if the resource was
     * not loaded or fully loaded yet.
     * @return the loaded value
     */
    @Nullable
    V value();
}
