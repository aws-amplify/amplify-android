package com.amplifyframework.api;

import com.amplifyframework.core.exception.AmplifyRuntimeException;

public class ApiException extends AmplifyRuntimeException {
    /** The plugin implements unsupported sub-category of API */
    public static class UnsupportedAPITypeException extends ApiException {
        public UnsupportedAPITypeException() { super("This type of API is not supported by the current version of Amplify."); }
        public UnsupportedAPITypeException(String message) { super(message); }
        public UnsupportedAPITypeException(Throwable throwable) { super(throwable); }
        public UnsupportedAPITypeException(String message, Throwable t) { super(message, t); }
    }

    /**
     * Creates a new ApiException with the specified message, and root
     * cause.
     *
     * @param message An error message describing why this exception was thrown.
     * @param t The underlying cause of this exception.
     */
    public ApiException(final String message, final Throwable t) { super(message, t); }

    /**
     * Creates a new ApiException with the specified message.
     *
     * @param message An error message describing why this exception was thrown.
     */
    public ApiException(final String message) {
        super(message);
    }

    /**
     * Create an ApiException with an exception cause.
     *
     * @param throwable the cause of the exception.
     */
    public ApiException(final Throwable throwable) {
        super(throwable);
    }
}
