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

package com.amplifyframework.api.aws;

import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.graphql.Operation;
import com.amplifyframework.api.graphql.QueryType;
import com.amplifyframework.core.model.AuthRule;
import com.amplifyframework.core.model.AuthStrategy;
import com.amplifyframework.core.model.CustomTypeField;
import com.amplifyframework.core.model.CustomTypeSchema;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelAssociation;
import com.amplifyframework.core.model.ModelField;
import com.amplifyframework.core.model.ModelList;
import com.amplifyframework.core.model.ModelReference;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.PropertyContainerPath;
import com.amplifyframework.core.model.SchemaRegistry;
import com.amplifyframework.core.model.SerializedModel;
import com.amplifyframework.core.model.types.JavaFieldType;
import com.amplifyframework.util.Empty;
import com.amplifyframework.util.FieldFinder;
import com.amplifyframework.util.Wrap;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Class representing a node of a SelectionSet for use in a GraphQLDocument.
 * A root SelectionSet node will have a null value.
 */
public final class SelectionSet {
    private static final String INDENT = "  ";

    private final String value;
    private final Set<SelectionSet> nodes;

    /**
     * Copy constructor.
     * @param selectionSet node to copy
     */
    @SuppressWarnings("CopyConstructorMissesField") // It is cloned, by recursion
    public SelectionSet(SelectionSet selectionSet) {
        this(selectionSet.value, new HashSet<>(selectionSet.nodes));
    }

    /**
     * Constructor for a leaf node (no children).
     * @param value String value of the field.
     */
    public SelectionSet(String value) {
        this(value, Collections.emptySet());
    }

    /**
     * Default constructor.
     * @param value String value of the field
     * @param nodes Set of child nodes
     */
    public SelectionSet(String value, @NonNull Set<SelectionSet> nodes) {
        this.value = value;
        this.nodes = Objects.requireNonNull(nodes);
    }

    /**
     * Returns node value.
     * @return node value
     */
    @Nullable
    protected String getValue() {
        return value;
    }

    /**
     * Returns child nodes.
     * @return child nodes
     */
    @NonNull
    public Set<SelectionSet> getNodes() {
        return nodes;
    }

    /**
     * Generate the String value of the SelectionSet used in the GraphQL query document, with no margin.
     *
     * Sample return value:
     *   items {
     *     foo
     *     bar
     *     modelName {
     *       foo
     *       bar
     *     }
     *   }
     *   nextToken
     *
     * @return String value of the selection set for a GraphQL query document.
     */
    @Override
    public String toString() {
        return toString("");
    }

    /**
     * Generates the String value of the SelectionSet for a GraphQL query document.
     * @param margin a margin with which to prefix each field of the selection set.
     * @return String value of the SelectionSet for a GraphQL query document.
     */
    public String toString(String margin) {
        List<String> fieldsList = new ArrayList<>();
        StringBuilder builder = new StringBuilder();

        if (value != null) {
            builder.append(value);
        }

        if (!Empty.check(nodes)) {
            for (SelectionSet node : nodes) {
                fieldsList.add(node.toString(margin + INDENT));
            }
            Collections.sort(fieldsList);
            String delimiter = "\n" + margin + INDENT;
            builder.append(Wrap.inPrettyBraces(TextUtils.join(delimiter, fieldsList), margin, INDENT));
        }

        return builder.toString();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        SelectionSet selectionSet = (SelectionSet) object;

        return ObjectsCompat.equals(value, selectionSet.value);
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(value);
    }

    /**
     * Create a new SelectionSet builder.
     * @return a new SelectionSet builder.
     */
    public static SelectionSet.Builder builder() {
        return new Builder();
    }

    /**
     * Factory class for creating and serializing a selection set within a GraphQL document.
     */
    static final class Builder {
        private String value;
        private Class<? extends Model> modelClass;
        private Operation operation;
        private GraphQLRequestOptions requestOptions;
        private ModelSchema modelSchema;
        private List<PropertyContainerPath> includeRelationships;

