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
 * Exception encountered in list API of Storage category.
 */
public class StorageListException extends AmplifyException {

    private static final long serialVersionUID = 1L;

    /**
     * Specifies that access to storage was denied.
     */
    public static class AccessDeniedException extends StorageListException {

        private static final long serialVersionUID = 1L;

        /**
         * Constructs a new AccessDeniedException using a default error message.
         */
        public AccessDeniedException() {
            super("Access to storage denied.");
        }

        /**
         * Constructs a new AccessDeniedException using a provided error message.
         * @param message Explains that access to a storage resource was denied
         */
        public AccessDeniedException(String message) {
            super(message);
        }

        /**
         * Constructs a new AccessDeniedException associated to an error.
         * @param throwable An error associated to this exception
         */
        public AccessDeniedException(Throwable throwable) {
            super(throwable);
        }

        /**
         * Constructs a new AccessDeniedException using a provided message and
         * associated to a provided error.
         * @param message Explains that access to a storage resource was denied
         * @param throwable An error associated with this exception
         */
        public AccessDeniedException(String message, Throwable throwable) {
            super(message, throwable);
        }
    }

    /**
     * Creates a new StorageListException with the specified message, and root
     * cause.
     * @param message An error message describing why this exception was thrown.
     * @param throwable The underlying cause of this exception.
     */
    public StorageListException(final String message, final Throwable throwable) {
        super(message, throwable);
    }

    /**
     * Creates a new StorageListException with the specified message.
     * @param message An error message describing why this exception was thrown.
     */
    public StorageListException(final String message) {
        super(message);
    }

    /**
     * Creates a new StorageListException with the root cause cause.
     * @param throwable The underlying cause of this exception.
     */
    public StorageListException(final Throwable throwable) {
        super(throwable);
    }
}
