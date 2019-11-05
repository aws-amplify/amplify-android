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

import com.amplifyframework.datastore.model.ModelSchema;

/**
 * An encapsulation of the information required to
 * create a SQL table.
 */
final class CreateSqlCommand {

    // name of the SQL table
    private final String tableName;

    // create table command in string representation
    private final String sqlStatement;

    /**
     * Construct a CreateSqlCommand object.
     *
     * @param tableName name of the SQL table
     * @param sqlStatement create table command in string representation
     */
    CreateSqlCommand(@NonNull String tableName,
                     @NonNull String sqlStatement) {
        this.tableName = tableName;
        this.sqlStatement = sqlStatement;
    }

    /**
     * Create the CREATE TABLE SQL command for the corresponding
     * ModelSchema.
     *
     * @param modelSchema the schema of the model
     * @return the CREATE TABLE SQL command
     */
    static CreateSqlCommand fromModelSchema(@NonNull ModelSchema modelSchema) {
        return AndroidSQLiteCommandFactory.getInstance().createTableFor(modelSchema);
    }

    /**
     * Return the name of the SQL table.
     * @return the name of the SQL table.
     */
    String tableName() {
        return tableName;
    }

    /**
     * Return the create table SQL command in string representation.
     * @return the create table SQL command in string representation.
     */
    String sqlStatement() {
        return sqlStatement;
    }
}
