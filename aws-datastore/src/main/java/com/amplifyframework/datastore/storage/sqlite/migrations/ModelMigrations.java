/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.util.Immutable;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that serves as an entry point for model migrations.
 */
public class ModelMigrations {
    private final List<ModelMigration> modelMigrations;

    /**
     * Constructor for the ModelMigrations class.
     * @param databaseConnectionHandle A connection to the local SQLite database.
     * @param modelsProvider An instance of the model provider.
     */
    public ModelMigrations(SQLiteDatabase databaseConnectionHandle, ModelProvider modelsProvider) {
        List<ModelMigration> migrationClasses = new ArrayList<>();
        migrationClasses.add(new AddModelNameToModelMetadataKey(databaseConnectionHandle, modelsProvider));
        this.modelMigrations = Immutable.of(migrationClasses);
    }

    /**
     * Apply all the migrations.
     */
    public void apply() {
        for (ModelMigration m : modelMigrations) {
            m.apply();
        }
    }
}
