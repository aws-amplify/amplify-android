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

package com.amplifyframework.core.model;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * All models should conform to the Model
 * interface.
 */
public interface Model {
    /**
     * Return the ID that is the primary key
     * of a Model.
     *
     * This API is internal to Amplify and should not be used.
     *
     * @return the ID that is the primary key of a Model.
     */
    @NonNull
    default Serializable resolveIdentifier() {
        String exceptionMessage = "Primary key field Id not found.";
        try {
            String defaultPrimaryKeyMethod = "getId";
            Method method = this.getClass().getMethod(defaultPrimaryKeyMethod);
            return (Serializable) Objects.requireNonNull(method.invoke(this));
        } catch (IllegalAccessException exception) {
            throw (new IllegalStateException(exceptionMessage, exception));
        } catch (NoSuchMethodException exception) {
            throw (new IllegalStateException(exceptionMessage, exception));
        } catch (InvocationTargetException exception) {
            throw (new IllegalStateException(exceptionMessage, exception));
        }
    }

    /**
     * Returns the name of this model as a String.
     * @return the name of this model as a String.
     */
    @NonNull
    default String getModelName() {
        return getClass().getSimpleName();
    }

    /**
     * Gets concatenated partitionkey#sortKey.
     * @return concatenated partitionkey#sortKey...
     */
    @NonNull
    default String getPrimaryKeyString() {
        try {
            if (resolveIdentifier() instanceof ModelIdentifier) {
                return ((ModelIdentifier<?>) resolveIdentifier()).getIdentifier();
            } else {
                return resolveIdentifier().toString();
            }
        } catch (Exception exception) {
            throw (new IllegalStateException("Invalid Primary Key", exception));
        }
    }

    /**
     * This enum represents the types of Model.
     */
    enum Type {
        USER,
        SYSTEM
    }
}
