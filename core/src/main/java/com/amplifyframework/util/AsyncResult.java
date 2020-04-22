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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A simple class which starts empty and will indicate if it's ever been set with a value and if so, what that value is.
 * @param <T> The type of result stored in this object.
 */
public class AsyncResult<T> {
    private T result;
    private boolean set = false;

    /**
     * Set a value for this result object.
     * @param result The value you want to set this object to - can be null.
     */
    public void set(@Nullable T result) {
        set = true;
        this.result = result;
    }

    /**
     * Returns the value of this object - null if it was never set.
     * @return the value of this object - null if it was never set.
     */
    @Nullable
    public T get() {
        return result;
    }

    /**
     * Returns true if a value was ever set for this object (including null), false otherwise.
     * @return true if a value was ever set for this object (including null), false otherwise
     */
    @NonNull
    public boolean isSet() {
        return set;
    }
}
