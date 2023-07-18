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
import android.database.sqlite.SQLiteException;
import androidx.annotation.NonNull;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelAssociation;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.SchemaRegistry;
import com.amplifyframework.core.model.SerializedModel;
import com.amplifyframework.datastore.storage.sqlite.adapter.SQLiteTable;
import com.amplifyframework.logging.Logger;
import com.amplifyframework.util.Empty;
import com.amplifyframework.util.GsonFactory;
import com.amplifyframework.util.Wrap;

import com.google.gson.Gson;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility class to help traverse a tree of models by relationship.
 */
final class SQLiteModelTree {
    private static final Logger LOG = Amplify.Logging.logger(CategoryType.DATASTORE, "amplify:aws-datastore");

    private final SchemaRegistry registry;
    private final SQLiteDatabase database;
    private final Gson gson;

    /**
     * Constructs a model family tree traversing utility.
     * @param registry model registry to search schema from
     * @param database SQLite database connection handle
     */
    SQLiteModelTree(SchemaRegistry registry,
                    SQLiteDatabase database) {
        this.registry = registry;
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
        Map<ModelSchema, Set<HashMap<String, String>>> modelMap = new LinkedHashMap<>();
        Model rootModel = root.iterator().next();
        ModelSchema rootSchema = registry.getModelSchemaForModelClass(getModelName(rootModel));
        Set<Serializable> rootIds = new HashSet<>();
        for (T model : root) {
            rootIds.add(model.getPrimaryKeyString());
        }
        recurseTree(modelMap, rootSchema, rootIds);

        List<Model> descendants = new ArrayList<>();
        /** This Map keeps information about the primary key fields for a particular schema. For models with composite
         primary key it will be more than one field. **/
        for (Map.Entry<ModelSchema, Set<HashMap<String, String>>> entry : modelMap.entrySet()) {
            ModelSchema schema = entry.getKey();
            String dummyJson;
            for (HashMap<String, String> keyMap : entry.getValue()) {
                if (rootModel.getClass() == SerializedModel.class) {
                    Map<String, Object> serializedData = new HashMap<String, Object>();
                    for (Map.Entry<String, String> keyMapEntry : keyMap.entrySet()) {
                        serializedData.put(keyMapEntry.getKey(), keyMapEntry.getValue());
                    }
                    SerializedModel dummyItem = SerializedModel.builder()
                            .modelSchema(schema)
                            .serializedData(serializedData)
                            .build();
                    descendants.add(dummyItem);
                } else {
                    /** Loop through primary key fields and get their respective values from the key map.
                     * Deserialize the key value to the model with primary key values populated.
                     * This will be used to create appsync delete mutation.  **/

                    HashMap<String, Serializable> hashMap = new HashMap<>();
                    if (schema.getPrimaryIndexFields().size() > 1) {
                        Iterator<String> pkFieldIterator = schema.getPrimaryIndexFields().listIterator();
                        while (pkFieldIterator.hasNext()) {
                            String pkField = pkFieldIterator.next();
                            hashMap.put(pkField, keyMap.get(pkField));
                        }
                        dummyJson = gson.toJson(hashMap);
                    } else {
                        // Create dummy model instance using just the ID and model type
                        dummyJson = gson.toJson(Collections.singletonMap("id", keyMap.get("id")));
                    }
                    Model dummyItem = gson.fromJson(dummyJson, schema.getModelClass());
                    descendants.add(dummyItem);
                }
            }
        }
        return descendants;
    }

