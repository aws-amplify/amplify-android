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
import com.amplifyframework.datastore.storage.sqlite.adapter.SQLiteTable;
import com.amplifyframework.logging.Logger;
import com.amplifyframework.util.Empty;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
    }

    /**
     * Returns a map of descendants of a set of models (of same type).
     * A model is a child of its parent if it uses its parent's ID as foreign key.
     * @param root Collection of models to query its descendants of.
     * @return Map of descendants keyed by model schema. The value contains a set of
     *          descendants' IDs for that model type.
     */
    <T extends Model> Map<ModelSchema, Set<String>> descendantsOf(Collection<T> root) {
        if (Empty.check(root)) {
            throw new IllegalArgumentException("Cannot traverse tree from an empty root.");
        }
        Map<ModelSchema, Set<String>> descendants = new LinkedHashMap<>();
        ModelSchema rootSchema = registry.getModelSchemaForModelInstance(root.iterator().next());
        Set<String> rootIds = new HashSet<>();
        for (T model : root) {
            rootIds.add(model.getId());
        }
        recurseTree(descendants, rootSchema, rootIds);
        return descendants;
    }

    private void recurseTree(
            Map<ModelSchema, Set<String>> map,
            ModelSchema modelSchema,
            Collection<String> parentIds
    ) {
        SQLiteTable parentTable = SQLiteTable.fromSchema(modelSchema);
        for (ModelAssociation association : modelSchema.getAssociations().values()) {
            switch (association.getName()) {
                case "HasOne":
                case "HasMany":
                    String childModel = association.getAssociatedType(); // model name
                    ModelSchema childSchema = registry.getModelSchemaForModelClass(childModel);
                    SQLiteTable childTable = SQLiteTable.fromSchema(childSchema);
                    String childPrimaryKey = childTable.getPrimaryKey().getAliasedName();
                    QueryField queryField = QueryField.field(parentTable.getPrimaryKeyColumnName());

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
}
