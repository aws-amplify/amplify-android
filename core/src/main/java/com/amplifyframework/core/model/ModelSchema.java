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

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.model.annotations.BelongsTo;
import com.amplifyframework.core.model.annotations.HasMany;
import com.amplifyframework.core.model.annotations.HasOne;
import com.amplifyframework.core.model.annotations.Index;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.util.FieldFinder;
import com.amplifyframework.util.Immutable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Schema of a Model that implements the {@link Model} interface.
 * The schema encapsulates the metadata information of a Model.
 */
public final class ModelSchema {
    // Name of the Model.
    private final String name;

    // The plural version of the name of the model.
    // Useful for generating GraphQL list query names.
    private final String pluralName;

    // Denotes whether this model has owner based authorization which changes the parameters for subscriptions
    // e.g. @auth(rules: [{allow: owner}]) on the model in the GraphQL Schema
    private final List<AuthRule> authRules;

    // A map that contains the fields of a Model.
    // The key is the name of the instance variable in the Java class that represents the Model
    // The value is the ModelField object that encapsulates all the information about the instance variable.
    private final Map<String, ModelField> fields;

    // A map that contains the associations of a Model.
    // The key is the name of the instance variable in the Java class that represents one of Model's associations
    private final Map<String, ModelAssociation> associations;

    // Specifies the indexes of a Model.
    private final Map<String, ModelIndex> indexes;

    // Class of the model this schema will represent
    private final Class<? extends Model> modelClass;

    private ModelSchema(Builder builder) {
        this.name = builder.name;
        this.pluralName = builder.pluralName;
        this.authRules = builder.authRules;
        this.fields = builder.fields;
        this.associations = builder.associations;
        this.indexes = builder.indexes;
        this.modelClass = builder.modelClass;
    }

