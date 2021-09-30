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

package com.amplifyframework.datastore.storage.sqlite;

import android.database.Cursor;
import androidx.annotation.NonNull;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.SchemaRegistry;
import com.amplifyframework.core.model.query.QueryOptions;
import com.amplifyframework.core.model.query.predicate.QueryField;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.storage.sqlite.adapter.SQLiteTable;
import com.amplifyframework.logging.Logger;
import com.amplifyframework.util.GsonFactory;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class SqlQueryProcessor {

    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-datastore");
    private final SchemaRegistry modelSchemaRegistry;
    private final SQLCommandFactory sqlCommandFactory;
    private final SQLCommandProcessor sqlCommandProcessor;
    private final Gson gson;

    SqlQueryProcessor(SQLCommandProcessor sqlCommandProcessor,
                      SQLCommandFactory sqlCommandFactory,
                      SchemaRegistry modelSchemaRegistry) {
        this.sqlCommandProcessor = sqlCommandProcessor;
        this.sqlCommandFactory = sqlCommandFactory;
        this.modelSchemaRegistry = modelSchemaRegistry;
        this.gson = GsonFactory.instance();
    }

    <T extends Model> List<T> queryOfflineData(@NonNull Class<T> itemClass,
                                               @NonNull QueryOptions options,
                                               @NonNull Consumer<DataStoreException> onError) {
        final ModelSchema modelSchema = modelSchemaRegistry.getModelSchemaForModelClass(itemClass.getSimpleName());
        final List<T> models = new ArrayList<>();

        try (Cursor cursor = sqlCommandProcessor.rawQuery(sqlCommandFactory.queryFor(modelSchema, options))) {
            LOG.debug("Querying item for: " + itemClass.getSimpleName());
            final SQLiteModelFieldTypeConverter converter =
                    new SQLiteModelFieldTypeConverter(modelSchema, modelSchemaRegistry, gson);

            if (cursor == null) {
                onError.accept(new DataStoreException(
                        "Error in getting a cursor to the table for class: " + itemClass.getSimpleName(),
                        AmplifyException.TODO_RECOVERY_SUGGESTION
                ));
            } else if (cursor.moveToFirst()) {
                do {
                    Map<String, Object> map = converter.buildMapForModel(cursor);
                    String jsonString = gson.toJson(map);
                    models.add(gson.fromJson(jsonString, itemClass));
                } while (cursor.moveToNext());
            }
        } catch (Exception exception) {
            onError.accept(new DataStoreException(
                    "Error in querying the model.", exception,
                    "See attached exception for details."
            ));
        }
        return models;
    }

    boolean modelExists(Model model, QueryPredicate predicate) throws DataStoreException {
        final String modelName = model.getModelName();
        final ModelSchema schema = modelSchemaRegistry.getModelSchemaForModelClass(modelName);
        final SQLiteTable table = SQLiteTable.fromSchema(schema);
        final String tableName = table.getName();
        final String primaryKeyName = table.getPrimaryKey().getName();
        final QueryPredicate matchId = QueryField.field(tableName, primaryKeyName).eq(model.getId());
        final QueryPredicate condition = predicate.and(matchId);
        return sqlCommandProcessor.executeExists(sqlCommandFactory.existsFor(schema, condition));
    }
}
