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

import com.amplifyframework.core.Action;

/**
 * An action which does nothing, when called.
 */
public final class EmptyAction implements Action {
    private EmptyAction() {}

    @Override
    public void call() {
    }

    /**
     * Creates a new instance of an {@link EmptyAction}.
     * @return A new {@link EmptyAction}
     */
    @NonNull
    public static EmptyAction create() {
        return new EmptyAction();
    }
}
