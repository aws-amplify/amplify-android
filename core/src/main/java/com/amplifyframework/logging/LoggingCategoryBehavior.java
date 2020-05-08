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

package com.amplifyframework.logging;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.core.category.CategoryType;

/**
 * Defines the client behavior (client API) consumed
 * by the app for collection and sending of Analytics
 * events.
 */
public interface LoggingCategoryBehavior {
    /**
     * Gets the default logger used by the Amplify framework.
     * @return Default logger
     */
    @NonNull
    Logger getDefaultLogger();

    /**
     * Gets a logger that is configured to emit logs against the provided namespace.
     * Logs will only be emitted if they are at or above the provided threshold.
     * Passing {@link LogLevel#VERBOSE} results in all logs being emitted. Passing
     * a value of {@link LogLevel#NONE} will result in no log(s) being emitted.
     * @param namespace A namespace for all logs emitted by the returned logger instance
     * @param threshold Only logs at or above this log level will be emitted
     * @return A logger that emits logs in a given namespace, at or above the provide threshold
     */
    @NonNull
    Logger forNamespaceAndThreshold(@Nullable String namespace, @Nullable LogLevel threshold);

    /**
     * Gets a logger configured to emit logs against a particular namespace.
     * The log threshold will be {@link LogLevel#INFO}. To use a different level,
     * see {@link #forNamespaceAndThreshold(String, LogLevel)}.
     * @param namespace A namespace for all logs emitted by the returned logger instance
     * @return A logger that emits logs in the provided namespace
     */
    @NonNull
    Logger forNamespace(@Nullable String namespace);

    /**
     * Gets a logger configured to emit logs against a particular category namespace.
     *
     * The log threshold will be {@link LogLevel#INFO}. To use a different level,
     * see {@link #forNamespaceAndThreshold(String, LogLevel)}.
     *
     * @param categoryType the category type that will be used as namespace
     * @return A logger that emits logs in the provided category type namespace
     */
    @NonNull
    Logger forCategory(@NonNull CategoryType categoryType);

}
