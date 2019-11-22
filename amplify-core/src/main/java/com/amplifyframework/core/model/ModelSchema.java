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

package com.amplifyframework.core.model;

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.Immutable;
import com.amplifyframework.core.model.annotations.BelongsTo;
import com.amplifyframework.core.model.annotations.Connection;
import com.amplifyframework.core.model.annotations.Index;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.util.FieldFinder;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Schema of a Model that implements the {@link Model} interface.
 * The schema encapsulates the metadata information of a Model.
 */
public final class ModelSchema {

    // Logcat Tag.
    private static final String TAG = ModelSchema.class.getSimpleName();

    // Name of the Java Class of the Model.
    private final String name;

    // the name of the Model in the target. For example: the name of the
    // model in the GraphQL Schema.
    private final String targetModelName;

    // A map that contains the fields of a Model.
    // The key is the name of the instance variable in the Java class that represents the Model
    // The value is the ModelField object that encapsulates all the information about the instance variable.
    private final Map<String, ModelField> fields;

    // A map that contains the connections of a Model.
    // The key is the name of the instance variable in the Java class that represents one of Model's connections
    // The value is the ModelConnection object that encapsulates all the information about the instance variable.
    private final Map<String, ModelConnection> connections;

    // Maintain a sorted copy of all the fields of a Model
    // This is useful so code that uses the sortedFields to generate queries and other
    // persistence-related operations guarantee that the results are always consistent.
    private final List<ModelField> sortedFields;

    // Specifies the index of a Model.
    private final ModelIndex modelIndex;

    private ModelSchema(String name,
                        String targetModelName,
                        Map<String, ModelField> fields,
                        Map<String, ModelConnection> connections,
                        ModelIndex modelIndex) {
        this.name = name;
        this.targetModelName = targetModelName;
        this.fields = fields;
        this.connections = connections;
        this.modelIndex = modelIndex;
        this.sortedFields = sortModelFields();
    }

    /**
     * Return the builder object.
     * @return the builder object.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Construct the ModelSchema from the {@link Model} class.
     *
     * @param clazz the instance of a model class
     * @return the ModelSchema object.
     */
    public static ModelSchema fromModelClass(@NonNull Class<? extends Model> clazz) {
        try {
            final Set<Field> classFields = FieldFinder.findFieldsIn(clazz);
            final TreeMap<String, ModelField> fields = new TreeMap<>();
            final TreeMap<String, ModelConnection> connections = new TreeMap<>();
            final ModelIndex modelIndex = getModelIndex(clazz);
            String targetModelName = null;
            if (clazz.isAnnotationPresent(ModelConfig.class)) {
                targetModelName = clazz.getAnnotation(ModelConfig.class).targetName();
                if (targetModelName.isEmpty()) {
                    targetModelName = clazz.getSimpleName();
                }
            }

            for (Field field : classFields) {
                final ModelConnection modelConnection = createModelConnection(field);
                if (modelConnection != null) {
                    connections.put(field.getName(), modelConnection);
                }

                final ModelField modelField = createModelField(field);
                if (modelField != null) {
                    fields.put(field.getName(), modelField);
                }
            }
            return ModelSchema.builder()
                    .name(clazz.getSimpleName())
                    .targetModelName(targetModelName)
                    .fields(fields)
                    .connections(connections)
                    .modelIndex(modelIndex)
                    .build();
        } catch (Exception exception) {
            throw new ModelSchemaException("Error in constructing a ModelSchema.", exception);
        }
    }

    // Utility method to extract connection metadata from a field
    private static ModelConnection createModelConnection(Field field) {
        Connection connection = field.getAnnotation(Connection.class);
        if (connection != null) {
            return ModelConnection.builder()
                    .name(connection.name())
                    .keyField(connection.keyField())
                    .sortField(connection.sortField())
                    .limit(connection.limit())
                    .keyName(connection.keyName())
                    .fields(Arrays.asList(connection.fields()))
                    .relationship(RelationalModel.valueOf(connection.relationship()))
                    .connectionTarget(field.getType().getName())
                    .build();
        }
        return null;
    }

