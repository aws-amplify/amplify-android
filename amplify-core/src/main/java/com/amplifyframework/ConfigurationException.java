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

package com.amplifyframework;

/**
 * Exceptions associated with configuring and inspecting Amplify Categories
 */
public class ConfigurationException extends AmplifyRuntimeException {

    private static final long serialVersionUID = 1L;

    public static class AmplifyAlreadyConfiguredException extends ConfigurationException {

        private static final long serialVersionUID = 1L;

        public AmplifyAlreadyConfiguredException() {
            super("The client issued a subsequent call to `Amplify.configure` after the first had already succeeded.");
        }

        public AmplifyAlreadyConfiguredException(String message, Throwable t) {
            super(message, t);
        }

        public AmplifyAlreadyConfiguredException(String message) {
            super(message);
        }

        public AmplifyAlreadyConfiguredException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class InvalidAmplifyConfigurationFileException extends ConfigurationException {

        private static final long serialVersionUID = 1L;

        public InvalidAmplifyConfigurationFileException() {
            super("The specified `amplifyconfiguration.json` file was not present or unreadable.");
        }

        public InvalidAmplifyConfigurationFileException(String message, Throwable t) {
            super(message, t);
        }

        public InvalidAmplifyConfigurationFileException(String message) {
            super(message);
        }

        public InvalidAmplifyConfigurationFileException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class UnableToDecodeException extends ConfigurationException {

        private static final long serialVersionUID = 1L;

        public UnableToDecodeException() {
            super("Unable to decode `amplifyconfiguration.json` into a valid AmplifyConfiguration object");
        }

        public UnableToDecodeException(String message, Throwable t) {
            super(message, t);
        }

        public UnableToDecodeException(String message) {
            super(message);
        }

        public UnableToDecodeException(Throwable throwable) {
            super(throwable);
        }
    }

    /**
     * Creates a new ConfigurationException with the specified message, and root
     * cause.
     *
     * @param message An error message describing why this exception was thrown.
     * @param t The underlying cause of this exception.
     */
    public ConfigurationException(final String message, final Throwable t) {
        super(message, t);
    }

    /**
     * Creates a new ConfigurationException with the specified message.
     *
     * @param message An error message describing why this exception was thrown.
     */
    public ConfigurationException(final String message) {
        super(message);
    }

    /**
     * Create an ConfigurationException with an exception cause.
     *
     * @param throwable the cause of the exception.
     */
    public ConfigurationException(final Throwable throwable) {
        super(throwable);
    }
}

