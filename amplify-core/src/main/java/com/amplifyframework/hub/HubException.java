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

package com.amplifyframework.hub;

import com.amplifyframework.AmplifyRuntimeException;

/**
 * Base exception type for errors in the Hub category/plugin(s).
 */
public class HubException extends AmplifyRuntimeException {

    private static final long serialVersionUID = 2L;

    /**
     * The Hub category has not been configured.
     */
    public static class HubNotConfiguredException extends AmplifyRuntimeException {

        private static final long serialVersionUID = 3L;

        /**
         * Constructs a new HubNotConfiguredException using a default error message.
         */
        public HubNotConfiguredException() {
            super("Hub category is not configured. " +
                    "Please configure it through Amplify.configure(context)");
        }

        /**
         * Constructs a new HubNotConfiguredException using a provided error message.
         * @param message Explains that the Hub is not configured, so can't be used
         */
        public HubNotConfiguredException(String message) {
            super(message);
        }

        /**
         * Constructs a new HubNotConfiguredException associated to a provided error.
         * @param throwable An associated error, perhaps the reason why the Hub
         *                  is not configured
         */
        public HubNotConfiguredException(Throwable throwable) {
            super(throwable);
        }

        /**
         * Constructs a new HubNotConfiguredException using a provided error message,
         * and associated to a provided error.
         * @param message Explains that Hub is not configured so can't be used
         * @param throwable An associated error, perhaps the reason why
         *                  Hub is not configured
         */
        public HubNotConfiguredException(String message, Throwable throwable) {
            super(message, throwable);
        }
    }

    /**
     * Creates a new ConfigurationException with the specified message, and root
     * cause.
     * @param message An error message describing why this exception was thrown.
     * @param throwable The underlying cause of this exception.
     */
    public HubException(String message, Throwable throwable) {
        super(message, throwable);
    }

    /**
     * Creates a new ConfigurationException with the specified message.
     * @param message An error message describing why this exception was thrown.
     */
    public HubException(String message) {
        super(message);
    }

    /**
     * Create an ConfigurationException with an exception cause.
     * @param throwable the cause of the exception.
     */
    public HubException(Throwable throwable) {
        super(throwable);
    }
}
