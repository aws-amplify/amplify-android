package com.amplifyframework.analytics;

import com.amplifyframework.core.exception.AmplifyException;
import com.amplifyframework.core.exception.AmplifyRuntimeException;

public class AnalyticsException extends AmplifyRuntimeException {

    /** Default serial version UID. */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new AmazonClientException with the specified message, and root
     * cause.
     *
     * @param message An error message describing why this exception was thrown.
     * @param t The underlying cause of this exception.
     */
    public AnalyticsException(final String message, final Throwable t) {
        super(message, t);
    }

    /**
     * Creates a new AmazonClientException with the specified message.
     *
     * @param message An error message describing why this exception was thrown.
     */
    public AnalyticsException(final String message) {
        super(message);
    }

    /**
     * Create an AmazonClientException with an exception cause.
     *
     * @param throwable the cause of the exception.
     */
    public AnalyticsException(final Throwable throwable) {
        super(throwable);
    }

    /**
     * Returns a hint as to whether it makes sense to retry upon this exception.
     * Default is true, but subclass may override.
     * @return true if it is retryable.
     */
    public boolean isRetryable() {
        return true;
    }
}