    // Utility method to extract field metadata
    private static ModelField createModelField(Field field) {
        com.amplifyframework.core.model.annotations.ModelField annotation =
                field.getAnnotation(com.amplifyframework.core.model.annotations.ModelField.class);
        if (annotation != null) {
            final String fieldName = field.getName();
            final String fieldType = field.getType().getSimpleName();
            final String targetName = annotation.targetName();
            final String targetType = annotation.targetType();
            return ModelField.builder()
                    .name(fieldName)
                    .type(fieldType)
                    .targetName(targetName.isEmpty() ? fieldName : targetName)
                    .targetType(targetType.isEmpty() ? fieldType : targetType)
                    .isRequired(annotation.isRequired())
                    .isArray(Collection.class.isAssignableFrom(field.getType()))
                    .isEnum(Enum.class.isAssignableFrom(field.getType()))
                    .isModel(Model.class.isAssignableFrom(field.getType()))
                    .isPrimaryKey(PrimaryKey.matches(fieldName))
                    .belongsTo(field.isAnnotationPresent(BelongsTo.class)
                            ? field.getAnnotation(BelongsTo.class).type().getSimpleName()
                            : null)
                    .build();
        }
        return null;
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
     * Returns the name of the Model in the target. For example: the name of the
     * model in the GraphQL Schema.
     * @return the name of the Model in the target. For example: the name of the
     *         model in the GraphQL Schema.
     */
    public String getTargetModelName() {
        return targetModelName;
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
     * Returns a sorted copy of all the fields of a Model.
     *
     * @return list of fieldName and the fieldObject of all
     *          the fields of the model in sorted order.
     */
    public List<ModelField> getSortedFields() {
        return sortedFields;
    }

    /**
     * Returns the attributes of a {@link Model}.
     *
     * @return the attributes of a {@link Model}.
     */
    public ModelIndex getModelIndex() {
        return modelIndex;
    }

    /**
     * Returns the primary key of the Model.
     *
     * @return the primary key of the Model.
     */
    public ModelField getPrimaryKey() {
        for (ModelField field: sortedFields) {
            if (field.isPrimaryKey()) {
                return field;
            }
        }
        return null;
    }

    /**
     * Returns the list of foreign keys of the Model.
     *
     * @return the list of foreign keys of the Model.
     */
    public List<ModelField> getForeignKeys() {
        List<ModelField> foreignKeys = new LinkedList<>();
        for (ModelField field: sortedFields) {
            if (field.isForeignKey()) {
                foreignKeys.add(field);
            }
        }
        return Immutable.of(foreignKeys);
    }

    /**
     * Returns a map of field to connection of the model.
     *
     * @return a map of field to connection of the model.
     */
    public Map<String, ModelConnection> getConnections() {
        return Immutable.of(connections);
    }

    /**
     * Creates a map of the fields in this schema to the actual values in the provided object.
     * NOTE: This uses the schema target names as the keys, not the local Java field names.
     * @param instance An instance of this model populated with values to map
     * @return a map of the target fields in the schema to the actual values in the provided object
     * @throws AmplifyException if the object does not match the fields in this schema
     */
    public Map<String, Object> getMapOfFieldNameAndValues(Model instance) throws AmplifyException {
        HashMap<String, Object> result = new HashMap<>();

        if (!instance.getClass().getSimpleName().equals(this.getName())) {
            throw new AmplifyException(
                    "The object provided is not an instance of this Model." +
                    "Please provide an instance of " + this.getName() + " which this is a schema for.");
        }

        for (ModelField field : this.getSortedFields()) {
            try {
                Field privateField = instance.getClass().getDeclaredField(field.getName());
                privateField.setAccessible(true);
                result.put(field.getTargetName(), privateField.get(instance));
            } catch (Exception exception) {
                throw new AmplifyException("An invalid field was provided - " +
                        field.getName() +
                        " is not present in " +
                        instance.getClass().getSimpleName(),
                        exception,
                        "Check if this model schema is a correct representation of the fields in the provided Object",
                        false);
            }
        }

        return result;
    }

    private static ModelIndex getModelIndex(@NonNull Class<? extends Model> clazz) {
        final ModelIndex.Builder builder = ModelIndex.builder();

        if (clazz.isAnnotationPresent(Index.class)) {
            Index indexAnnotation = clazz.getAnnotation(Index.class);
            if (indexAnnotation != null) {
                builder.indexName(indexAnnotation.name());
                builder.indexFieldNames(Arrays.asList(indexAnnotation.fields()));
            }
        }
        return builder.build();
    }

    private List<ModelField> sortModelFields() {
        if (fields == null) {
            return null;
        }

        // Create a list from elements of sortedFields
        final List<ModelField> modelFieldEntries = new LinkedList<>(fields.values());

        // Returns an array of the values sorted by some pre-defined rules:
        //
        // 1. primary key comes always first
        // 2. foreign keys come always at the end
        // 3. the other sortedFields are sorted alphabetically
        //
        // This is useful so code that uses the sortedFields to generate queries and other
        // persistence-related operations guarantee that the results are always consistent.
        Collections.sort(modelFieldEntries, (fieldOne, fieldOther) -> {

            if (fieldOne.isPrimaryKey()) {
                return -1;
            }
            if (fieldOther.isPrimaryKey()) {
                return 1;
            }
            if (fieldOne.isForeignKey() && !fieldOther.isForeignKey()) {
                return 1;
            }
            if (!fieldOne.isForeignKey() && fieldOther.isForeignKey()) {
                return -1;
            }
            return fieldOne.getName().compareTo(fieldOther.getName());
        });

        return modelFieldEntries;
    }

    @Override
    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        ModelSchema that = (ModelSchema) thatObject;

        if (!ObjectsCompat.equals(name, that.name)) {
            return false;
        }
        if (!ObjectsCompat.equals(targetModelName, that.targetModelName)) {
            return false;
        }
        if (!ObjectsCompat.equals(fields, that.fields)) {
            return false;
        }
        if (!ObjectsCompat.equals(sortedFields, that.sortedFields)) {
            return false;
        }
        return ObjectsCompat.equals(modelIndex, that.modelIndex);
    }

    @SuppressWarnings("checkstyle:MagicNumber") // 31 is auto-generated by IDE
    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (targetModelName != null ? targetModelName.hashCode() : 0);
        result = 31 * result + (fields != null ? fields.hashCode() : 0);
        result = 31 * result + (connections != null ? connections.hashCode() : 0);
        result = 31 * result + (sortedFields != null ? sortedFields.hashCode() : 0);
        result = 31 * result + (modelIndex != null ? modelIndex.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ModelSchema{" +
            "name='" + name + '\'' +
            ", targetModelName='" + targetModelName + '\'' +
            ", fields=" + fields +
            ", connections" + connections +
            ", sortedFields=" + sortedFields +
            ", modelIndex=" + modelIndex +
            '}';
    }

    /**
     * The Builder to build the {@link ModelSchema} object.
     */
    public static final class Builder {
        // the name of the Model class.
        private String name;

        // the name of the Model in the target. For example: the name of the
        // model in the GraphQL Schema.
        private String targetModelName;
        private Map<String, ModelField> fields;
        private Map<String, ModelConnection> connections;
        private ModelIndex modelIndex;

        /**
         * Set the the name of the Model class.
         * @param name the name of the Model class.
         * @return the builder object
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * the name of the Model in the target. For example: the name of the
         * model in the GraphQL Schema.
         * @param targetModelName the name of the Model in the target. For example: the name of the
         *                        model in the GraphQL Schema.
         * @return the builder object
         */
        public Builder targetModelName(String targetModelName) {
            this.targetModelName = targetModelName;
            return this;
        }

        /**
         * Set the map of fieldName and the fieldObject of all the fields of the model.
         * @param fields the map of fieldName and the fieldObject of all the fields of the model.
         * @return the builder object.
         */
        public Builder fields(Map<String, ModelField> fields) {
            this.fields = fields;
            return this;
        }

        /**
         * Set the map of fieldName and the connectionObject of all the connection fields of the model.
         * @param connections the map of fieldName and the connectionObjects.
         * @return the builder object.
         */
        public Builder connections(Map<String, ModelConnection> connections) {
            this.connections = connections;
            return this;
        }

        /**
         * Set the index of a model.
         * @param modelIndex the index of the model.
         * @return the builder object.
         */
        public Builder modelIndex(ModelIndex modelIndex) {
            this.modelIndex = modelIndex;
            return this;
        }

        /**
         * Return the ModelSchema object.
         * @return the ModelSchema object.
         */
        public ModelSchema build() {
            return new ModelSchema(name,
                    targetModelName,
                    fields,
                    connections,
                    modelIndex);
        }
    }
}
