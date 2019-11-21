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

import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.NonNull;

import com.amplifyframework.core.model.ModelSchema;

/**
 * A factory that produces the SQLite commands from the
 * {@link ModelSchema} and the {@link com.amplifyframework.core.model.Model}.
 */
interface SQLCommandFactory {
    /**
     * Generates the CREATE TABLE SQL command from the {@link ModelSchema}.
     * @param modelSchema the schema of a {@link com.amplifyframework.core.model.Model}
     *                    for which a CREATE TABLE SQL command needs to be generated.
     * @return the CREATE TABLE SQL command
     */
    SqlCommand createTableFor(@NonNull ModelSchema modelSchema);

    /**
     * Generates the CREATE INDEX SQL command from the {@link ModelSchema}.
     * @param modelSchema the schema of a {@link com.amplifyframework.core.model.Model}
     *                    for which a CREATE INDEX SQL command needs to be generated.
     * @return the CREATE INDEX SQL command
     */
    SqlCommand createIndexFor(@NonNull ModelSchema modelSchema);

    /**
     * Generates the INSERT INTO command in a raw string representation and a compiled
     * prepared statement that can be bound later with inputs.
     *
     * @param tableName name of the table
     * @param modelSchema schema of the model
     * @param writableDatabaseConnectionHandle connection handle to writable database
     * @return the SQL command that encapsulates the INSERT INTO command
     */
    SqlCommand insertFor(@NonNull String tableName,
                         @NonNull ModelSchema modelSchema,
                         @NonNull SQLiteDatabase writableDatabaseConnectionHandle);

    /**
     * Generates the UPDATE command in a raw string representation and a compiled
     * prepared statement that can be bound later with inputs.
     *
     * @param tableName name of the table
     * @param modelSchema schema of the model
     * @param writableDatabaseConnectionHandle connection handle to writable database
     * @return the SQL command that encapsulates the UPDATE command
     */
    SqlCommand updateFor(@NonNull String tableName,
                         @NonNull ModelSchema modelSchema,
                         @NonNull SQLiteDatabase writableDatabaseConnectionHandle);

    /**
     * Generates the SELECT * FROM command in a raw string representation.
     *
     * @param tableName name of the table
     * @param columnName name of the column
     * @param columnValue value of the column
     * @return the SQL command that encapsulates the SELECT * FROM command
     */
    SqlCommand queryFor(@NonNull String tableName,
                        @NonNull String columnName,
                        @NonNull String columnValue);
}
