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

/**
 * Defines the client behavior (client API) consumed
 * by the app for collection and sending of Analytics
 * events.
 */
public interface LoggingCategoryBehavior {
    /**
     * Gets a logger configured to emit logs against a particular namespace.
     * @param namespace A namespace for all logs emitted by the returned logger instance
     * @return A logger that emits logs in the provided namespace
     */
    @NonNull
    Logger forNamespace(@Nullable String namespace);
}
