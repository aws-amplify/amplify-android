/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.auth.result;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.auth.AuthException;

/**
 * Wraps a session value which could have had an error even though the method fetching it had a success callback.
 * @param <T> The type of data this result wraps.
 */
public final class AuthSessionResult<T> {
    private final T value;
    private final AuthException error;
    private final Type type;

    private AuthSessionResult(T value, AuthException error, Type type) {
        this.value = value;
        this.error = error;
        this.type = type;
    }

    /**
     * Create a result object for a successful session value fetch operation.
     * @param value The value successfully retrieved.
     * @param <T> The type of value this stores.
     * @return A new result object for the successfully retrieved value.
     */
    public static <T> AuthSessionResult<T> success(T value) {
        return new AuthSessionResult<>(value, null, Type.SUCCESS);
    }

    /**
     * Create a result object for a failed value fetch operation.
     * @param error The error describing what went wrong while trying to fetch the value.
     * @param <T> The type of value attempting to be retrieved.
     * @return A new result object for the unsuccessfully retrieved value.
     */
    public static <T> AuthSessionResult<T> failure(AuthException error) {
        return new AuthSessionResult<>(null, error, Type.FAILURE);
    }

    /**
     * Returns the value which was successfully retrieved.
     * @return the value which was successfully retrieved.
     */
    @Nullable
    public T getValue() {
        return value;
    }

    /**
     * Returns the error describing what went wrong while attempting to retrieve the value.
     * @return the error describing what went wrong while attempting to retrieve the value.
     */
    @Nullable
    public AuthException getError() {
        return error;
    }

    /**
     * Returns the type of result this was - whether it was successful or a failure. Useful for switch statements.
     * @return the type of result this was - whether it was successful or a failure.
     */
    @NonNull
    public Type getType() {
        return type;
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(
                getValue(),
                getError(),
                getType()
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || !(obj instanceof AuthSessionResult<?>)) {
            return false;
        } else {
            AuthSessionResult<?> authSessionResult = (AuthSessionResult<?>) obj;
            return ObjectsCompat.equals(getValue(), authSessionResult.getValue()) &&
                    ObjectsCompat.equals(getError(), authSessionResult.getError()) &&
                    ObjectsCompat.equals(getType(), authSessionResult.getType());
        }
    }

    @Override
    public String toString() {
        return "AuthSessionResult{" +
                "value=" + getValue() +
                ", error=" + getError() +
                ", type=" + getType() +
                '}';
    }

    /**
     * The type of result this was - whether it was successful or a failure.
     */
    public enum Type {
        /**
         * Fetch value operation was successful.
         * You can retrieve the result with {@link #getValue()}.
         */
        SUCCESS,
        /**
         * Fetch value operation failed.
         * You can retrieve the exception explaining what went wrong with {@link #getError()}.
         */
        FAILURE
    }
}
