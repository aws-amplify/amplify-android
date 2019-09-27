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

import com.amplifyframework.core.exception.AmplifyException;
import com.amplifyframework.core.exception.AmplifyRuntimeException;

/**
 * Exceptions associated with configuring and inspecting Amplify Plugins
 */
public class PluginException extends AmplifyRuntimeException {
    /**
     * The plugin's `key` property is empty
     */
    public static class EmptyKeyException extends PluginException {
        public EmptyKeyException() { super("The plugin's `key` property is empty"); }
        public EmptyKeyException(String message) {
            super(message);
        }
        public EmptyKeyException(Throwable throwable) {
            super(throwable);
        }
        public EmptyKeyException(String message, Throwable t) {
            super(message, t);
        }
    }

    /**
     * A plugin is being added to the wrong category
     */
    public static class MismatchedPluginException extends PluginException {
        public MismatchedPluginException() { super("A plugin is being added to the wrong category"); }
        public MismatchedPluginException(String message) {
            super(message);
        }
        public MismatchedPluginException(Throwable throwable) {
            super(throwable);
        }
        public MismatchedPluginException(String message, Throwable t) {
            super(message, t);
        }
    }

    /**
     * The plugin specified by `getPlugin(key)` does not exist
     */
    public static class NoSuchPluginException extends PluginException {
        public NoSuchPluginException() { super("The plugin specified by `getPlugin(key)` does not exist"); }
        public NoSuchPluginException(String message) { super(message); }
        public NoSuchPluginException(Throwable throwable) { super(throwable); }
        public NoSuchPluginException(String message, Throwable t) { super(message, t); }
    }

    /**
     * Creates a new PluginException with the specified message, and root
     * cause.
     *
     * @param message An error message describing why this exception was thrown.
     * @param t The underlying cause of this exception.
     */
    public PluginException(final String message, final Throwable t) { super(message, t); }

    /**
     * Creates a new PluginException with the specified message.
     *
     * @param message An error message describing why this exception was thrown.
     */
    public PluginException(final String message) {
        super(message);
    }

    /**
     * Create an PluginException with an exception cause.
     *
     * @param throwable the cause of the exception.
     */
    public PluginException(final Throwable throwable) {
        super(throwable);
    }
}
