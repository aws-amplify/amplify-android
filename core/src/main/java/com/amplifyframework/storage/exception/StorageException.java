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

import com.amplifyframework.ConfigurationException;

/**
 * Base exception type for any exception that is raised from within the
 * storage category.
 */
public class StorageException extends ConfigurationException {

    private static final long serialVersionUID = 1L;

    /**
     * An attempt has been made to use the storage category, but the
     * storage category was never configured.
     */
    public static class StorageNotConfiguredException extends StorageException {

        private static final long serialVersionUID = 1L;

        /**
         * Constructs a new StorageNotConfiguredException using a
         * default message.
         */
        public StorageNotConfiguredException() {
            super("Storage category is not configured. Please configure it through Amplify.configure(context)");
        }

        /**
         * Constructs a new StorageNotConfiguredException using a
         * user-provided message.
         * @param message Explains in more detail why the exception was thrown
         */
        public StorageNotConfiguredException(String message) {
            super(message);
        }

        /**
         * Constructs a new StorageNotConfiguredException that has been
         * caused by another error.
         * @param throwable The error that caused storage to not be configured
         */
        public StorageNotConfiguredException(Throwable throwable) {
            super(throwable);
        }

        /**
         * Constructs a new StorageNotConfiguredException, providing a
         * custom message and an underlying error that caused this
         * state.
         * @param message Explanation of why the exception has been raised
         * @param throwable An underlying error that caused this * exception
         */
        public StorageNotConfiguredException(String message, Throwable throwable) {
            super(message, throwable);
        }
    }

    /**
     * Creates a new ConfigurationException with the specified message, and root
     * cause.
     * @param message An error message describing why this exception was thrown.
     * @param throwable The underlying cause of this exception.
     */
    public StorageException(String message, Throwable throwable) {
        super(message, throwable);
    }

    /**
     * Creates a new ConfigurationException with the specified message.
     * @param message An error message describing why this exception was thrown.
     */
    public StorageException(String message) {
        super(message);
    }

    /**
     * Create an ConfigurationException with an exception cause.
     * @param throwable the cause of the exception.
     */
    public StorageException(Throwable throwable) {
        super(throwable);
    }
}