        Builder() { }

        Builder value(@Nullable String value) {
            this.value = value;
            return Builder.this;
        }

        public Builder modelClass(@NonNull Class<? extends Model> modelClass) {
            this.modelClass = Objects.requireNonNull(modelClass);
            return Builder.this;
        }

        public Builder modelSchema(@NonNull ModelSchema modelSchema) {
            this.modelSchema = Objects.requireNonNull(modelSchema);
            return Builder.this;
        }

        public Builder operation(@NonNull Operation operation) {
            this.operation = Objects.requireNonNull(operation);
            return Builder.this;
        }

        public Builder requestOptions(@NonNull GraphQLRequestOptions requestOptions) {
            this.requestOptions = Objects.requireNonNull(requestOptions);
            return Builder.this;
        }

        public Builder includeRelationships(@NonNull List<PropertyContainerPath> relationships) {
            this.includeRelationships = relationships;
            return Builder.this;
        }

        /**
         * Builds the SelectionSet containing all of the fields of the provided model class.
         * @return selection set
         * @throws AmplifyException if a ModelSchema cannot be created from the provided model class.
         */
        public SelectionSet build() throws AmplifyException {
            if (this.modelClass == null && this.modelSchema == null) {
                throw new AmplifyException("Both modelClass and modelSchema cannot be null",
                        "Provide either a modelClass or a modelSchema to build the selection set");
            }
            Objects.requireNonNull(this.operation);
            SelectionSet node = new SelectionSet(value,
                    SerializedModel.class == modelClass
                            ? getModelFields(modelSchema, requestOptions.maxDepth(), operation)
                            : getModelFields(modelClass, requestOptions.maxDepth(), operation, false));

            // Relationships need to be added before wrapping pagination
            if (includeRelationships != null) {
                for (PropertyContainerPath association : includeRelationships) {
                    SelectionSet included = SelectionSetUtils.asSelectionSetWithoutRoot(association);
                    if (included != null) {
                        SelectionSetUtils.mergeChild(node, included);
                    }
                }
            }

            if (QueryType.LIST.equals(operation) || QueryType.SYNC.equals(operation)) {
                node = wrapPagination(node);
            }
            return node;
        }

        /**
         * Expects a {@link SelectionSet} containing {@link Model} fields as nodes, and returns a new root node with two
         * children:
         *  - "items" with nodes being the children of the provided node.
         *  - "nextToken"
         *
         * @param node a root node, with a value of null, and pagination fields
         * @return A selection set
         */
        private SelectionSet wrapPagination(SelectionSet node) {
            return new SelectionSet(null, wrapPagination(node.getNodes()));
        }

        private Set<SelectionSet> wrapPagination(Set<SelectionSet> nodes) {
            Set<SelectionSet> paginatedSet = new HashSet<>();
            paginatedSet.add(new SelectionSet(requestOptions.listField(), nodes));
            for (String metaField : requestOptions.paginationFields()) {
                paginatedSet.add(new SelectionSet(metaField));
            }
            return paginatedSet;
        }

