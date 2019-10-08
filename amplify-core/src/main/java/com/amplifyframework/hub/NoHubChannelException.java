package com.amplifyframework.hub;

import com.amplifyframework.AmplifyRuntimeException;

public class NoHubChannelException extends AmplifyRuntimeException {

    /** Default serial version UID. */
    private static final long serialVersionUID = 4L;

    /**
     * Creates a new AmplifyRuntimeException with the specified message, and root
     * cause.
     *
     * @param message An error message describing why this exception was thrown.
     * @param t       The underlying cause of this exception.
     */
    public NoHubChannelException(String message, Throwable t) {
        super(message, t);
    }

    /**
     * Creates a new AmplifyRuntimeException with the specified message.
     *
     * @param message An error message describing why this exception was thrown.
     */
    public NoHubChannelException(String message) {
        super(message);
    }

    /**
     * Create an AmplifyRuntimeException with an exception cause.
     *
     * @param throwable the cause of the exception.
     */
    public NoHubChannelException(Throwable throwable) {
        super(throwable);
    }
}
