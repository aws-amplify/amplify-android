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
 * A factory that produces the SQLite commands from the
 * {@link ModelSchema} and the {@link com.amplifyframework.datastore.model.Model}.
 */
interface SQLCommandFactory {
    /**
     * Generates the CREATE TABLE SQL command from the {@link ModelSchema}.
     * @param modelSchema the schema of a {@link com.amplifyframework.datastore.model.Model}
     *                    for which a CREATE TABLE SQL command needs to be generated.
     * @return the CREATE TABLE SQL command
     */
    SqlCommand createTableFor(@NonNull ModelSchema modelSchema);

    /**
     * Generates the CREATE INDEX SQL command from the {@link ModelSchema}.
     * @param modelSchema the schema of a {@link com.amplifyframework.datastore.model.Model}
     *                    for which a CREATE INDEX SQL command needs to be generated.
     * @return the CREATE INDEX SQL command
     */
    SqlCommand createIndexFor(@NonNull ModelSchema modelSchema);
}
