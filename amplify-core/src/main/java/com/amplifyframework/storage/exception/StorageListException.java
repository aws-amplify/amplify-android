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

public class StorageListException extends AmplifyException {
    public static class AccessDenied extends StorageListException {
        public AccessDenied() { super("Access to storage denied."); }
        public AccessDenied(String message) {
            super(message);
        }
        public AccessDenied(Throwable throwable) {
            super(throwable);
        }
        public AccessDenied(String message, Throwable t) {
            super(message, t);
        }
    }

    public StorageListException(final String message, final Throwable t) { super(message, t); }

    public StorageListException(final String message) {
        super(message);
    }

    public StorageListException(final Throwable throwable) {
        super(throwable);
    }
}
