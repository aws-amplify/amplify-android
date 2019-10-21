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

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic query using which API calls will be made.
 * @param <T> The object type to invoke API calls on
 */
public class Query<T> {
    private final String query;
    private final String prefix;
    private final Class<T> classToCast;

    private final List<FieldValue> fieldValues = new ArrayList<>();
    private final List<VariableValues> variableValues = new ArrayList<>();
    private final List<String> fragments = new ArrayList<>();

    private GraphQLCallback<T> callback;

    /**
     * Constructs a query object.
     * @param prefix query prefix
     * @param query query document
     * @param classToCast class to cast to
     */
    public Query(String prefix, String query, Class<T> classToCast) {
        this.prefix = prefix;
        this.query = query;
        this.classToCast = classToCast;
    }

    /**
     * Duplicates query with different class to cast.
     * @param <R> new class type
     * @param <Q> An instance of Query object
     * @param targetClazz class to be converted to
     * @return a new query object with different class to cast to
     */
    @SuppressWarnings("unchecked")
    protected <R, Q extends Query<R>> Q cast(Class<R> targetClazz) {
        Query<R> newQuery = new Query<>(prefix, query, targetClazz);
        for (FieldValue field : fieldValues) {
            newQuery.field(field.getName(), field.getValue());
        }
        for (VariableValues variable : variableValues) {
            newQuery.variable(variable.getName(), variable.getValue());
        }
        for (String fragment : fragments) {
            newQuery.fragment(fragment);
        }
        return (Q) newQuery;
    }

    /**
     * Sets field value.
     * @param <Q> An instance of Query object
     * @param name name of field value
     * @param value field value
     * @return this query object containing new field value
     */
    @SuppressWarnings("unchecked")
    protected <Q extends Query<T>> Q field(String name, String value) {
        fieldValues.add(new FieldValue(name, value));
        return (Q) this;
    }

    /**
     * Processes query parameters into a query string.
     * @return processed query string
     */
    public String getContent() {
        final StringBuilder completeQuery = new StringBuilder();
        final StringBuilder realQuery = new StringBuilder();

        completeQuery.append("{\"query\":")
                .append("\"");

        if (prefix != null) {
            completeQuery.append(prefix).append(" ");
        }

        realQuery.append(query.replace("\"", "\\\""));

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

        Log.d("real query", realQuery.toString());

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
     * Executed on response.
     * @param responseFactory Factory to build response objects from JSON string
     * @param responseJson json string to convert from
     */
    @SuppressWarnings("unchecked")
    public void onResponse(ResponseFactory responseFactory, String responseJson) {
        final Response<T> response;
        try {
            if (classToCast == null || String.class.equals(classToCast)) {
                response = new Response<>((T) responseJson, null);
            } else { //convert only if cast != string
                response = responseFactory.buildResponse(responseJson, classToCast);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
            callback.onError(exception);
            return;
        }

        new Handler(Looper.getMainLooper()).post(() -> callback.onResponse(response));
    }

    /**
     * Executed on error.
     * @param exception thrown exception
     */
    public void onError(Exception exception) {
        callback.onError(exception);
    }

    /**
     * Attaches a callback to query object.
     * @param <Q> An instance of Query object
     * @param callback callback object to be attached
     * @return this query object for chaining
     */
    @SuppressWarnings("unchecked")
    public <Q extends Query<T>> Q withCallback(GraphQLCallback<T> callback) {
        this.callback = callback;
        return (Q) this;
    }

    /**
     * Attaches variable key-value pair.
     * @param <Q> An instance of Query object
     * @param key variable name
     * @param value variable value
     * @return this query object for chaining
     */
    @SuppressWarnings("unchecked")
    public <Q extends Query<T>> Q variable(String key, Object value) {
        variableValues.add(new VariableValues(key, value));
        return (Q) this;
    }

    /**
     * Attaches fragment.
     * @param <Q> An instance of Query object
     * @param fragment fragment to be attached
     * @return this query object for chaining
     */
    @SuppressWarnings("unchecked")
    public <Q extends Query<T>> Q fragment(String fragment) {
        fragments.add(fragment);
        return (Q) this;
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
