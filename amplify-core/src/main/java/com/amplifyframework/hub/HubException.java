package com.amplifyframework.hub;

import com.amplifyframework.AmplifyRuntimeException;

public class HubException extends AmplifyRuntimeException {
    public static class HubNotConfiguredException extends AmplifyRuntimeException {
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
