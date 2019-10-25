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

package com.amplifyframework.api.graphql;

import com.amplifyframework.api.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * An extension of general Query class customized for GraphQL functionality.
 */
public final class GraphQLQuery extends Query {
    private final List<FieldValue> fieldValues;
    private final List<VariableValues> variableValues;
    private final List<String> fragments;

    /**
     * Constructor for GraphQLQuery with
     * specification for type of API call.
     * @param operationType type of API operation being made
     * @param document query document to process
     */
    public GraphQLQuery(OperationType operationType, String document) {
        super(operationType.getQueryPrefix(), document);
        this.fieldValues = new ArrayList<>();
        this.variableValues = new ArrayList<>();
        this.fragments = new ArrayList<>();
    }

    /**
     * Processes query parameters into a query string.
     * @return processed query string
     */
    @Override
    public String getContent() {
        final StringBuilder completeQuery = new StringBuilder();
        final StringBuilder realQuery = new StringBuilder();

        completeQuery.append("{\"query\":")
                .append("\"");

        realQuery.append(getDocument()
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
        );

        for (String fragment : fragments) {
            realQuery
                    .append("fragment ")
                    .append(fragment);
        }

        completeQuery.append(realQuery)
                .append("\"")
                .append(",")
                .append("\"variables\":");
        if (variableValues.isEmpty()) {
            completeQuery.append("null");
        } else {
            completeQuery.append("{");
            final int size = variableValues.size();
            for (int i = 0; i < size; i++) {
                final VariableValues variableValues = this.variableValues.get(i);
                completeQuery.append("\"").append(variableValues.getName()).append("\":");

                final Object value = variableValues.getValue();
                if (value == null) {
                    completeQuery.append("null");
                } else if (value instanceof Number || value instanceof Boolean) {
                    completeQuery.append(value.toString());
                } else {
                    completeQuery.append("\"").append(value.toString()).append("\"");
                }
                if (i != size - 1) {
                    completeQuery.append(",");
                }
            }
            completeQuery.append("}");
        }
        completeQuery.append("}");

        String contentString = completeQuery.toString();
        for (FieldValue fieldValue : fieldValues) {
            contentString = contentString.replace("@" + fieldValue.getName(), "\\\"" + fieldValue.getValue() + "\\\"");
        }

        while (contentString.contains("\\\\")) {
            contentString = contentString.replace("\\\\", "\\");
        }

        return contentString;
    }

    /**
     * Sets field value.
     * @param name name of field value
     * @param value field value
     * @return this query object containing new field value
     */
    public GraphQLQuery field(String name, String value) {
        fieldValues.add(new FieldValue(name, value));
        return this;
    }

    /**
     * Attaches variable key-value pair.
     * @param key variable name
     * @param value variable value
     * @return this query object for chaining
     */
    public GraphQLQuery variable(String key, Object value) {
        variableValues.add(new VariableValues(key, value));
        return this;
    }

    /**
     * Attaches fragment.
     * @param fragment fragment to be attached
     * @return this query object for chaining
     */
    public GraphQLQuery fragment(String fragment) {
        fragments.add(fragment);
        return this;
    }

    /**
     * Wrapper to contain field value and its name.
     */
    final class FieldValue {
        private final String name;
        private final String value;

        /**
         * Constructor for field key-value pair.
         * @param name name of field value
         * @param value field value
         */
        FieldValue(String name, String value) {
            this.name = name;
            this.value = value;
        }

        /**
         * Gets field name.
         * @return field name
         */
        public String getName() {
            return name;
        }

        /**
         * Gets field value.
         * @return field value
         */
        public String getValue() {
            return value;
        }
    }

    /**
     * Wrapper to contain variable value and its name.
     */
    final class VariableValues {
        private final String name;
        private final Object value;

        /**
         * Constructor for variable key-value pair.
         * @param name name of variable
         * @param value variable value
         */
        VariableValues(String name, Object value) {
            this.name = name;
            this.value = value;
        }

        /**
         * Gets variable name.
         * @return variable name.
         */
        public String getName() {
            return name;
        }

        /**
         * Gets variable value.
         * @return variable value
         */
        public Object getValue() {
            return value;
        }
    }
}
