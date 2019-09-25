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

import com.amplifyframework.core.exception.AmplifyRuntimeException;

/**
 * Exceptions associated with configuring and inspecting Amplify Plugins
 */
public class PluginRuntimeException extends AmplifyRuntimeException {
    /**
     * The plugin encountered an error during configuration
     */
    public static class PluginConfigurationException extends PluginRuntimeException {
        public PluginConfigurationException() { super("The plugin encountered an error during configuration"); }
        public PluginConfigurationException(String message) {
            super(message);
        }
        public PluginConfigurationException(Throwable throwable) {
            super(throwable);
        }
        public PluginConfigurationException(String message, Throwable t) {
            super(message, t);
        }
    }

    /** There is no registered plugin for a category */
    public static class NoPluginException extends PluginRuntimeException {
        public NoPluginException() { super("There is no registered plugin for this category"); }
        public NoPluginException(String message) { super(message); }
        public NoPluginException(Throwable throwable) { super(throwable); }
        public NoPluginException(String message, Throwable t) { super(message, t); }
    }

    /** There are multiple registered plugins for a category */
    public static class MultiplePluginsException extends PluginRuntimeException {
        public MultiplePluginsException() { super("There is more than one plugin registered in this category"); }
        public MultiplePluginsException(String message) { super(message); }
        public MultiplePluginsException(Throwable throwable) { super(throwable); }
        public MultiplePluginsException(String message, Throwable t) { super(message, t); }
    }

    /**
     * Creates a new PluginRuntimeException with the specified message, and root
     * cause.
     *
     * @param message An error message describing why this exception was thrown.
     * @param t The underlying cause of this exception.
     */
    public PluginRuntimeException(final String message, final Throwable t) { super(message, t); }

    /**
     * Creates a new PluginRuntimeException with the specified message.
     *
     * @param message An error message describing why this exception was thrown.
     */
    public PluginRuntimeException(final String message) {
        super(message);
    }

    /**
     * Create an PluginRuntimeException with an exception cause.
     *
     * @param throwable the cause of the exception.
     */
    public PluginRuntimeException(final Throwable throwable) {
        super(throwable);
    }
}
