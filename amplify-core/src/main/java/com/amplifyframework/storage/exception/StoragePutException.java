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

import com.amplifyframework.core.exception.AmplifyException;

/**
 * Exception encountered in put API of Storage category
 */
public class StoragePutException extends AmplifyException {

    private static final long serialVersionUID = 1L;

    /**
     * Specifies that exception was due to missing file
     */
    public static class MissingFileException extends StoragePutException {

        private static final long serialVersionUID = 1L;

        public MissingFileException() {
            super("Missing file to store.");
        }

        public MissingFileException(String message) {
            super(message);
        }

        public MissingFileException(Throwable throwable) {
            super(throwable);
        }

        public MissingFileException(String message, Throwable t) {
            super(message, t);
        }
    }

    /**
     * Creates a new StoragePutException with the specified message, and root
     * cause.
     *
     * @param message An error message describing why this exception was thrown.
     * @param t The underlying cause of this exception.
     */
    public StoragePutException(final String message, final Throwable t) {
        super(message, t);
    }

    /**
     * Creates a new StoragePutException with the specified message.
     *
     * @param message An error message describing why this exception was thrown.
     */
    public StoragePutException(final String message) {
        super(message);
    }

    /**
     * Creates a new StoragePutException with the root cause.
     *
     * @param throwable The underlying cause of this exception.
     */
    public StoragePutException(final Throwable throwable) {
        super(throwable);
    }
}

