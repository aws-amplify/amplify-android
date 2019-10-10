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

package com.amplifyframework.storage.exception;

import com.amplifyframework.AmplifyException;

/**
 * Exception encountered in get API of Storage category.
 */
public class StorageGetException extends AmplifyException {

    private static final long serialVersionUID = 1L;

    /**
     * Specifies that exception was due to object not being found.
     */
    public static class NotFoundException extends StorageGetException {

        private static final long serialVersionUID = 1L;

        /**
         * Constructs a new NotFoundException with a default error message.
         */
        public NotFoundException() {
            super("No object is associated with provided key.");
        }

        /**
         * Constructs a new NotFoundException with a custom error message.
         * @param message Explains that an object was not found
         */
        public NotFoundException(String message) {
            super(message);
        }

        /**
         * Constructs a new NotFoundException associated to an error.
         * @param throwable An associated error
         */
        public NotFoundException(Throwable throwable) {
            super(throwable);
        }

        /**
         * Constructs a new NotFoundException using a provided error message,
         * and associated to a provided error.
         * @param message Explains that an object was not found
         * @param throwable An associated error
         */
        public NotFoundException(String message, Throwable throwable) {
            super(message, throwable);
        }
    }

    /**
     * Creates a new StorageGetException with the specified message, and root
     * cause.
     * @param message An error message describing why this exception was thrown.
     * @param throwable The underlying cause of this exception.
     */
    public StorageGetException(final String message, final Throwable throwable) {
        super(message, throwable);
    }

    /**
     * Creates a new StorageGetException with the specified message.
     * @param message An error message describing why this exception was thrown.
     */
    public StorageGetException(final String message) {
        super(message);
    }

    /**
     * Creates a new StorageGetException with the root cause.
     * @param throwable The underlying cause of this exception.
     */
    public StorageGetException(final Throwable throwable) {
        super(throwable);
    }
}
