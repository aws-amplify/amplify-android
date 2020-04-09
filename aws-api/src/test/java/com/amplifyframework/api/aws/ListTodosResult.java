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

package com.amplifyframework.api.aws;

import androidx.core.util.ObjectsCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * A hypothetical model for some data returned from a GraphQL API.
 * This is a placeholder for the model that gets code generated by the
 * Amplify CLI.
 */
final class ListTodosResult {
    private final List<Todo> items;

    ListTodosResult() {
        this.items = new ArrayList<>();
    }

    ListTodosResult(final List<Todo> items) {
        this.items = new ArrayList<>();
        if (items != null) {
            this.items.addAll(items);
        }
    }

    List<Todo> getItems() {
        return ListTodosResult.this.items;
    }

    @Override
    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        ListTodosResult that = (ListTodosResult) thatObject;

        return ObjectsCompat.equals(items, that.items);
    }

    @Override
    public int hashCode() {
        return items != null ? items.hashCode() : 0;
    }

    static final class Todo {
        private final String id;
        private final String name;
        private final String description;

        Todo() {
            this.id = null;
            this.name = null;
            this.description = null;
        }

        Todo(Builder builder) {
            this.id = builder.getId();
            this.name = builder.getName();
            this.description = builder.getDescription();
        }

        String getId() {
            return Todo.this.id;
        }

        String getName() {
            return Todo.this.name;
        }

        String getDescription() {
            return Todo.this.description;
        }

        static Builder builder() {
            return new Builder();
        }

        @Override
        public boolean equals(Object thatObject) {
            if (this == thatObject) {
                return true;
            }
            if (thatObject == null || getClass() != thatObject.getClass()) {
                return false;
            }

            Todo todo = (Todo) thatObject;

            if (!ObjectsCompat.equals(id, todo.id)) {
                return false;
            }
            if (!ObjectsCompat.equals(name, todo.name)) {
                return false;
            }
            return ObjectsCompat.equals(description, todo.description);
        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (name != null ? name.hashCode() : 0);
            result = 31 * result + (description != null ? description.hashCode() : 0);
            return result;
        }

        static final class Builder {
            private String id;
            private String name;
            private String description;

            public Builder id(@SuppressWarnings("ParameterName") String id) {
                this.id = id;
                return this;
            }

            public Builder name(String name) {
                this.name = name;
                return this;
            }

            public Builder description(String description) {
                this.description = description;
                return this;
            }

            public Todo build() {
                return new Todo(this);
            }

            String getId() {
                return id;
            }

            String getName() {
                return name;
            }

            String getDescription() {
                return description;
            }
        }
    }
}

