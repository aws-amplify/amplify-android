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

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.datastore.model.SystemModelsProviderFactory;
import com.amplifyframework.logging.Logger;
import com.amplifyframework.util.Wrap;

import java.util.Set;

/**
 * Add model name to ModelMetadata key.
 */
public final class AddModelNameToModelMetadataKey implements ModelMigration {
    private static final Logger LOG = Amplify.Logging.logger(CategoryType.DATASTORE, "amplify:aws-datastore");
    private final SQLiteDatabase database;
    private final ModelProvider modelProvider;

    /**
     * Constructor for the migration class.
     * @param database Connection to the SQLite database.
     * @param modelProvider The model provider.
     */
    public AddModelNameToModelMetadataKey(SQLiteDatabase database, ModelProvider modelProvider) {
        this.database = database;
        this.modelProvider = modelProvider;
    }

    @Override
    public void apply() {
        if (!needsMigration()) {
            LOG.debug("No ModelMetadata migration needed.");
            return;
        }
        try (Cursor duplicateIds = duplicateIds(modelProvider.modelNames())) {
            database.beginTransaction();
            if (duplicateIds.moveToNext()) {
                // Truncate the data in ModelMetadata and LastSyncMetadata
                LOG.debug("There are duplicate IDs. Clearing ModelMetadata to force base sync.");
                database.execSQL("DELETE FROM ModelMetadata;", new String[]{});
                database.execSQL("DELETE FROM LastSyncMetadata;", new String[]{});
            } else {
                LOG.debug("No duplicate IDs found. Modifying and backfilling ModelMetadata.");
                // Create a copy of the the ModelMetadata table with the new itemModelName column.
                database.execSQL("DROP TABLE IF EXISTS ModelMetadataCopy;",
                                                 new String[]{});
                // Create a copy of the ModelMetadata table
                database.execSQL(createModelMetadataTable("ModelMetadataCopy"), new String[]{});
                // Backfill data into copy table
                database.execSQL(backfillModelMetadataQuery(), new String[]{});
                //Drop the existing ModelMetadata table
                database.execSQL("DROP TABLE ModelMetadata;", new String[]{});
                database.execSQL(
                        "ALTER TABLE ModelMetadataCopy RENAME TO ModelMetadata;", new String[]{});

            }
            database.setTransactionSuccessful();
        } finally {
            if (database.inTransaction()) {
                database.endTransaction();
            }
        }
    }

    private Cursor duplicateIds(Set<String> modelNames) {
        Set<String> systemModelNames = SystemModelsProviderFactory.create().modelNames();
        StringBuilder sb = new StringBuilder("");
        for (String modelName : modelNames) {
            // Exclude system tables
            if (systemModelNames.contains(modelName)) {
                continue;
            }
            if (!"".equals(sb.toString())) {
                sb.append(" UNION ALL ");
            }
            sb.append("SELECT id,")
              .append(Wrap.inSingleQuotes(modelName))
              .append(" as tableName ")
              .append("FROM ")
                .append(modelName);
        }
        sb.insert(0, "SELECT id, tableName, count(id) as count FROM (");
        sb.append(") GROUP BY id HAVING count > 1");
        LOG.debug("Check for duplicate IDs:" + sb.toString());
        return database.rawQuery(sb.toString(), new String[]{});
    }

    private String backfillModelMetadataQuery() {
        Set<String> systemModelNames = SystemModelsProviderFactory.create().modelNames();
        StringBuilder sb = new StringBuilder("");
        for (String modelName : modelProvider.modelNames()) {
            // Exclude system tables
            if (systemModelNames.contains(modelName)) {
                continue;
            }
            if (!"".equals(sb.toString())) {
                sb.append(" UNION ALL ");
            }
            sb.append("SELECT id,")
              .append(Wrap.inSingleQuotes(modelName))
              .append(" as tableName ")
              .append("FROM ")
                .append(modelName);
        }
        sb.insert(0, "select models.tableName || '|' || mm.id," +
                "mm._deleted, " +
                "mm._lastChangedAt," +
                "mm._version from ModelMetadata mm INNER JOIN (");
        sb.append(") as models on mm.id=models.id;");
        sb.insert(0, "INSERT INTO ModelMetadataCopy(id,_deleted,_lastChangedAt,_version) ");
        LOG.debug("Backfill query: " + sb.toString());
        return sb.toString();
    }

    private String createModelMetadataTable(String tableName) {
        return "create table " +
                tableName + " " +
                "(id text NOT NULL, " +
                "_deleted integer, " +
                "_lastChangedAt integer," +
                "_version integer, " +
                "PRIMARY KEY (id))";
    }

    private boolean needsMigration() {
        final String sql = "select (select count(1) as count from ModelMetadata) as allRecords, " +
                "(select count(1) as count from ModelMetadata where id like '%|%') as newKeys";
        try (Cursor results = database.rawQuery(sql, new String[]{})) {
            if (results.moveToNext()) {
                int allRecords = results.getInt(0);
                int newKeyFormat = results.getInt(1);
                if (allRecords == 0 || newKeyFormat > 0) {
                    return false;
                }
            }

        }
        return true;
    }
}
