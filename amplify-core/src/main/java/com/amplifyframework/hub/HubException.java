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

public class HubException extends AmplifyRuntimeException {

    /** Default serial version UID. */
    private static final long serialVersionUID = 2L;

    public static class HubNotConfiguredException extends AmplifyRuntimeException {
        /** Default serial version UID. */
        private static final long serialVersionUID = 3L;

        public HubNotConfiguredException() { super("Hub category is not configured. Please configure it through Amplify.configure(context)"); }
        public HubNotConfiguredException(String message) { super(message); }
        public HubNotConfiguredException(Throwable throwable) { super(throwable); }
        public HubNotConfiguredException(String message, Throwable t) { super(message, t); }
    }

    /**
     * Creates a new ConfigurationException with the specified message, and root
     * cause.
     *
     * @param message An error message describing why this exception was thrown.
     * @param t       The underlying cause of this exception.
     */
    public HubException(String message, Throwable t) {
        super(message, t);
    }

    /**
     * Creates a new ConfigurationException with the specified message.
     *
     * @param message An error message describing why this exception was thrown.
     */
    public HubException(String message) {
        super(message);
    }

    /**
     * Create an ConfigurationException with an exception cause.
     *
     * @param throwable the cause of the exception.
     */
    public HubException(Throwable throwable) {
        super(throwable);
    }
}
