/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import androidx.annotation.Nullable;

/**
 * A consumer of a nullable value type.
 * @param <T> Type of thing being consumed
 */
@SuppressWarnings("EmptyMethod") // Lint looks for class impl, not lambda (as almost all uses are)
public interface NullableConsumer<T> {

    /**
     * Accept a value.
     * @param value A value
     */
    void accept(@Nullable T value);
}
