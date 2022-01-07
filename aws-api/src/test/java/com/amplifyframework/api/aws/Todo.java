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

import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.annotations.ModelConfig;

@ModelConfig(listPluralName = "Todos", syncPluralName = "Todos")
final class Todo implements Model {
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

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
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
