/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.NonNull;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelAssociation;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.ModelSchemaRegistry;
import com.amplifyframework.core.model.query.QueryOptions;
import com.amplifyframework.core.model.query.Where;
import com.amplifyframework.core.model.query.predicate.QueryField;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.core.model.query.predicate.QueryPredicates;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.appsync.SerializedModel;
import com.amplifyframework.datastore.storage.sqlite.adapter.SQLiteTable;
import com.amplifyframework.logging.Logger;
import com.amplifyframework.util.Empty;
import com.amplifyframework.util.GsonFactory;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility class to help traverse a tree of models by relationship.
 */
final class SQLiteModelTree {
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-datastore");

    private final ModelSchemaRegistry registry;
    private final SQLCommandFactory commandFactory;
    private final SQLiteDatabase database;
    private final Gson gson;

    /**
     * Constructs a model family tree traversing utility.
     * @param registry model registry to search schema from
     * @param commandFactory SQL command factory
     * @param database SQLite database connection handle
     */
    SQLiteModelTree(ModelSchemaRegistry registry,
                    SQLCommandFactory commandFactory,
                    SQLiteDatabase database) {
        this.registry = registry;
        this.commandFactory = commandFactory;
        this.database = database;
        this.gson = GsonFactory.instance();
    }

    /**
     * Returns a map of descendants of a set of models (of same type).
     * A model is a child of its parent if it uses its parent's ID as foreign key.
     * @param root Collection of models to query its descendants of.
     * @return List of models that are descendants of given models. These models will
     *          have the correct model type and ID, but no other field will be populated.
     */
    <T extends Model> List<Model> descendantsOf(Collection<T> root) {
        if (Empty.check(root)) {
            return new ArrayList<>();
        }
        Map<ModelSchema, Set<String>> modelMap = new LinkedHashMap<>();
        ModelSchema rootSchema = registry.getModelSchemaForModelClass(getModelName(root.iterator().next()));
        Set<String> rootIds = new HashSet<>();
        for (T model : root) {
            rootIds.add(model.getId());
        }
        recurseTree(modelMap, rootSchema, rootIds);

        List<Model> descendants = new ArrayList<>();
        for (Map.Entry<ModelSchema, Set<String>> entry : modelMap.entrySet()) {
            ModelSchema schema = entry.getKey();
            for (String id : entry.getValue()) {
                // Create dummy model instance using just the ID and model type
                String dummyJson = gson.toJson(Collections.singletonMap("id", id));
                Model dummyItem = gson.fromJson(dummyJson, schema.getModelClass());
                descendants.add(dummyItem);
            }
        }
        return descendants;
    }

    private void recurseTree(
            Map<ModelSchema, Set<String>> map,
            ModelSchema modelSchema,
            Collection<String> parentIds
    ) {
        SQLiteTable parentTable = SQLiteTable.fromSchema(modelSchema);
        final String parentTableName = parentTable.getName();
        final String parentPrimaryKeyName = parentTable.getPrimaryKey().getName();
        for (ModelAssociation association : modelSchema.getAssociations().values()) {
            switch (association.getName()) {
                case "HasOne":
                case "HasMany":
                    String childModel = association.getAssociatedType(); // model name
                    ModelSchema childSchema = registry.getModelSchemaForModelClass(childModel);
                    SQLiteTable childTable = SQLiteTable.fromSchema(childSchema);
                    String childPrimaryKey = childTable.getPrimaryKey().getAliasedName();
                    QueryField queryField = QueryField.field(parentTableName, parentPrimaryKeyName);

                    // Chain predicates with OR operator.
                    QueryPredicate predicate = QueryPredicates.none();
                    for (String parentId : parentIds) {
                        QueryPredicate operation = queryField.eq(parentId);
                        predicate = predicate.or(operation);
                    }

                    // Collect every children one level deeper than current level
                    // SELECT * FROM <CHILD_TABLE> WHERE <PARENT> = <ID_1> OR <PARENT> = <ID_2> OR ...
                    QueryOptions options = Where.matches(predicate);
                    Set<String> childrenIds = new HashSet<>();
                    try (Cursor cursor = queryAll(childModel, options)) {
                        if (cursor != null && cursor.moveToFirst()) {
                            int index = cursor.getColumnIndexOrThrow(childPrimaryKey);
                            do {
                                childrenIds.add(cursor.getString(index));
                            } while (cursor.moveToNext());
                        }
                    } catch (DataStoreException exception) {
                        // Don't cut the search short. Populate rest of the tree.
                        LOG.error("Failed to query children of deleted model(s).", exception);
                    }

                    // Add queried result to the map
                    if (!childrenIds.isEmpty()) {
                        if (!map.containsKey(childSchema)) {
                            map.put(childSchema, childrenIds);
                        } else {
                            map.get(childSchema).addAll(childrenIds);
                        }
                        recurseTree(map, childSchema, childrenIds);
                    }
                    break;
                case "BelongsTo":
                default:
                    // Ignore other relationships
            }
        }
    }

    private Cursor queryAll(
            @NonNull String tableName,
            @NonNull QueryOptions options
    ) throws DataStoreException {
        final ModelSchema schema = registry.getModelSchemaForModelClass(tableName);
        final SqlCommand sqlCommand = commandFactory.queryFor(schema, options);
        final String rawQuery = sqlCommand.sqlStatement();
        final String[] bindings = sqlCommand.getBindingsAsArray();
        return database.rawQuery(rawQuery, bindings);
    }

    private String getModelName(@NonNull Model model) {
        if (model.getClass() == SerializedModel.class) {
            return ((SerializedModel) model).getModelName();
        } else {
            return model.getClass().getSimpleName();
        }
    }
}