        /**
         * Gets a selection set for the given class.
         * TODO: this is mostly duplicative of {@link #getModelFields(ModelSchema, int, Operation)}.
         * Long-term, we want to remove this current method and rely only on the ModelSchema-based
         * version.
         * 
         * @param clazz          Class from which to build selection set
         * @param depth          Number of children deep to explore
         * @param primaryKeyOnly if keys should only be included
         * @return SelectionSet for given class
         * @throws AmplifyException On failure to build selection set
         */
        @SuppressWarnings("unchecked") // Cast to Class<Model>
        private Set<SelectionSet> getModelFields(
                Class<? extends Model> clazz,
                int depth,
                Operation operation,
                Boolean primaryKeyOnly
        )
                throws AmplifyException {
            if (depth < 0) {
                return new HashSet<>();
            }

            Set<SelectionSet> result = new HashSet<>();
            ModelSchema schema = ModelSchema.fromModelClass(clazz);

            if (
                    (depth == 0
                    && (LeafSerializationBehavior.JUST_ID.equals(
                            requestOptions.leafSerializationBehavior()
                    ) || primaryKeyOnly)
                    && operation != QueryType.SYNC)
            ) {
                for (String s : schema.getPrimaryIndexFields()) {
                    result.add(new SelectionSet(s));
                }
                return result;
            }

            for (Field field : FieldFinder.findModelFieldsIn(clazz)) {
                String fieldName = field.getName();
                if (schema.getAssociations().containsKey(fieldName)) {
                    if (ModelList.class.isAssignableFrom(field.getType())) {
                        // Default behavior is to not include ModeList to allow for lazy loading
                        // We do not need to inject any keys since ModelList values are pulled
                        // from parent information.
                        continue;
                    } else if (List.class.isAssignableFrom(field.getType())) {
                        if (depth >= 1) {
                            ParameterizedType listType = (ParameterizedType) field.getGenericType();
                            Class<Model> listTypeClass = (Class<Model>) listType.getActualTypeArguments()[0];
                            Set<SelectionSet> fields = wrapPagination(
                                    getModelFields(
                                            listTypeClass,
                                            depth - 1,
                                            operation,
                                            false
                                    )
                            );
                            result.add(new SelectionSet(fieldName, fields));
                        }
                    } else if (ModelReference.class.isAssignableFrom(field.getType())) {
                        ParameterizedType pType = (ParameterizedType) field.getGenericType();
                        Class<Model> modalClass = (Class<Model>) pType.getActualTypeArguments()[0];
                        Set<SelectionSet> fields = getModelFields(modalClass, 0, operation, true);
                        result.add(new SelectionSet(fieldName, fields));
                    } else if (depth >= 1) {
                        Class<Model> modalClass = (Class<Model>) field.getType();
                        Set<SelectionSet> fields = getModelFields(modalClass, depth - 1, operation, false);
                        result.add(new SelectionSet(fieldName, fields));
                    }
                } else if (isCustomType(field)) {
                    result.add(new SelectionSet(fieldName, getNestedCustomTypeFields(getClassForField(field))));
                } else {
                    result.add(new SelectionSet(fieldName));
                }
                for (AuthRule authRule : schema.getAuthRules()) {
                    if (AuthStrategy.OWNER.equals(authRule.getAuthStrategy())) {
                        result.add(new SelectionSet(authRule.getOwnerFieldOrDefault()));
                        break;
                    }
                }
            }
            for (String fieldName : requestOptions.modelMetaFields()) {
                result.add(new SelectionSet(fieldName));
            }
            return result;
        }

        /**
         * We handle customType fields differently as DEPTH does not apply here.
         * @param clazz class we wish to build selection set for
         * @return A set of selection sets
         */
        private Set<SelectionSet> getNestedCustomTypeFields(Class<?> clazz) {
            Set<SelectionSet> result = new HashSet<>();
            for (Field field : FieldFinder.findNonTransientFieldsIn(clazz)) {
                String fieldName = field.getName();
                if (isCustomType(field)) {
                    result.add(new SelectionSet(fieldName, getNestedCustomTypeFields(getClassForField(field))));
                } else {
                    result.add(new SelectionSet(fieldName));
                }
            }
            return result;
        }

        /**
         * Helper to determine if field is a custom type. If custom types we need to build nested selection set.
         * @param field field we wish to check
         * @return True if the field is of a custom type
         */
        private static boolean isCustomType(@NonNull Field field) {
            Class<?> cls = getClassForField(field);
            if (Model.class.isAssignableFrom(cls) || Enum.class.isAssignableFrom(cls)) {
                return false;
            }
            try {
                JavaFieldType.from(cls);
                return false;
            } catch (IllegalArgumentException exception) {
                // if we get here then field is  a custom type
                return true;
            }
        }

