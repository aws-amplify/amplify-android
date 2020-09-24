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
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.graphql.Operation;
import com.amplifyframework.api.graphql.QueryType;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelSchema;
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
import java.util.List;
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
    public SelectionSet(SelectionSet selectionSet) {
        this(selectionSet.value, new HashSet<>(selectionSet.nodes));
    }

    /**
     * Default constructor.
     * @param value String value of the field
     * @param nodes Set of child nodes
     */
    public SelectionSet(String value, Set<SelectionSet> nodes) {
        this.value = value;
        this.nodes = nodes;
    }

    /**
     * Returns child nodes.
     * @return child nodes
     */
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
        private Class<? extends Model> modelClass;
        private Operation operation;
        private GraphQLRequestOptions requestOptions;

        Builder() { }

        public Builder modelClass(@NonNull Class<? extends Model> modelClass) {
            this.modelClass = Objects.requireNonNull(modelClass);
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

        /**
         * Builds the SelectionSet containing all of the fields of the provided model class.
         * @return selection set
         * @throws AmplifyException if a ModelSchema cannot be created from the provided model class.
         */
        public SelectionSet build() throws AmplifyException {
            Objects.requireNonNull(this.modelClass);
            Objects.requireNonNull(this.operation);
            SelectionSet node = new SelectionSet(null, getModelFields(modelClass, requestOptions.maxDepth()));
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
         * @return
         */
        private SelectionSet wrapPagination(SelectionSet node) {
            return new SelectionSet(null, wrapPagination(node.getNodes()));
        }

        private Set<SelectionSet> wrapPagination(Set<SelectionSet> nodes) {
            Set<SelectionSet> paginatedSet = new HashSet<>();
            paginatedSet.add(new SelectionSet(requestOptions.listField(), nodes));
            for (String metaField : requestOptions.paginationFields()) {
                paginatedSet.add(new SelectionSet(metaField, null));
            }
            return paginatedSet;
        }

        @SuppressWarnings("unchecked") // Cast to Class<Model>
        private Set<SelectionSet> getModelFields(Class<? extends Model> clazz, int depth)
                throws AmplifyException {
            if (depth < 0) {
                return new HashSet<>();
            }

            Set<SelectionSet> result = new HashSet<>();

            if (depth == 0 && LeafSerializationBehavior.JUST_ID.equals(requestOptions.leafSerializationBehavior())) {
                result.add(new SelectionSet("id", null));
                return result;
            }

            ModelSchema schema = ModelSchema.fromModelClass(clazz);
            for (Field field : FieldFinder.findModelFieldsIn(clazz)) {
                String fieldName = field.getName();
                if (schema.getAssociations().containsKey(fieldName)) {
                    if (List.class.isAssignableFrom(field.getType())) {
                        if (depth >= 1) {
                            ParameterizedType listType = (ParameterizedType) field.getGenericType();
                            Class<Model> listTypeClass = (Class<Model>) listType.getActualTypeArguments()[0];
                            Set<SelectionSet> fields = wrapPagination(getModelFields(listTypeClass, depth - 1));
                            result.add(new SelectionSet(fieldName, fields));
                        }
                    } else if (depth >= 1) {
                        Set<SelectionSet> fields = getModelFields((Class<Model>) field.getType(), depth - 1);
                        result.add(new SelectionSet(fieldName, fields));
                    }
                } else if (isCustomType(field)) {
                    result.add(new SelectionSet(fieldName, getNestedCustomTypeFields(getClassForField(field))));
                } else {
                    result.add(new SelectionSet(fieldName, null));
                }
            }
            for (String fieldName : requestOptions.modelMetaFields()) {
                result.add(new SelectionSet(fieldName, null));
            }
            return result;
        }

        /**
         * We handle customType fields differently as DEPTH does not apply here.
         * @param clazz class we wish to build selection set for
         * @return
         */
        private Set<SelectionSet> getNestedCustomTypeFields(Class<?> clazz) {
            Set<SelectionSet> result = new HashSet<>();
            for (Field field : FieldFinder.findNonTransientFieldsIn(clazz)) {
                String fieldName = field.getName();
                if (isCustomType(field)) {
                    result.add(new SelectionSet(fieldName, getNestedCustomTypeFields(getClassForField(field))));
                } else {
                    result.add(new SelectionSet(fieldName, null));
                }
            }
            return result;
        }

        /**
         * Helper to determine if field is a custom type. If custom types we need to build nested selection set.
         * @param field field we wish to check
         * @return
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
         * @return
         */
        static Class<?> getClassForField(Field field) {
            Class<?> typeClass;
            if (Collection.class.isAssignableFrom(field.getType())) {
                ParameterizedType listType = (ParameterizedType) field.getGenericType();
                typeClass = (Class) listType.getActualTypeArguments()[0];
            } else {
                typeClass = field.getType();
            }
            return typeClass;
        }
    }
}
