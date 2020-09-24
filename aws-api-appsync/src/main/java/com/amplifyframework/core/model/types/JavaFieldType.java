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

package com.amplifyframework.core.model.types;

import androidx.annotation.NonNull;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.temporal.Temporal;

import java.util.Date;

/**
 * Enumerate the types used in the fields
 * of {@link com.amplifyframework.core.model.Model} classes.
 */
public enum JavaFieldType {
    /**
     * Represents the boolean data type.
     */
    BOOLEAN(Boolean.class),

    /**
     * Represents the int data type.
     */
    INTEGER(Integer.class),

    /**
     * Represents the long data type.
     */
    LONG(Long.class),

    /**
     * Represents the float data type.
     */
    FLOAT(Float.class),

    /**
     * Represents the String data type.
     */
    STRING(String.class),

    /**
     * Represents the java.lang.Date data type.
     */
    JAVA_DATE(Date.class),

    /**
     * Represents the Date data type.
     */
    DATE(Temporal.Date.class),

    /**
     * Represents the DateTime data type.
     */
    DATE_TIME(Temporal.DateTime.class),

    /**
     * Represents the Time data type.
     */
    TIME(Temporal.Time.class),

    /**
     * Represents the Timestamp data type.
     */
    TIMESTAMP(Temporal.Timestamp.class),
    
    /**
     * Represents the Enum type.
     */
    ENUM(Enum.class),

    /**
     * Represents the Model type.
     */
    MODEL(Model.class),

    /**
     * Represents any custom type (objects that are not models).
     */
    CUSTOM_TYPE(Object.class);

    private final Class<?> javaFieldType;

    JavaFieldType(@NonNull Class<?> javaFieldType) {
        this.javaFieldType = javaFieldType;
    }

    /**
     * Construct and return the JavaFieldType enumeration for the given string
     * representation of the field type.
     * @param javaFieldType the string representation of the field type.
     * @return the enumeration constant.
     */
    public static JavaFieldType from(@NonNull Class<?> javaFieldType) {
        for (final JavaFieldType type : JavaFieldType.values()) {
            if (javaFieldType.equals(type.javaFieldType)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Cannot create enum from " + javaFieldType + " value.");
    }
}
