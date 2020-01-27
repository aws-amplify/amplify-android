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

package com.amplifyframework.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * The result of a component's initialization.
 */
public final class InitializationResult {
    private final Throwable failure;
    private final InitializationStatus initializationStatus;

    private InitializationResult(@NonNull InitializationStatus initializationStatus, @Nullable Throwable failure) {
        this.initializationStatus = initializationStatus;
        this.failure = failure;
    }

    /**
     * Gets the initialization status, e.g. {@link InitializationStatus#SUCCEEDED}.
     * @return The initialization status
     */
    @SuppressWarnings("unused")
    @NonNull
    public InitializationStatus getInitializationStatus() {
        return initializationStatus;
    }

    /**
     * Gets the initialization failure, if present.
     * @return Initialization failure; null, if none occurred
     */
    @Nullable
    public Throwable getFailure() {
        return failure;
    }

    /**
     * Constructs an initialization result that notes a failure occurred.
     * @param failure The failure that interrupted initialization
     * @return An initialization result
     */
    @NonNull
    public static InitializationResult failure(@NonNull Throwable failure) {
        Objects.requireNonNull(failure);
        return new InitializationResult(InitializationStatus.FAILED, failure);
    }

    /**
     * Constructs an Initialization result that notes a success occurred.
     * @return A successful initialization result.
     */
    @NonNull
    public static InitializationResult success() {
        return new InitializationResult(InitializationStatus.SUCCEEDED, null);
    }

    /**
     * Checks if the result is a success result.
     * @return True if the result is success, false otherwise
     */
    public boolean isSuccess() {
        return InitializationStatus.SUCCEEDED.equals(initializationStatus);
    }

    /**
     * Checks if the result is a failure.
     * @return True if the result is a failure.
     */
    public boolean isFailure() {
        return InitializationStatus.FAILED.equals(initializationStatus);
    }
}