        /**
         * Get the class of a field. If field is a collection, it returns the Generic type
         * @return The class of the field
         */
        static Class<?> getClassForField(Field field) {
            Class<?> typeClass;
            if (Collection.class.isAssignableFrom(field.getType())) {
                ParameterizedType listType = (ParameterizedType) field.getGenericType();
                typeClass = (Class<?>) listType.getActualTypeArguments()[0];
            } else {
                typeClass = field.getType();
            }
            return typeClass;
        }

        // TODO: this method is tech debt. We added it to support usage of the library from Flutter.
        // This version of the method needs to be unified with getModelFields(Class<? extends Model> clazz, int depth).
        private Set<SelectionSet> getModelFields(ModelSchema modelSchema, int depth, Operation operation) {
            if (depth < 0) {
                return new HashSet<>();
            }
            Set<SelectionSet> result = new HashSet<>();
            if (
                    depth == 0
                    && LeafSerializationBehavior.JUST_ID.equals(requestOptions.leafSerializationBehavior())
                    && operation != QueryType.SYNC
            ) {
                Iterator<String> primaryKeyIterator = modelSchema.getPrimaryIndexFields().listIterator();
                if (primaryKeyIterator.hasNext()) {
                    result.add(new SelectionSet(primaryKeyIterator.next()));
                }
            }

            SchemaRegistry modelSchemas = SchemaRegistry.instance();

            for (Map.Entry<String, ModelField> entry : modelSchema.getFields().entrySet()) {
                String fieldName = entry.getKey();
                ModelAssociation association = modelSchema.getAssociations().get(fieldName);
                if (association != null) {
                    if (depth >= 1) {
                        String associatedModelName = association.getAssociatedType();
                        ModelSchema associateModelSchema =
                                modelSchemas.getModelSchemaForModelClass(associatedModelName);
                        Set<SelectionSet> fields;
                        if (entry.getValue().isArray()) { // If modelField is an Array
                            fields = wrapPagination(getModelFields(associateModelSchema, depth - 1, operation));
                        } else {
                            fields = getModelFields(associateModelSchema, depth - 1, operation);
                        }
                        result.add(new SelectionSet(fieldName, fields));
                    }
                } else if (entry.getValue().isCustomType()) {
                    CustomTypeSchema fieldCustomTypeSchema =
                            modelSchemas.getCustomTypeSchemaForCustomTypeClass(entry.getValue().getTargetType());
                    Set<SelectionSet> fields = getCustomTypeFields(fieldCustomTypeSchema);
                    result.add(new SelectionSet(fieldName, fields));
                } else {
                    result.add(new SelectionSet(fieldName));
                }
                for (AuthRule authRule : modelSchema.getAuthRules()) {
                    if (AuthStrategy.OWNER.equals(authRule.getAuthStrategy())) {
                        result.add(new SelectionSet(authRule.getOwnerFieldOrDefault()));
                        break;
                    }
                }
            }
            for (String fieldName : requestOptions.modelMetaFields()) {
                result.add(new SelectionSet(fieldName));
            }
            return result;
        }

        private Set<SelectionSet> getCustomTypeFields(@NonNull CustomTypeSchema customTypeSchema) {
            SchemaRegistry schemaRegistry = SchemaRegistry.instance();
            Set<SelectionSet> result = new HashSet<>();

            for (Map.Entry<String, CustomTypeField> entry : customTypeSchema.getFields().entrySet()) {
                String fieldName = entry.getKey();
                if (entry.getValue().isCustomType()) {
                    CustomTypeSchema fieldCustomTypeSchema =
                            schemaRegistry.getCustomTypeSchemaForCustomTypeClass(entry.getValue().getTargetType());
                    Set<SelectionSet> fields = getCustomTypeFields(fieldCustomTypeSchema);
                    result.add(new SelectionSet(fieldName, fields));
                } else {
                    result.add(new SelectionSet(fieldName));
                }
            }

            return result;
        }
    }
}
