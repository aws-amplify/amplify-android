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

package com.amplifyframework.datastore.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Utility class to provide helpful functions to use on throwable instances.
 */
public final class ErrorInspector {
    private ErrorInspector() {}

    /**
     * Returns true if an error object was caused by a specific throwable type.
     * @param error Error object to perform the check on.
     * @param causeType Class type of the error to look for in stacktrace.
     * @return true if an error object contains given cause.
     */
    public static boolean contains(
        @Nullable Throwable error,
        @NonNull Class<? extends Throwable> causeType
    ) {
        Objects.requireNonNull(causeType);
        if (error == null) {
            return false;
        }
        try {
            return causeType.isInstance(error) || contains(error.getCause(), causeType);
        } catch (Throwable unexpected) {
            // May encounter unexpected error during recursive search.
            // e.g. StackOverflowError, NoClassDefFoundError, etc.
            return false;
        }
    }

    /**
     * Returns true if an error object was caused by a specific throwable type.
     * @param error Error object to perform the check on.
     * @param causeTypeList Class type of the error to look for in stacktrace.
     * @return true if an error object contains given cause.
     */
    public static boolean contains(
            @Nullable Throwable error,
            @NonNull List<Class<? extends Throwable>> causeTypeList) {
        Objects.requireNonNull(causeTypeList);
        if (error == null) {
            return false;
        }
        try {
            for (Class<? extends Throwable> causeType : causeTypeList) {
                if (contains(error, causeType)) {
                    return true;
                }
            }
        } catch (Throwable unexpected) {
            // May encounter unexpected error during recursive search.
            // e.g. StackOverflowError, NoClassDefFoundError, etc.
            return false;
        }
        return false;
    }
}
