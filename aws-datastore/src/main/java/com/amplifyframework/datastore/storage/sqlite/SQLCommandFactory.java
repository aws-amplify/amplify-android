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

import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.query.QueryOptions;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.datastore.DataStoreException;

import java.util.Set;

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
    @NonNull
    SqlCommand createTableFor(@NonNull ModelSchema modelSchema);

    /**
     * Generates the set of CREATE INDEX SQL commands from the {@link ModelSchema}.
     * @param modelSchema the schema of a {@link com.amplifyframework.core.model.Model}
     *                    for which a CREATE INDEX SQL command needs to be generated.
     * @return the set of CREATE INDEX SQL commands
     */
    @NonNull
    Set<SqlCommand> createIndexesFor(@NonNull ModelSchema modelSchema);

    /**
     * Generates the QUERY command in a raw string representation from
     * the {@link ModelSchema}.
     *
     * @param modelSchema schema of the model
     * @return the QUERY SQL command
     */
    @NonNull
    SqlCommand queryFor(@NonNull ModelSchema modelSchema,
                        @NonNull QueryOptions options) throws DataStoreException;

    /**
     * Generates the INSERT INTO command in a raw string representation and a compiled
     * prepared statement that can be bound later with inputs.
     *
     * @param modelSchema schema of the model
     * @return the SQL command that encapsulates the INSERT INTO command
     */
    @NonNull
    SqlCommand insertFor(@NonNull ModelSchema modelSchema);

    /**
     * Generates the UPDATE command in a raw string representation and a compiled
     * prepared statement that can be bound later with inputs.
     *
     * @param modelSchema schema of the model
     * @return the SQL command that encapsulates the UPDATE command
     */
    @NonNull
    SqlCommand updateFor(@NonNull ModelSchema modelSchema,
                         @NonNull QueryPredicate predicate) throws DataStoreException;

    /**
     * Generates the DELETE command in a raw string representation.
     *
     * @param modelSchema schema of the model
     * @return the SQL command that encapsulates the DELETE command
     */
    @NonNull
    SqlCommand deleteFor(@NonNull ModelSchema modelSchema,
                         @NonNull QueryPredicate predicate) throws DataStoreException;
}