    private void recurseTree(
            Map<ModelSchema, Set<HashMap<String, String>>> map,
            ModelSchema modelSchema,
            Collection<Serializable> parentIds
    ) {
        for (ModelAssociation association : modelSchema.getAssociations().values()) {
            switch (association.getName()) {
                case "HasOne":
                case "HasMany":
                    String childModel = association.getAssociatedType(); // model name
                    ModelSchema childSchema = registry.getModelSchemaForModelClass(childModel);
                    SQLiteTable childTable = SQLiteTable.fromSchema(childSchema);
                    List<String> childFields = new ArrayList<>();
                    String parentId;
                    try {
                        /** Get the primary key field name for the tables in local database.**/
                        childFields.add(childTable.getPrimaryKey().getName());
                        /** Get the primary key field names for the Model.**/
                        if (childSchema.getPrimaryIndexFields().size() > 1) {
                            childFields.addAll(childSchema.getPrimaryIndexFields());
                        }
                        parentId = SQLiteTable.getForeignKeyColumnName(childSchema.getVersion(),
                                // get a map of associations
                                association.getAssociatedName(), childSchema.getAssociations()
                                        // get the target field (parent) name
                                        .get(association.getAssociatedName()));
                    } catch (NullPointerException unexpectedAssociation) {
                        LOG.warn("Foreign key was not found due to unidirectional relationship without " +
                                        "@BelongsTo. " + "Failed to publish cascading mutations.",
                                unexpectedAssociation);
                        return;
                    }

                    // Collect every children one level deeper than current level
                    Set<Serializable> childrenIds = new HashSet<>();
                    Set<HashMap<String, String>> childrenIdMap = new HashSet<>();
                    try (Cursor cursor = queryChildren(childTable.getName(), childFields, parentId, parentIds)) {
                        /** Populate the mapOfModelPrimaryKeys with the values of primary key for local sql table and
                         *  the primary key/ keys for the model**/
                        if (cursor != null && cursor.moveToFirst()) {
                            do {
                                HashMap<String, String> mapOfModelPrimaryKeys = new HashMap<>();
                                for (String field : childFields) {
                                    int index = cursor.getColumnIndexOrThrow(field);
                                    String fieldValue = cursor.getString(index);
                                    if (!field.equals(SQLiteTable.PRIMARY_KEY_FIELD_NAME)) {
                                        mapOfModelPrimaryKeys.put(field, fieldValue);
                                    }
                                    if (field.equals(childTable.getPrimaryKey().getName())) {
                                        childrenIds.add(fieldValue);
                                    }
                                }
                                childrenIdMap.add(mapOfModelPrimaryKeys);
                            } while (cursor.moveToNext());
                        }
                    } catch (SQLiteException exception) {
                        // Don't cut the search short. Populate rest of the tree.
                        LOG.warn("Failed to query children of deleted model(s).", exception);
                    }

                    // Add queried result to the map
                    if (!childrenIds.isEmpty()) {
                        if (!map.containsKey(childSchema)) {
                            map.put(childSchema, childrenIdMap);
                        } else {
                            map.get(childSchema).addAll(childrenIdMap);
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

    private Cursor queryChildren(
            @NonNull String childTable,
            @NonNull List<String> childIdFields,
            @NonNull String parentIdField,
            @NonNull Collection<Serializable> parentIds
    ) {
        // Wrap each ID with single quote
        StringBuilder quotedIds = new StringBuilder();
        for (Iterator<Serializable> ids = parentIds.iterator(); ids.hasNext();) {
            quotedIds.append(Wrap.inSingleQuotes(ids.next().toString()));
            if (ids.hasNext()) {
                quotedIds.append(SqlKeyword.SEPARATOR);
            }
        }
        // SELECT <child_id> FROM <child_table> WHERE <parent_id> IN (<id_1>, <id_2>, ...)
        String queryString = String.valueOf(SqlKeyword.SELECT) +
                SqlKeyword.DELIMITER +
                getChildFieldString(childIdFields) +
                SqlKeyword.DELIMITER +
                SqlKeyword.FROM +
                SqlKeyword.DELIMITER +
                Wrap.inBackticks(childTable) +
                SqlKeyword.DELIMITER +
                SqlKeyword.WHERE +
                SqlKeyword.DELIMITER +
                Wrap.inBackticks(parentIdField) +
                SqlKeyword.DELIMITER +
                SqlKeyword.IN +
                SqlKeyword.DELIMITER +
                Wrap.inParentheses(quotedIds.toString()) +
                ";";
        return database.rawQuery(queryString, new String[0]);
    }

    private String getChildFieldString(List<String> childIdFields) {
        StringBuilder builder = new StringBuilder();
        Iterator<String> childFieldsIterator = childIdFields.listIterator();
        while (childFieldsIterator.hasNext()) {
            builder.append(Wrap.inBackticks(childFieldsIterator.next()));
            if (childFieldsIterator.hasNext()) {
                builder.append(", ");
            }
        }
        return builder.toString();
    }

    private String getModelName(@NonNull Model model) {
        if (model.getClass() == SerializedModel.class) {
            return ((SerializedModel) model).getModelName();
        } else {
            return model.getClass().getSimpleName();
        }
    }
}
