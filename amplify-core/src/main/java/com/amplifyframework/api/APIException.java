package com.amplifyframework.api;

import com.amplifyframework.core.exception.AmplifyException;

public class APIException extends AmplifyException {
    /**
     * Creates a new AmplifyException with the specified message, and root
     * cause.
     *
     * @param message An error message describing why this exception was thrown.
     * @param t       The underlying cause of this exception.
     */
    public APIException(String message, Throwable t) {
        super(message, t);
    }

    /**
     * Creates a new AmplifyException with the specified message.
     *
     * @param message An error message describing why this exception was thrown.
     */
    public APIException(String message) {
        super(message);
    }

    /**
     * Create an AmplifyException with an exception cause.
     *
     * @param throwable the cause of the exception.
     */
    public APIException(Throwable throwable) {
        super(throwable);
    }
}
