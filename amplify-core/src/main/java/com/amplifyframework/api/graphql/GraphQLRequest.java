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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A request against a GraphQL endpoint.
 * This is essentially just a wrapper around a string,
 * plus a fancy builder. In order to have obtained this string-wrapper,
 * you would have needed to go through the {@link GraphQLRequest.Builder}.
 */
public final class GraphQLRequest {
    private final String content;

    /**
     * Constructs a new GraphQLRequest.
     * @param content The string content of the request body that will be
     *                passed to a GraphQL endpoint
     */
    public GraphQLRequest(String content) {
        this.content = Objects.requireNonNull(content);
    }

    /**
     * Gets the content of the request.
     * @return Request content
     */
    public String content() {
        return content;
    }

    /**
     * Gets a GraphQLRequest builder, that can be used to formulate a new request instance.
     * @return GraphQLRequest builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Wrapper to contain field value and its name.
     */
    static final class FieldValue {
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
    static final class VariableValues {
        private final String name;
        private final String value;

        /**
         * Constructor for variable key-value pair.
         * @param name name of variable
         * @param value variable value
         */
        VariableValues(String name, String value) {
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

    /**
     * Utility to generate {@link GraphQLRequest}s through fluent configuration
     * methods.
     */
    public static final class Builder {
        private final List<FieldValue> fieldValues;
        private final List<VariableValues> variableValues;
        private final List<String> fragments;
        private String document;

        /**
         * Constructs a new GraphQLRequest Builder.
         */
        public Builder() {
            this.fieldValues = new ArrayList<>();
            this.variableValues = new ArrayList<>();
            this.fragments = new ArrayList<>();
        }

        /**
         * Sets the document string to be used in the GraphQLRequest.
         * @param document GraphQL document string
         * @return Current builder instance, for fluent configuration call chaining
         */
        public Builder document(@NonNull String document) {
            Builder.this.document = Objects.requireNonNull(document);
            return Builder.this;
        }

        /**
         * Adds a field value.
         * @param name Name of field
         * @param value Value of field
         * @return Current Builder instance for fluent method chaining
         */
        public Builder field(String name, String value) {
            Builder.this.fieldValues.add(new FieldValue(name, value));
            return Builder.this;
        }

        /**
         * Adds a variable value to the request.
         * @param key The name of the variable
         * @param value Its value
         * @return Current Builder instance, for fluent method chaining
         */
        public Builder addVariable(String key, String value) {
            variableValues.add(new VariableValues(key, value));
            return Builder.this;
        }

        /**
         * Adds a map of variable names and their string values.
         * @param variables Variables to be added to request
         * @return Current Builder instance for fluent configuration method chaining
         */
        public Builder addVariables(@Nullable Map<String, String> variables) {
            if (variables != null) {
                for (Map.Entry<String, String> variable : variables.entrySet()) {
                    addVariable(variable.getKey(), variable.getValue());
                }
            }
            return this;
        }

        /**
         * Adds a fragment to the request.
         * @param fragment A GraphQL fragment
         * @return Current builder instance for fluent chaining of configuration calls
         */
        public Builder addFragment(String fragment) {
            fragments.add(fragment);
            return Builder.this;
        }

        /**
         * Compiles the content of the request body, from the provided
         * document, variable(s), and fragment(s).
         * @return Content of the request body, as string
         */
        private String compileContent() {
            final StringBuilder completeQuery = new StringBuilder();
            final StringBuilder realQuery = new StringBuilder();

            completeQuery.append("{\"query\":")
                .append("\"");

            realQuery.append(document
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
                    completeQuery.append("\"")
                        .append(variableValues.getName())
                        .append("\":");
                    final Object value = variableValues.getValue();
                    if (value == null) {
                        completeQuery.append("null");
                    } else if (value instanceof Number || value instanceof Boolean) {
                        completeQuery.append(value.toString());
                    } else {
                        completeQuery.append("\"")
                            .append(value.toString())
                            .append("\"");
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
                contentString = contentString.replace(
                    "@" + fieldValue.getName(),
                    "\\\"" + fieldValue.getValue() + "\\\"");
            }

            while (contentString.contains("\\\\")) {
                contentString = contentString.replace("\\\\", "\\");
            }

            return contentString;
        }

        /**
         * Builds a GraphQLRequest.
         * @return A GraphQLRequest instance
         */
        public GraphQLRequest build() {
            return new GraphQLRequest(compileContent());
        }
    }
}