    /**
     * Return the builder object.
     * @return the builder object.
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Construct the ModelSchema from the {@link Model} class.
     *
     * @param clazz the instance of a model class
     * @return the ModelSchema object.
     * @throws AmplifyException If the conversion fails
     */
    @NonNull
    public static ModelSchema fromModelClass(@NonNull Class<? extends Model> clazz) throws AmplifyException {
        try {
            final List<Field> classFields = FieldFinder.findModelFieldsIn(clazz);
            final TreeMap<String, ModelField> fields = new TreeMap<>();
            final TreeMap<String, ModelAssociation> associations = new TreeMap<>();
            final TreeMap<String, ModelIndex> indexes = new TreeMap<>();
            final List<AuthRule> authRules = new ArrayList<>();

            // Set the model name and plural name (null if not provided)
            ModelConfig modelConfig = clazz.getAnnotation(ModelConfig.class);
            final String modelName = clazz.getSimpleName();
            final String modelPluralName = modelConfig != null && !modelConfig.pluralName().isEmpty()
                    ? modelConfig.pluralName()
                    : null;

            if (modelConfig != null) {
                for (com.amplifyframework.core.model.annotations.AuthRule ruleAnnotation : modelConfig.authRules()) {
                    authRules.add(new AuthRule(ruleAnnotation));
                }
            }

            for (Annotation annotation : clazz.getAnnotations()) {
                ModelIndex modelIndex = createModelIndex(annotation);
                if (modelIndex != null) {
                    indexes.put(modelIndex.getIndexName(), modelIndex);
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
                    .name(modelName)
                    .pluralName(modelPluralName)
                    .authRules(authRules)
                    .fields(fields)
                    .associations(associations)
                    .indexes(indexes)
                    .modelClass(clazz)
                    .build();
        } catch (Exception exception) {
            throw new AmplifyException(
                    "Error in constructing a ModelSchema.",
                    exception,
                    AmplifyException.TODO_RECOVERY_SUGGESTION
            );
        }
    }

    // Utility method to extract field metadata
    private static ModelField createModelField(Field field) {
        com.amplifyframework.core.model.annotations.ModelField annotation =
                field.getAnnotation(com.amplifyframework.core.model.annotations.ModelField.class);
        if (annotation != null) {
            final String fieldName = field.getName();
            final Class<?> fieldType = field.getType();
            final String targetType = annotation.targetType();
            final List<AuthRule> authRules = new ArrayList<>();
            for (com.amplifyframework.core.model.annotations.AuthRule ruleAnnotation : annotation.authRules()) {
                authRules.add(new AuthRule(ruleAnnotation));
            }
            return ModelField.builder()
                    .name(fieldName)
                    .javaClassForValue(fieldType)
                    .targetType(targetType.isEmpty() ? fieldType.getSimpleName() : targetType)
                    .isReadOnly(annotation.isReadOnly())
                    .isRequired(annotation.isRequired())
                    .isArray(Collection.class.isAssignableFrom(field.getType()))
                    .isEnum(Enum.class.isAssignableFrom(field.getType()))
                    .isModel(Model.class.isAssignableFrom(field.getType()))
                    .authRules(authRules)
                    .build();
        }
        return null;
    }

    // Utility method to extract association metadata from a field
    private static ModelAssociation createModelAssociation(Field field) {
        if (field.isAnnotationPresent(BelongsTo.class)) {
            BelongsTo association = Objects.requireNonNull(field.getAnnotation(BelongsTo.class));
            return ModelAssociation.builder()
                    .name(BelongsTo.class.getSimpleName())
                    .targetName(association.targetName())
                    .associatedType(association.type().getSimpleName())
                    .build();
        }
        if (field.isAnnotationPresent(HasOne.class)) {
            HasOne association = Objects.requireNonNull(field.getAnnotation(HasOne.class));
            return ModelAssociation.builder()
                    .name(HasOne.class.getSimpleName())
                    .associatedName(association.associatedWith())
                    .associatedType(association.type().getSimpleName())
                    .build();
        }
        if (field.isAnnotationPresent(HasMany.class)) {
            HasMany association = Objects.requireNonNull(field.getAnnotation(HasMany.class));
            return ModelAssociation.builder()
                    .name(HasMany.class.getSimpleName())
                    .associatedName(association.associatedWith())
                    .associatedType(association.type().getSimpleName())
                    .build();
        }
        return null;
    }

    // Utility method to extract model index metadata
    private static ModelIndex createModelIndex(Annotation annotation) {
        if (annotation.annotationType().isAssignableFrom(Index.class)) {
            Index indexAnnotation = (Index) annotation;
            return ModelIndex.builder()
                    .indexName(indexAnnotation.name())
                    .indexFieldNames(Arrays.asList(indexAnnotation.fields()))
                    .build();
        }
        return null;
    }

    /**
     * Returns the name of the Model class.
     *
     * @return the name of the Model class.
     */
    @NonNull
    public String getName() {
        return name;
    }

    /**
     * Returns the plural name of the Model in the target.
     * Null if not explicitly annotated in ModelConfig.
     *
     * @return the plural name of the Model in the target
     *         if explicitly provided.
     */
    @Nullable
    public String getPluralName() {
        return pluralName;
    }

    /**
     * Returns list of rules defining which users can access or operate against an object.
     * e.g. @auth(rules: [{allow: owner}]) on the model in the GraphQL Schema.
     *
     * @return List of {@link AuthRule}s for this Model
     */
    public List<AuthRule> getAuthRules() {
        return authRules;
    }

    /**
     * Returns the map of fieldName and the fieldObject
     * of all the fields of the model.
     *
     * @return map of fieldName and the fieldObject
     *         of all the fields of the model.
     */
    @NonNull
    public Map<String, ModelField> getFields() {
        return fields;
    }

    /**
     * Returns a map of field to associations of the model.
     *
     * @return a map of field to associations of the model.
     */
    @NonNull
    public Map<String, ModelAssociation> getAssociations() {
        return Immutable.of(associations);
    }

    /**
     * Returns the map of indexes of a {@link Model}.
     *
     * @return the map of indexes of a {@link Model}.
     */
    @NonNull
    public Map<String, ModelIndex> getIndexes() {
        return indexes;
    }

    /**
     * Returns the class of {@link Model}.
     *
     * @return the class of {@link Model}.
     */
    @NonNull
    public Class<? extends Model> getModelClass() {
        return modelClass;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            ModelSchema that = (ModelSchema) obj;
            return ObjectsCompat.equals(getName(), that.getName()) &&
                ObjectsCompat.equals(getPluralName(), that.getPluralName()) &&
                ObjectsCompat.equals(getAuthRules(), that.getAuthRules()) &&
                ObjectsCompat.equals(getFields(), that.getFields()) &&
                ObjectsCompat.equals(getAssociations(), that.getAssociations()) &&
                ObjectsCompat.equals(getIndexes(), that.getIndexes()) &&
                ObjectsCompat.equals(getModelClass(), that.getModelClass());
        }
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(
                getName(),
                getPluralName(),
                getAuthRules(),
                getFields(),
                getAssociations(),
                getIndexes(),
                getModelClass()
        );
    }

    @Override
    public String toString() {
        return "ModelSchema{" +
            "name='" + name + '\'' +
            ", pluralName='" + pluralName + '\'' +
            ", authRules=" + authRules +
            ", fields=" + fields +
            ", associations=" + associations +
            ", indexes=" + indexes +
            ", modelClass=" + modelClass +
            '}';
    }

    /**
     * The Builder to build the {@link ModelSchema} object.
     */
    @SuppressWarnings("WeakerAccess")
    public static final class Builder {
        private final Map<String, ModelField> fields;
        private final Map<String, ModelAssociation> associations;
        private final Map<String, ModelIndex> indexes;
        private Class<? extends Model> modelClass;
        private String name;
        private String pluralName;
        private final List<AuthRule> authRules;

        Builder() {
            this.authRules = new ArrayList<>();
            this.fields = new TreeMap<>();
            this.associations = new TreeMap<>();
            this.indexes = new TreeMap<>();
        }

        /**
         * Set the name of the Model class.
         * @param name the name of the Model class.
         * @return the builder object
         */
        @NonNull
        public Builder name(@NonNull String name) {
            this.name = Objects.requireNonNull(name);
            return this;
        }

        /**
         * The plural version of the name of the Model.
         * If null, a default plural version name will be generated.
         * @param pluralName the plural version of model name.
         * @return the builder object
         */
        @NonNull
        public Builder pluralName(@Nullable String pluralName) {
            this.pluralName = pluralName;
            return this;
        }

        /**
         * Denotes authorization rules for this model defined by the @auth directive.
         * e.g. @auth(rules: [{allow: owner}]) on the model in the GraphQL Schema
         *
         * @param authRules list of {@link AuthRule}s for this {@link Model}
         * @return the builder object
         */
        @NonNull
        public Builder authRules(@NonNull List<AuthRule> authRules) {
            Objects.requireNonNull(authRules);
            this.authRules.clear();
            this.authRules.addAll(authRules);
            return this;
        }

        /**
         * Set the map of fieldName and the fieldObject of all the fields of the model.
         * @param fields the map of fieldName and the fieldObject of all the fields of the model.
         * @return the builder object.
         */
        @NonNull
        public Builder fields(@NonNull Map<String, ModelField> fields) {
            Objects.requireNonNull(fields);
            this.fields.clear();
            this.fields.putAll(fields);
            return this;
        }

        /**
         * Set the map of fieldName and the association of all the associated fields of the model.
         * @param associations the map of fieldName and the association metadata.
         * @return the builder object.
         */
        @NonNull
        public Builder associations(@NonNull Map<String, ModelAssociation> associations) {
            Objects.requireNonNull(associations);
            this.associations.clear();
            this.associations.putAll(associations);
            return this;
        }

        /**
         * Set the map of indexes of a model.
         * @param indexes the indexes of the model.
         * @return the builder object.
         */
        @NonNull
        public Builder indexes(@NonNull Map<String, ModelIndex> indexes) {
            Objects.requireNonNull(indexes);
            this.indexes.clear();
            this.indexes.putAll(indexes);
            return this;
        }

        /**
         * The class of the Model this schema represents.
         * @param modelClass the class of the model.
         * @return the builder object
         */
        @NonNull
        public Builder modelClass(@NonNull Class<? extends Model> modelClass) {
            this.modelClass = modelClass;
            return this;
        }

        /**
         * Return the ModelSchema object.
         * @return the ModelSchema object.
         */
        @SuppressLint("SyntheticAccessor")
        @NonNull
        public ModelSchema build() {
            Objects.requireNonNull(name);
            return new ModelSchema(Builder.this);
        }
    }
}
