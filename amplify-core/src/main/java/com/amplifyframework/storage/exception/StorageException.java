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

public class StorageException extends ConfigurationException {

    private static final long serialVersionUID = 1L;

    public static class StorageNotConfiguredException extends StorageException {

        private static final long serialVersionUID = 1L;

        public StorageNotConfiguredException() {
            super("Storage category is not configured. Please configure it through Amplify.configure(context)");
        }

        public StorageNotConfiguredException(String message) {
            super(message);
        }

        public StorageNotConfiguredException(Throwable throwable) {
            super(throwable);
        }

        public StorageNotConfiguredException(String message, Throwable t) {
            super(message, t);
        }
    }

    /**
     * Creates a new ConfigurationException with the specified message, and root
     * cause.
     *
     * @param message An error message describing why this exception was thrown.
     * @param t       The underlying cause of this exception.
     */
    public StorageException(String message, Throwable t) {
        super(message, t);
    }

    /**
     * Creates a new ConfigurationException with the specified message.
     *
     * @param message An error message describing why this exception was thrown.
     */
    public StorageException(String message) {
        super(message);
    }

    /**
     * Create an ConfigurationException with an exception cause.
     *
     * @param throwable the cause of the exception.
     */
    public StorageException(Throwable throwable) {
        super(throwable);
    }
}

