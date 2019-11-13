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

/**
 * The PrimaryKey of a data model is the field
 * with the name "id". This class is used to
 * encapsulate the field name "id".
 */
public final class PrimaryKey {
    // Name of the field that is the primary key
    // of any {@link Model}.
    private static final String ID = "id";

    @SuppressWarnings("checkstyle:all") private PrimaryKey() {}

    /**
     * Checks if the provided object is a primary key.
     * @param anything Any instance of any object
     * @return true when the provided object is a String whose value
     *         matches the value of {@link #fieldName()}.
     */
    public static boolean matches(Object anything) {
        return (anything instanceof String) && fieldName().equals(anything);
    }

    /**
     * Gets the field name for a primary key.
     * @return Field name for primary key
     */
    public static String fieldName() {
        return ID;
    }
}
