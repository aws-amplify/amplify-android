package com.amplifyframework.storage.exception;

import com.amplifyframework.core.exception.ConfigurationException;

public class StorageException extends ConfigurationException {

    public static class StorageNotConfiguredException extends StorageException {
        public StorageNotConfiguredException() { super("Storage category is not configured. Please configure it through Amplify.configure(context)"); }
        public StorageNotConfiguredException(String message) { super(message); }
        public StorageNotConfiguredException(Throwable throwable) { super(throwable); }
        public StorageNotConfiguredException(String message, Throwable t) { super(message, t); }
    }

    /**
     * Creates a new ConfigurationException with the specified message, and root
     * cause.
     *
     * @param message An error message describing why this exception was thrown.
     * @param t       The underlying cause of this exception.
     */
    public StorageException(String message, Throwable t) {
        super(message, t);
    }

    /**
     * Creates a new ConfigurationException with the specified message.
     *
     * @param message An error message describing why this exception was thrown.
     */
    public StorageException(String message) {
        super(message);
    }

    /**
     * Create an ConfigurationException with an exception cause.
     *
     * @param throwable the cause of the exception.
     */
    public StorageException(Throwable throwable) {
        super(throwable);
    }
}
