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

package com.amplifyframework.datastore.model;

import androidx.annotation.NonNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Schema of a Model that implements the {@link Model} interface.
 * The schema encapsulates the metadata information of a Model.
 */
public class ModelSchema {
    // Name of the Java Class of the Model.
    private final String name;

    // Name of the target type. For example: name of
    // the graphQL type in the cloud.
    private final String targetName;

    // A map that contains the fields of a Model.
    // The key is the name of the instance variable in the Java class that represents the Model
    // The value is the ModelField object that encapsulates all the information about the instance variable.
    private final Map<String, ModelField> fields;

    // Maintain a sorted copy of all the fields of a Model
    private final List<Map.Entry<String, ModelField>> sortedFields;

    static <T extends Model> ModelSchema fromModelClass(@NonNull Class<? extends Model> clazz) {
        final Set<Field> classFields = findFields(clazz, com.amplifyframework.datastore.annotations.Field.class);
        final Map<String, ModelField> fields = new HashMap<String, ModelField>();
        for (Field field: classFields) {
            com.amplifyframework.datastore.annotations.Field annotation = null;
            if (field.getAnnotation(com.amplifyframework.datastore.annotations.Field.class) != null) {
                annotation = field.getAnnotation(com.amplifyframework.datastore.annotations.Field.class);
            }
            ModelField modelField = new ModelField(
                    field.getName(),
                    annotation.targetName(),
                    field.getType().getName(),
                    annotation.isRequired(),
                    Collection.class.isAssignableFrom(field.getType()),
                    "id".equals(field.getName()),
                    Model.class.isAssignableFrom(field.getType()) ? field.getType().getName() : null
            );
            fields.put(field.getName(), modelField);
        }
        return new ModelSchema(clazz.getSimpleName(), clazz.getSimpleName(), fields);
    }

    /**
     * Construct the ModelSchema object.
     *
     * @param name name of the Model class.
     * @param targetName
     *                   that the Model is targeting against.
     * @param fields map of fieldName and the fieldObject
     *               of all the fields of the model.
     */
    public ModelSchema(@NonNull String name,
                       @NonNull String targetName,
                       @NonNull Map<String, ModelField> fields) {
        this.name = name;
        this.targetName = targetName;
        this.fields = fields;
        this.sortedFields = sortModelFields();
    }

    /**
     * @return the name of the Model class.
     */
    public String getName() {
        return name;
    }

    /**
     * @return map of fieldName and the fieldObject
     *         of all the fields of the model.
     */
    public Map<String, ModelField> getFields() {
        return fields;
    }

    /**
     * @return the name of the target.
     */
    public String getTargetName() {
        return targetName;
    }

    /**
     * @return the primary key of the Model.
     */
    public ModelField getPrimaryKey() {
        for (Map.Entry<String, ModelField> field: sortedFields) {
            if (field.getValue().isPrimaryKey()) {
                return field.getValue();
            }
        }
        return null;
    }

    private List<Map.Entry<String, ModelField>> sortModelFields() {
        if (fields == null) {
            return null;
        }

        // Create a list from elements of sortedFields
        List<Map.Entry<String, ModelField> > modelFieldEntries =
                new LinkedList<Map.Entry<String, ModelField> >(fields.entrySet());

        // Returns an array of the values sorted by some pre-defined rules:
        //
        // 1. primary key comes always first
        // 2. foreign keys come always at the end
        // 3. the other sortedFields are sorted alphabetically
        //
        // This is useful so code that uses the sortedFields to generate queries and other
        // persistence-related operations guarantee that the results are always consistent.
        Collections.sort(modelFieldEntries, new Comparator<Map.Entry<String, ModelField>>() {
            public int compare(Map.Entry<String, ModelField> fieldEntryOne,
                               Map.Entry<String, ModelField> fieldEntryOther) {
                ModelField fieldOne = fieldEntryOne.getValue();
                ModelField fieldOther = fieldEntryOther.getValue();

                if (fieldOne.isPrimaryKey()) {
                    return 1;
                }
                if (fieldOther.isPrimaryKey()) {
                    return -1;
                }
                if (fieldOne.isConnected() && !fieldOther.isConnected()) {
                    return -1;
                }
                if (fieldOne.isConnected() && !fieldOther.isConnected()) {
                    return 1;
                }
                return fieldOne.getName().compareTo(fieldOther.getName());
            }
        });

        return modelFieldEntries;
    }

    private static Set<Field> findFields(@NonNull Class<?> clazz,
                                         @NonNull Class<? extends Annotation> annotation) {
        Set<Field> set = new HashSet<>();
        Class<?> c = clazz;
        while (c != null) {
            for (Field field : c.getDeclaredFields()) {
                if (field.isAnnotationPresent(annotation)) {
                    set.add(field);
                }
            }
            c = c.getSuperclass();
        }
        return set;
    }
}
