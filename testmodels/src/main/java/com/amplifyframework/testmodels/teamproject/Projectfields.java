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

package com.amplifyframework.testmodels.teamproject;

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.annotations.BelongsTo;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.core.model.annotations.ModelField;
import com.amplifyframework.core.model.query.predicate.QueryField;

import java.util.UUID;

/** This is an auto generated class representing the projectfields type in your schema. */
@SuppressWarnings("all")
@ModelConfig
public final class Projectfields implements Model {
    public static final QueryField ID = QueryField.field("id");
    public static final QueryField NAME = QueryField.field("name");
    public static final QueryField TEAM = QueryField.field("team");
    private final @ModelField(targetType="ID", isRequired = true) String id;
    private final @ModelField(targetType="String") String name;
    private final @ModelField(targetType="Team") @BelongsTo(targetName = "teamID", type = Team.class) Team team;
    public String getId() {
        return id;
    }

    @NonNull
    public String resolveIdentifier() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Team getTeam() {
        return team;
    }

    private Projectfields(String id, String name, Team team) {
        this.id = id;
        this.name = name;
        this.team = team;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if(obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            Projectfields projectfields = (Projectfields) obj;
            return ObjectsCompat.equals(getId(), projectfields.getId()) &&
                    ObjectsCompat.equals(getName(), projectfields.getName()) &&
                    ObjectsCompat.equals(getTeam(), projectfields.getTeam());
        }
    }

    @Override
    public int hashCode() {
        return new StringBuilder()
                .append(getId())
                .append(getName())
                .append(getTeam())
                .hashCode();
    }

    public static BuildStep builder() {
        return new Builder();
    }

    /**
     * WARNING: This method should not be used to build an instance of this object for a CREATE mutation.
     * This is a convenience method to return an instance of the object with only its ID populated
     * to be used in the context of a parameter in a delete mutation or referencing a foreign key
     * in a relationship.
     * @param id the id of the existing item this instance will represent
     * @return an instance of this model with only ID populated
     * @throws IllegalArgumentException Checks that ID is in the proper format
     */
    public static Projectfields justId(String id) {
        try {
            UUID.fromString(id); // Check that ID is in the UUID format - if not an exception is thrown
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Model IDs must be unique in the format of UUID. This method is for creating instances " +
                            "of an existing object with only its ID field for sending as a mutation parameter. When " +
                            "creating a new object, use the standard builder method and leave the ID field blank."
            );
        }
        return new Projectfields(
                id,
                null,
                null
        );
    }

    public CopyOfBuilder copyOfBuilder() {
        return new CopyOfBuilder(id,
                name,
                team);
    }
    public interface BuildStep {
        Projectfields build();
        BuildStep id(String id) throws IllegalArgumentException;
        BuildStep name(String name);
        BuildStep team(Team team);
    }


    public static class Builder implements BuildStep {
        private String id;
        private String name;
        private Team team;
        @Override
        public Projectfields build() {
            String id = this.id != null ? this.id : UUID.randomUUID().toString();

            return new Projectfields(
                    id,
                    name,
                    team);
        }

        @Override
        public BuildStep name(String name) {
            this.name = name;
            return this;
        }

        @Override
        public BuildStep team(Team team) {
            this.team = team;
            return this;
        }

        /**
         * WARNING: Do not set ID when creating a new object. Leave this blank and one will be auto generated for you.
         * This should only be set when referring to an already existing object.
         * @param id id
         * @return Current Builder instance, for fluent method chaining
         * @throws IllegalArgumentException Checks that ID is in the proper format
         */
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


    public final class CopyOfBuilder extends Builder {
        private CopyOfBuilder(String id, String name, Team team) {
            super.id(id);
            super.name(name)
                    .team(team);
        }

        @Override
        public CopyOfBuilder name(String name) {
            return (CopyOfBuilder) super.name(name);
        }

        @Override
        public CopyOfBuilder team(Team team) {
            return (CopyOfBuilder) super.team(team);
        }
    }

}
