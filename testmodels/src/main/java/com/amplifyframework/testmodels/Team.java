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

package com.amplifyframework.testmodels;

import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.core.model.annotations.ModelField;
import com.amplifyframework.core.model.query.predicate.QueryField;

import java.util.Objects;
import java.util.UUID;

/** This is an auto generated class representing the Team type in your schema. */
@SuppressWarnings("all")
@ModelConfig
public final class Team implements Model {
    public static final QueryField ID = QueryField.field("id");
    public static final QueryField NAME = QueryField.field("name");
    private final @ModelField(targetType="ID", isRequired = true) String id;
    private final @ModelField(targetType="String", isRequired = true) String name;
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    private Team(String id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if(obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            Team team = (Team) obj;
            return ObjectsCompat.equals(getId(), team.getId()) &&
                    ObjectsCompat.equals(getName(), team.getName());
        }
    }

    @Override
    public int hashCode() {
        return new StringBuilder()
                .append(getId())
                .append(getName())
                .hashCode();
    }

    /**
     * WARNING: This method should not be used to build an instance of this object for a CREATE mutation.
     *
     * This is a convenience method to return an instance of the object with only its ID populated
     * to be used in the context of a parameter in a delete mutation or referencing a foreign key
     * in a relationship.
     * @param id the id of the existing item this instance will represent
     * @return an instance of this model with only ID populated
     * @throws IllegalArgumentException Checks that ID is in the proper format
     */
    public static Team justId(String id) {
        try {
            UUID.fromString(id); // Check that ID is in the UUID format - if not an exception is thrown
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Model IDs must be unique in the format of UUID. This method is for creating instances " +
                            "of an existing object with only its ID field for sending as a mutation parameter. When " +
                            "creating a new object, use the standard builder method and leave the ID field blank."
            );
        }

        return new Team(
                id,
                null
        );
    }

    public static NameStep builder() {
        return new Builder();
    }

    public NewBuilder newBuilder() {
        return new NewBuilder(id,
                name);
    }
    public interface NameStep {
        BuildStep name(String name);
    }


    public interface BuildStep {
        Team build();
        BuildStep id(String id) throws IllegalArgumentException;
    }


    public static class Builder implements NameStep, BuildStep {
        private String id;
        private String name;
        @Override
        public Team build() {
            String id = this.id != null ? this.id : UUID.randomUUID().toString();

            return new Team(
                    id,
                    name);
        }

        @Override
        public BuildStep name(String name) {
            Objects.requireNonNull(name);
            this.name = name;
            return this;
        }

        /**
         * WARNING: Do not set ID when creating a new object. Leave this blank and one will be auto generated for you.
         * This should only be set when referring to an already existing object.
         * @param id id
         * @return Current Builder instance, for fluent method chaining
         * @throws IllegalArgumentException Checks that ID is in the proper format
         **/
        public BuildStep id(String id) throws IllegalArgumentException {
            this.id = id;

            try {
                UUID.fromString(id); // Check that ID is in the UUID format - if not an exception is thrown
            } catch (Exception exception) {
                throw new IllegalArgumentException("Model IDs must be unique in the format of UUID.",
                        exception);
            }

            return this;
        }
    }


    public final class NewBuilder extends Builder {
        private NewBuilder(String id, String name) {
            super.id(id);
            super.name(name);
        }

        @Override
        public NewBuilder name(String name) {
            return (NewBuilder) super.name(name);
        }
    }

}
