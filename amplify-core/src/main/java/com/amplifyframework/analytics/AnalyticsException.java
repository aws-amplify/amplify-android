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

package com.amplifyframework.analytics;

import com.amplifyframework.AmplifyRuntimeException;

/**
 * Exception thrown by Analytics category plugins.
 */
public class AnalyticsException extends AmplifyRuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new AmazonClientException with the specified message, and root
     * cause.
     * @param message An error message describing why this exception was thrown.
     * @param throwable The underlying cause of this exception.
     */
    public AnalyticsException(final String message, final Throwable throwable) {
        super(message, throwable, null, true);
    }

    /**
     * Creates a new AmazonClientException with the specified message.
     * @param message An error message describing why this exception was thrown.
     */
    public AnalyticsException(final String message) {
        this(message, null);
    }

    /**
     * Create an AmazonClientException with an exception cause.
     * @param throwable the cause of the exception.
     */
    public AnalyticsException(final Throwable throwable) {
        this(null, throwable);
    }
}

