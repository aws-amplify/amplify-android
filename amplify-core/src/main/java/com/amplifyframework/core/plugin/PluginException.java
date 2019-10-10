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

package com.amplifyframework.core.plugin;

import com.amplifyframework.AmplifyRuntimeException;

/**
 * Base exception type, encountered when configuring or interacting with
 * Amplify plugins.
 */
public class PluginException extends AmplifyRuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * The plugin encountered an error during configuration.
     */
    public static class PluginConfigurationException extends PluginException {

        private static final long serialVersionUID = 1L;

        /**
         * Constructs a new PluginConfigurationException using a default
         * exception message.
         */
        public PluginConfigurationException() {
            super("The plugin encountered an error during configuration");
        }

        /**
         * Constructs a new PluginConfigurationException using a
         * user-provided exception message.
         * @param message A message which explains why the exception occurred
         */
        public PluginConfigurationException(String message) {
            super(message);
        }

        /**
         * Constructs a new PluginConfigurationException that itself has
         * been caused by another underlying error.
         * @param throwable An error that has caused this exception to be raised
         */
        public PluginConfigurationException(Throwable throwable) {
            super(throwable);
        }

        /**
         * Constructs a new PluginConfigurationException from a
         * user-provided exception message and with reference to an
         * underlying error.
         * @param message Explains why the exception was caused
         * @param throwable An underyling error associated with this exception
         */
        public PluginConfigurationException(String message, Throwable throwable) {
            super(message, throwable);
        }
    }

    /**
     * An exception that is raised when a plugin's `key` property is empty.
     */
    public static class EmptyKeyException extends PluginException {

        private static final long serialVersionUID = 1L;

        /**
         * Constructs a new EmptyKeyException using a default error message.
         */
        public EmptyKeyException() {
            super("The plugin's `key` property is empty");
        }

        /**
         * Constructs a new EmptyKeyException using a custom error message.
         * @param message Explains that the plugin's key property is empty
         */
        public EmptyKeyException(String message) {
            super(message);
        }

        /**
         * Constructs a new EmptyKeyException associated with an error.
         * @param throwable An associated error
         */
        public EmptyKeyException(Throwable throwable) {
            super(throwable);
        }

        /**
         * Constructs a new EmptyKeyException using a custom error message,
         * and associated with an error.
         * @param message Explains that the plugin's key property is empty
         * @param throwable An associated error
         */
        public EmptyKeyException(String message, Throwable throwable) {
            super(message, throwable);
        }
    }

    /**
     * A plugin is being added to the wrong category.
     */
    public static class MismatchedPluginException extends PluginException {

        private static final long serialVersionUID = 1L;

        /**
         * Constructs a new MismatchedPluginException, containing a default error message.
         */
        public MismatchedPluginException() {
            super("A plugin is being added to the wrong category");
        }

        /**
         * Constructs a new MismatchedPluginException from a user-provided error message.
         * @param message Explains how/why/that the plugin is mismatched
         */
        public MismatchedPluginException(String message) {
            super(message);
        }

        /**
         * Constructs a new MismatchedPluginException, associated to a related error.
         * @param throwable A related error
         */
        public MismatchedPluginException(Throwable throwable) {
            super(throwable);
        }

        /**
         * Constructs a new MismatchedPluginException using a custom error message,
         * and associated to a given error.
         * @param message Explains the way in which the plugin is mismatched
         * @param throwable An associated error
         */
        public MismatchedPluginException(String message, Throwable throwable) {
            super(message, throwable);
        }
    }

    /**
     * The plugin specified by `getPlugin(key)` does not exist.
     */
    public static class NoSuchPluginException extends PluginException {

        private static final long serialVersionUID = 1L;

        /**
         * Constructs a new NoSuchPluginException using a default error
         * message.
         */
        public NoSuchPluginException() {
            super("The plugin specified by `getPlugin(key)` does not exist");
        }

        /**
         * Constructs a new NoSuchPluginException using a user-provided
         * error message.
         * @param message An explanation that the plugin is not in existence
         */
        public NoSuchPluginException(String message) {
            super(message);
        }

        /**
         * Constructs a new NoSuchPluginException that has been caused
         * by an underlying error.
         * @param throwable An underlying error which causes this exception
         */
        public NoSuchPluginException(Throwable throwable) {
            super(throwable);
        }

        /**
         * Constructs a new NoSuchPluginException from a user-provided
         * message, and associated to an underlying error.
         * @param message A user-provided message explaining that the
         *                plugin does not exist
         * @param throwable An error that is associated with, or
         *                  underlying this exception
         */
        public NoSuchPluginException(String message, Throwable throwable) {
            super(message, throwable);
        }
    }

    /**
     * There are multiple registered plugins for a category.
     */
    public static class MultiplePluginsException extends PluginException {

        private static final long serialVersionUID = 1L;

        /**
         * Constructs a new MultiplePluginsException, using a default error message.
         */
        public MultiplePluginsException() {
            super("There is more than one plugin registered in this category");
        }

        /**
         * Constructs a new MultiplePluginsException from a provided error message.
         * @param message Explains that multiple plugins are registered
         */
        public MultiplePluginsException(String message) {
            super(message);
        }

        /**
         * Constructs a new MultiplePluginsException, associated to a provided error.
         * @param throwable An associated error
         */
        public MultiplePluginsException(Throwable throwable) {
            super(throwable);
        }

        /**
         * Constructs a new MultiplePluginsException using a provided error message,
         * and associated to an error.
         * @param message Explains that multiple plugins are registered
         * @param throwable An associated error
         */
        public MultiplePluginsException(String message, Throwable throwable) {
            super(message, throwable);
        }
    }

    /**
     * Creates a new PluginException with the specified message, and root
     * cause.
     * @param message An error message describing why this exception was thrown.
     * @param throwable The underlying cause of this exception.
     */
    public PluginException(final String message, final Throwable throwable) {
        super(message, throwable);
    }

    /**
     * Creates a new PluginException with the specified message.
     * @param message An error message describing why this exception was thrown.
     */
    public PluginException(final String message) {
        super(message);
    }

    /**
     * Create an PluginException with an exception cause.
     * @param throwable the cause of the exception.
     */
    public PluginException(final Throwable throwable) {
        super(throwable);
    }
}

