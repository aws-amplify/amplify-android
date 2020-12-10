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

package com.amplifyframework.datastore.storage.sqlite;

import androidx.annotation.NonNull;

/**
 * Enumerate the types of data supported by SQLite.
 */
public enum SQLiteDataType {
    /**
     * The value is a NULL value.
     */
    NULL("NULL"),

    /**
     * The value is a signed integer, stored in 1, 2, 3, 4, 6, or 8 bytes
     * depending on the magnitude of the value.
     */
    INTEGER("INTEGER"),

    /**
     * The value is a floating point value, stored as an 8-byte ("double precision")
     * IEEE floating point number.
     */
    REAL("REAL"),

    /**
     * The value is a text string, stored using the database encoding
     * (UTF-8, UTF-16BE or UTF-16LE).
     */
    TEXT("TEXT"),

    /**
     * The value is a blob of data, stored exactly as it was input.
     */
    BLOB("BLOB");

    private final String sqliteDataType;

    SQLiteDataType(@NonNull String sqliteDataType) {
        this.sqliteDataType = sqliteDataType;
    }

    /**
     * Return the string that represents the value of the enumeration constant.
     * @return the string that represents the value of the enumeration constant.
     */
    public String getSqliteDataType() {
        return this.sqliteDataType;
    }

    /**
     * Construct and return the SqliteDataType enumeration for the given string
     * representation of the field type.
     * @param sqliteDataType the string representation of the field type.
     * @return the enumeration constant.
     */
    public static SQLiteDataType from(@NonNull String sqliteDataType) {
        for (final SQLiteDataType type : SQLiteDataType.values()) {
            if (sqliteDataType.equals(type.getSqliteDataType())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Cannot create enum from " + sqliteDataType + " value.");
    }
}
