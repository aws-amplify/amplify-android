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
import com.amplifyframework.core.model.annotations.HasMany;
import com.amplifyframework.core.model.annotations.HasOne;
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
import java.util.Objects;
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

    // A map that contains the associations of a Model.
    // The key is the name of the instance variable in the Java class that represents one of Model's associations
    private final Map<String, ModelAssociation> associations;

    // Specifies the index of a Model.
    private final ModelIndex modelIndex;

    // Maintain a sorted copy of all the fields of a Model
    // This is useful so code that uses the sortedFields to generate queries and other
    // persistence-related operations guarantee that the results are always consistent.
    private final List<ModelField> sortedFields;

    private ModelSchema(String name,
                        String targetModelName,
                        Map<String, ModelField> fields,
                        Map<String, ModelAssociation> associations,
                        ModelIndex modelIndex) {
        this.name = name;
        this.targetModelName = targetModelName;
        this.fields = fields;
        this.associations = associations;
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
            final TreeMap<String, ModelAssociation> associations = new TreeMap<>();
            final ModelIndex modelIndex = getModelIndex(clazz);
            String targetModelName = null;
            if (clazz.isAnnotationPresent(ModelConfig.class)) {
                targetModelName = clazz.getAnnotation(ModelConfig.class).targetName();
                if (targetModelName.isEmpty()) {
                    targetModelName = clazz.getSimpleName();
                }
            }

            for (Field field : classFields) {
                final ModelField modelField = createModelField(field);
                if (modelField != null) {
                    fields.put(field.getName(), modelField);
                }
                final ModelAssociation modelAssociation = createModelAssociation(field);
                if (modelAssociation != null) {
                    associations.put(field.getName(), modelAssociation);
                }
            }

            return ModelSchema.builder()
                    .name(clazz.getSimpleName())
                    .targetModelName(targetModelName)
                    .fields(fields)
                    .associations(associations)
                    .modelIndex(modelIndex)
                    .build();
        } catch (Exception exception) {
            throw new ModelSchemaException("Error in constructing a ModelSchema.", exception);
        }
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
                    .build();
        }
        return null;
    }

    // Utility method to extract association metadata from a field
    private static ModelAssociation createModelAssociation(Field field) {
        if (field.isAnnotationPresent(BelongsTo.class)) {
            BelongsTo association = field.getAnnotation(BelongsTo.class);
            return ModelAssociation.builder()
                    .associatedName(association.targetName())
                    .associatedType(association.type().getSimpleName())
                    .isOwner(true)
                    .build();
        }
        if (field.isAnnotationPresent(HasOne.class)) {
            HasOne association = field.getAnnotation(HasOne.class);
            return ModelAssociation.builder()
                    .associatedName(association.associatedWith())
                    .associatedType(association.type().getSimpleName())
                    .build();
        }
        if (field.isAnnotationPresent(HasMany.class)) {
            HasMany association = field.getAnnotation(HasMany.class);
            return ModelAssociation.builder()
                    .associatedName(association.associatedWith())
                    .associatedType(association.type().getSimpleName())
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
        return Immutable.of(sortedFields);
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
     * Returns a map of field to associations of the model.
     *
     * @return a map of field to associations of the model.
     */
    public Map<String, ModelAssociation> getAssociations() {
        return Immutable.of(associations);
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

        for (ModelField modelField : this.fields.values()) {
            try {
                Field privateField = instance.getClass().getDeclaredField(modelField.getName());
                privateField.setAccessible(true);

                if (getAssociations().containsKey(modelField.getName())) {
                    ModelAssociation association = getAssociations().get(modelField.getName());
                    // All ModelAssociation targets are required to be instances of Model so this is a safe cast
                    Model target = (Model) privateField.get(instance);
                    result.put(association.getAssociatedName(), target.getId());
                } else {
                    result.put(modelField.getTargetName(), privateField.get(instance));
                }
            } catch (Exception exception) {
                throw new AmplifyException("An invalid field was provided - " +
                        modelField.getName() +
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
        // 1. ID comes always first
        // 2. The other sortedFields are sorted alphabetically
        //
        // This is useful so code that uses the sortedFields to generate queries and other
        // persistence-related operations guarantee that the results are always consistent.
        Collections.sort(modelFieldEntries, (fieldOne, fieldOther) -> {

            if (fieldOne.isId()) {
                return -1;
            }
            if (fieldOther.isId()) {
                return 1;
            }
            if (associations.containsKey(fieldOne.getName()) && !associations.containsKey(fieldOther.getName())) {
                return 1;
            }
            if (associations.containsKey(fieldOther.getName()) && !associations.containsKey(fieldOne.getName())) {
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
        if (!ObjectsCompat.equals(associations, that.associations)) {
            return false;
        }
        if (!ObjectsCompat.equals(modelIndex, that.modelIndex)) {
            return false;
        }
        return true;
    }

    @SuppressWarnings("checkstyle:MagicNumber") // 31 is auto-generated by IDE
    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (targetModelName != null ? targetModelName.hashCode() : 0);
        result = 31 * result + (fields != null ? fields.hashCode() : 0);
        result = 31 * result + (associations != null ? associations.hashCode() : 0);
        result = 31 * result + (modelIndex != null ? modelIndex.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ModelSchema{" +
            "name='" + name + '\'' +
            ", targetModelName='" + targetModelName + '\'' +
            ", fields=" + fields +
            ", associations" + associations +
            ", modelIndex=" + modelIndex +
            '}';
    }

    /**
     * The Builder to build the {@link ModelSchema} object.
     */
    public static final class Builder {
        private String name;
        private String targetModelName;
        private Map<String, ModelField> fields = new TreeMap<>();
        private Map<String, ModelAssociation> associations = new TreeMap<>();
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
        public Builder fields(@NonNull Map<String, ModelField> fields) {
            Objects.requireNonNull(fields);
            this.fields = fields;
            return this;
        }

        /**
         * Set the map of fieldName and the association of all the associated fields of the model.
         * @param associations the map of fieldName and the association metadata.
         * @return the builder object.
         */
        public Builder associations(@NonNull Map<String, ModelAssociation> associations) {
            Objects.requireNonNull(associations);
            this.associations = associations;
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
                    associations,
                    modelIndex);
        }
    }
}
