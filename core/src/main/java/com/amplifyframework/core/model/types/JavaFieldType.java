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

/**
 * Enumerate the types used in the fields
 * of {@link com.amplifyframework.core.model.Model} classes.
 */
public enum JavaFieldType {
    /**
     * Represents the boolean data type.
     */
    BOOLEAN(Boolean.class.getSimpleName()),

    /**
     * Represents the int data type.
     */
    INTEGER(Integer.class.getSimpleName()),

    /**
     * Represents the long data type.
     */
    LONG(Long.class.getSimpleName()),

    /**
     * Represents the float data type.
     */
    FLOAT(Float.class.getSimpleName()),

    /**
     * Represents the String data type.
     */
    STRING(String.class.getSimpleName()),

    /**
     * Represents the java.util.Date data type.
     */
    DATE(java.util.Date.class.getSimpleName()),

    /**
     * Represents the java.sql.Time data type.
     */
    TIME(java.sql.Time.class.getSimpleName()),

    /**
     * Represents the Enum type.
     */
    ENUM(Enum.class.getSimpleName()),

    /**
     * Represents the Model type.
     */
    MODEL(Model.class.getSimpleName());

    private final String javaFieldType;

    JavaFieldType(@NonNull String javaFieldType) {
        this.javaFieldType = javaFieldType;
    }

    /**
     * Return the string that represents the value of the enumeration constant.
     * @return the string that represents the value of the enumeration constant.
     */
    public String stringValue() {
        return this.javaFieldType;
    }

    /**
     * Construct and return the JavaFieldType enumeration for the given string
     * representation of the field type.
     * @param javaFieldType the string representation of the field type.
     * @return the enumeration constant.
     */
    public static JavaFieldType from(@NonNull String javaFieldType) {
        for (final JavaFieldType type : JavaFieldType.values()) {
            if (javaFieldType.equals(type.stringValue())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Cannot create enum from " + javaFieldType + " value.");
    }
}
