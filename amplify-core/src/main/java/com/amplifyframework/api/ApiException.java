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

package com.amplifyframework.api;

import com.amplifyframework.AmplifyRuntimeException;

/**
 * Base exception type for any exception that is raised from within the
 * API category.
 */
public class ApiException extends AmplifyRuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new AmplifyRuntimeException with the specified message, and root
     * cause.
     * @param message An error message describing why this exception was thrown.
     * @param throwable The underlying cause of this exception.
     */
    public ApiException(String message, Throwable throwable) {
        super(message, throwable);
    }

    /**
     * Creates a new AmplifyRuntimeException with the specified message.
     * @param message An error message describing why this exception was thrown.
     */
    public ApiException(String message) {
        super(message);
    }

    /**
     * Create an AmplifyRuntimeException with an exception cause.
     * @param throwable the cause of the exception.
     */
    public ApiException(Throwable throwable) {
        super(throwable);
    }
}