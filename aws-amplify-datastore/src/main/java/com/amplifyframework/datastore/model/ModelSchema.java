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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Schema of a Model that implements the {@link Model} interface.
 * The schema encapsulates the metadata information of a Model.
 */
public final class ModelSchema {
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
    // This is useful so code that uses the sortedFields to generate queries and other
    // persistence-related operations guarantee that the results are always consistent.
    private final List<Map.Entry<String, ModelField>> sortedFields;

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
                       @NonNull SortedMap<String, ModelField> fields) {
        this.name = name;
        this.targetName = targetName;
        this.fields = fields;
        this.sortedFields = sortModelFields();
    }

    /**
     * Returns the name of the Model class.
     *
     * @return the name of the Model class.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the map of fieldName and the fieldObject
     * of all the fields of the model.
     *
     * @return map of fieldName and the fieldObject
     *         of all the fields of the model.
     */
    public Map<String, ModelField> getFields() {
        return fields;
    }

    /**
     * Returns the name of the target.
     *
     * @return the name of the target.
     */
    public String getTargetName() {
        return targetName;
    }

    /**
     * Returns the primary key of the Model.
     *
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

    /**
     * Construct the ModelSchema from the {@link Model} class.
     *
     * @param clazz the instance of a model class
     * @param <T> parameter type of a data model that conforms
     *           to the {@link Model} interface.
     * @return the ModelSchema object.
     */
    static <T extends Model> ModelSchema fromModelClass(@NonNull Class<? extends Model> clazz) {
        final Set<Field> classFields = findFields(clazz, com.amplifyframework.datastore.annotations.ModelField.class);
        final TreeMap<String, ModelField> fields = new TreeMap<>();
        for (Field field: classFields) {
            com.amplifyframework.datastore.annotations.ModelField annotation = null;
            if (field.getAnnotation(com.amplifyframework.datastore.annotations.ModelField.class) != null) {
                annotation = field.getAnnotation(com.amplifyframework.datastore.annotations.ModelField.class);
            }
            final ModelField modelField = ModelField.builder()
                    .name(field.getName())
                    .targetName(annotation.targetName())
                    .targetType(annotation.targetType())
                    .isRequired(annotation.isRequired())
                    .isArray(Collection.class.isAssignableFrom(field.getType()))
                    .isPrimaryKey(PrimaryKey.matches(field.getName()))
                    .connectionTarget(Model.class.isAssignableFrom(field.getType())
                            ? field.getType().getName()
                            : null)
                    .build();
            fields.put(field.getName(), modelField);
        }
        return new ModelSchema(clazz.getSimpleName(), clazz.getSimpleName(), fields);
    }

    private List<Map.Entry<String, ModelField>> sortModelFields() {
        if (fields == null) {
            return null;
        }

        // Create a list from elements of sortedFields
        final List<Map.Entry<String, ModelField>> modelFieldEntries = new LinkedList<>(fields.entrySet());

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
