/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.datastore.storage.sqlite.migrations;

import android.database.sqlite.SQLiteDatabase;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.logging.Logger;

public class AddSyncExpressionToLastSyncMetadata implements ModelMigration{
    private static final Logger LOG = Amplify.Logging.logger(CategoryType.DATASTORE, "amplify:aws-datastore");
    private final SQLiteDatabase database;
    private final ModelProvider modelProvider;

    /**
     * Constructor for the migration class.
     * @param database Connection to the SQLite database.
     * @param modelProvider The model provider.
     */
    public AddSyncExpressionToLastSyncMetadata(SQLiteDatabase database, ModelProvider modelProvider) {
        this.database = database;
        this.modelProvider = modelProvider;
    }

    @Override
    public void apply() {
        LOG.info("AddSyncExpressionToLastSyncMetadata is applied");
    }
}
