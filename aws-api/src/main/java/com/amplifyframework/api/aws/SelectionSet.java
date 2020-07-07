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
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.graphql.OperationType;
import com.amplifyframework.api.graphql.QueryType;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.util.FieldFinder;
import com.amplifyframework.util.Wrap;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Class representing a node of a SelectionSet for use in a GraphQLDocument.
 * A root SelectionSet node will have a null value.
 */
public final class SelectionSet {
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
     * Generate the String value of the SelectionSet used in the GraphQL query document.
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
        List<String> fieldsList = new ArrayList<>();
        StringBuilder builder = new StringBuilder();

        if (value != null) {
            builder.append(value);
        }

        if (nodes != null && nodes.size() > 0) {
            for (SelectionSet node : nodes) {
                fieldsList.add(node.toString());
            }
            Collections.sort(fieldsList);
            builder.append(" " + Wrap.inBraces(TextUtils.join(" ", fieldsList)));
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
     * Factory class for creating and serializing a selection set within a GraphQL document.
     */
    static final class Factory {
        private static final String ITEMS_KEY = "items";
        private static final String NEXT_TOKEN_KEY = "nextToken";

        // This class should not be instantiated
        private Factory() {}

        /**
         * Returns selection set containing the fields of the provided model class, as well as nested models.
         * @param clazz model class
         * @return selection set containing all of the fields of the provided model class
         * @throws AmplifyException if a ModelSchema cannot be created from the provided model class.
         */
        public static SelectionSet fromModelClass(Class<? extends Model> clazz, OperationType operationType, int depth)
                throws AmplifyException {
            SelectionSet node = new SelectionSet(null, getModelFields(clazz, depth));
            if (QueryType.LIST.equals(operationType)) {
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
        private static SelectionSet wrapPagination(SelectionSet node) {
            return new SelectionSet(null, wrapPagination(node.getNodes()));
        }

        private static Set<SelectionSet> wrapPagination(Set<SelectionSet> nodes) {
            Set<SelectionSet> paginatedSet = new HashSet<>();
            paginatedSet.add(new SelectionSet(ITEMS_KEY, nodes));
            paginatedSet.add(new SelectionSet(NEXT_TOKEN_KEY, null));
            return paginatedSet;
        }

        @SuppressWarnings("unchecked") // Cast to Class<Model>
        private static Set<SelectionSet> getModelFields(Class<? extends Model> clazz, int depth)
                throws AmplifyException {
            if (depth < 0) {
                return new HashSet<>();
            }

            Set<SelectionSet> result = new HashSet<>();
            ModelSchema schema = ModelSchema.fromModelClass(clazz);
            for (Field field : FieldFinder.findFieldsIn(clazz)) {
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
                } else {
                    result.add(new SelectionSet(fieldName, null));
                }
            }
            return result;
        }
    }
}
