/*
 * Copyright (c) 2019. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.amplifyframework.core.plugin;

import com.amplifyframework.core.exception.AmplifyException;

public class PluginException extends AmplifyException {
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

    public static class NoSelectorException extends PluginException {
        public NoSelectorException() { super("An attempt was made to add a plugin to a category that already had one plugin, without first registering a PluginSelectorFactory"); }
        public NoSelectorException(String message) { super(message); }
        public NoSelectorException(Throwable throwable) { super(throwable); }
        public NoSelectorException(String message, Throwable t) { super(message, t); }
    }

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

    public PluginException(final String message, final Throwable t) {
        super(message, t);
    }

    public PluginException(final String message) {
        super(message);
    }

    public PluginException(final Throwable throwable) {
        super(throwable);
    }
}
