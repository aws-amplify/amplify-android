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
 * Exceptions associated with configuring and inspecting Amplify Categories.
 */
public class ConfigurationException extends AmplifyRuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * The Ampilfy framework has already been configured.
     */
    public static class AmplifyAlreadyConfiguredException extends ConfigurationException {

        private static final long serialVersionUID = 1L;

        /**
         * Constructs a new AmplifyAlreadyConfiguredException using a
         * default error message.
         */
        public AmplifyAlreadyConfiguredException() {
            super("The client issued a subsequent call to `Amplify.configure` after the first had already succeeded.");
        }

        /**
         * Constructs a new AmplifyAlreadyConfiguredException using a
         * user-provided message, and an associated error.
         * @param message Explains that Amplify has already been configured
         * @param throwable An associated or underlying error
         */
        public AmplifyAlreadyConfiguredException(String message, Throwable throwable) {
            super(message, throwable);
        }

        /**
         * Constructs a new AmplifyAlreadyConfiguredException using a
         * user-provided explanatory message.
         * @param message Explains that amplify has already been configured
         */
        public AmplifyAlreadyConfiguredException(String message) {
            super(message);
        }

        /**
         * Constructs a new AmplifiyAlreadyConfiguredException
         * associated to a related error.
         * @param throwable An error relating to the newly created exception
         */
        public AmplifyAlreadyConfiguredException(Throwable throwable) {
            super(throwable);
        }
    }

    /**
     * The Amplify configuration file is not valid.
     */
    public static class InvalidAmplifyConfigurationFileException extends ConfigurationException {

        private static final long serialVersionUID = 1L;

        /**
         * Constructs a new InvalidAmplifyConfigurationFileException,
         * using a default explanation for its invalidity.
         */
        public InvalidAmplifyConfigurationFileException() {
            super("The specified `amplifyconfiguration.json` file was not present or unreadable.");
        }

        /**
         * Constructs a new InvalidAmplifyConfigurationFileException,
         * using a custom message to explain the issue, as well as an
         * associated error that causes the exception.
         * @param message Explains why/how/that the configuration is invalid
         * @param throwable An error that caused or relates to this exception
         */
        public InvalidAmplifyConfigurationFileException(String message, Throwable throwable) {
            super(message, throwable);
        }

        /**
         * Constructs a new InvalidAmplifyConfigurationFileException,
         * using a custom explanation for its invalidity.
         * @param message Explains how/why/that the Amplify
         *                configuration file is invalid in some way
         */
        public InvalidAmplifyConfigurationFileException(String message) {
            super(message);
        }

        /**
         * Constructs a new InvalidAmplifyConfigurationFileException,
         * associated to a related/underlying error.
         * @param throwable An error related to this exception
         */
        public InvalidAmplifyConfigurationFileException(Throwable throwable) {
            super(throwable);
        }
    }

    /**
     * Unable to decode the Amplify configuration file into an
     * AmplifyConfiguration representation.
     */
    public static class UnableToDecodeException extends ConfigurationException {

        private static final long serialVersionUID = 1L;

        /**
         * Constructs a new UnableToDecodeException using a default
         * error message.
         */
        public UnableToDecodeException() {
            super("Unable to decode `amplifyconfiguration.json` into a valid AmplifyConfiguration object");
        }

        /**
         * Constructs a new UnableToDecodeException using a custom error
         * message and associating it with a related error.
         * @param message Explains how/why the configuration could not be decoded
         * @param throwable An error related to the decoding exception
         */
        public UnableToDecodeException(String message, Throwable throwable) {
            super(message, throwable);
        }

        /**
         * Constructs a new UnableToDecodeException using a custom error
         * message.
         * @param message Explains why/how/that the configuration coul not be decoded
         */
        public UnableToDecodeException(String message) {
            super(message);
        }

        /**
         * Constructs a new UnableToDecodeException related to an
         * associated error.
         * @param throwable An error relating to the decode exception
         */
        public UnableToDecodeException(Throwable throwable) {
            super(throwable);
        }
    }

    /**
     * Creates a new ConfigurationException with the specified message, and root
     * cause.
     * @param message An error message describing why this exception was thrown.
     * @param throwable The underlying cause of this exception.
     */
    public ConfigurationException(final String message, final Throwable throwable) {
        super(message, throwable);
    }

    /**
     * Creates a new ConfigurationException with the specified message.
     * @param message An error message describing why this exception was thrown.
     */
    public ConfigurationException(final String message) {
        super(message);
    }

    /**
     * Create an ConfigurationException with an exception cause.
     * @param throwable the cause of the exception.
     */
    public ConfigurationException(final Throwable throwable) {
        super(throwable);
    }
}

