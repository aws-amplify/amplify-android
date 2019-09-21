/*
 * Copyright (c) 2019. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.amplifyframework.core.plugin;

import com.amplifyframework.core.exception.AmplifyException;

/**
 * Exceptions associated with configuring and inspecting Amplify Plugins
 */
public class PluginException extends AmplifyException {
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
     * The selector factory being assigned to a category is invalid
     */
    public static class InvalidSelectorFactoryException extends PluginException {
        public InvalidSelectorFactoryException() { super("The selector factory being assigned to a category is invalid"); }
        public InvalidSelectorFactoryException(String message) {
            super(message);
        }
        public InvalidSelectorFactoryException(Throwable throwable) {
            super(throwable);
        }
        public InvalidSelectorFactoryException(String message, Throwable t) {
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
        public NoSuchPluginException(String message) {
            super(message);
        }
        public NoSuchPluginException(Throwable throwable) {
            super(throwable);
        }
        public NoSuchPluginException(String message, Throwable t) {
            super(message, t);
        }
    }

    /**
     * An attempt was made to add a plugin to a category that already had
     * one plugin, without first registering a PluginSelectorFactory
     */
    public static class NoSelectorException extends PluginException {
        public NoSelectorException() { super("An attempt was made to add a plugin to a category that already had one plugin, without first registering a PluginSelectorFactory"); }
        public NoSelectorException(String message) { super(message); }
        public NoSelectorException(Throwable throwable) { super(throwable); }
        public NoSelectorException(String message, Throwable t) { super(message, t); }
    }

    /**
     * The plugin encountered an error during configuration
     */
    public static class PluginConfigurationException extends PluginException {
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
