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
import com.amplifyframework.core.model.AuthRule;
import com.amplifyframework.core.model.AuthStrategy;
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
 * Class for creating and serializing a selection set within a GraphQL document.
 */
final class SelectionSet {
    private static final String ITEMS_KEY = "items";
    private static final String NEXT_TOKEN_KEY = "nextToken";

    // This class should not be instantiated
    private SelectionSet() {}

    /**
     * Returns selection set containing the fields of the provided model class, as well as nested models.
     * @param modelClass model class
     * @return selection set containing all of the fields of the provided model class
     * @throws AmplifyException if a ModelSchema cannot be created from the provided model class.
     */
    public static Node fromModelClass(Class<? extends Model> modelClass, OperationType operationType, int depth)
            throws AmplifyException {
        Node node = new Node(null, getModelFields(modelClass, depth));
        if (QueryType.LIST.equals(operationType)) {
            node = wrapPagination(node);
        }
        return node;
    }

    /**
     * Expects a {@link Node} containing {@link Model} fields as nodes, and returns a new root node with two children:
     *  - "items" with nodes being the children of the provided node.
     *  - "nextToken"
     *
     * @param node a root node, with a value of null, and pagination fields
     * @return
     */
    private static Node wrapPagination(Node node) {
        return new Node(null, wrapPagination(node.nodes));
    }

    private static Set<Node> wrapPagination(Set<Node> nodes) {
        Set<Node> paginatedSet = new HashSet<>();
        paginatedSet.add(new Node(ITEMS_KEY, nodes));
        paginatedSet.add(new Node(NEXT_TOKEN_KEY, null));
        return paginatedSet;
    }

    @SuppressWarnings("unchecked") // Cast to Class<Model>
    private static Set<Node> getModelFields(Class<? extends Model> clazz, int depth)
            throws AmplifyException {
        if (depth < 0) {
            return new HashSet<>();
        }

        Set<Node> result = new HashSet<>();
        ModelSchema schema = ModelSchema.fromModelClass(clazz);
        for (Field field : FieldFinder.findFieldsIn(clazz)) {
            String fieldName = field.getName();
            if (schema.getAssociations().containsKey(fieldName)) {
                if (List.class.isAssignableFrom(field.getType())) {
                    if (depth >= 1) {
                        ParameterizedType listType = (ParameterizedType) field.getGenericType();
                        Class<Model> listTypeClass = (Class<Model>) listType.getActualTypeArguments()[0];
                        Set<Node> fields = wrapPagination(getModelFields(listTypeClass, depth - 1));
                        result.add(new Node(fieldName, fields));
                    }
                } else if (depth >= 1) {
                    Set<Node> fields = getModelFields((Class<Model>) field.getType(), depth - 1);
                    result.add(new Node(fieldName, fields));
                }
            } else {
                result.add(new Node(fieldName, null));
            }
            for (AuthRule authRule : schema.getAuthRules()) {
                if (AuthStrategy.OWNER.equals(authRule.getAuthStrategy())) {
                    result.add(new Node(authRule.getOwnerFieldOrDefault(), null));
                    break;
                }
            }
        }
        return result;
    }

    static final class Node {
        private final String value;
        private final Set<Node> nodes;

        /**
         * Copy constructor.
         * @param node node to copy
         */
        Node(Node node) {
            this(node.value, new HashSet<>(node.nodes));
        }

        private Node(String value, Set<Node> nodes) {
            this.value = value;
            this.nodes = nodes;
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
                for (Node field : nodes) {
                    fieldsList.add(field.toString());
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
            Node node = (Node) object;

            return ObjectsCompat.equals(value, node.value);
        }

        @Override
        public int hashCode() {
            return ObjectsCompat.hash(value);
        }
    }
}
