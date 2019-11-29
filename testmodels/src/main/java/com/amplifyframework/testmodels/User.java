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
import com.amplifyframework.core.model.annotations.HasMany;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.core.model.annotations.ModelField;
import com.amplifyframework.core.model.query.predicate.QueryField;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** This is an auto generated class representing the User type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "Users")
public final class User implements Model {
    public static final QueryField ID = QueryField.field("id");
    public static final QueryField USERNAME = QueryField.field("username");
    public static final QueryField POSTS = QueryField.field("posts");
    private final @ModelField(targetType="ID", isRequired = true) String id;
    private final @ModelField(targetType="String", isRequired = true) String username;
    private final @ModelField(targetType="PostEditor") @HasMany(associatedWith = "editor", type = PostEditor.class) List<PostEditor> posts;
    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public List<PostEditor> getPosts() {
        return posts;
    }

    private User(String id, String username, List<PostEditor> posts) {
        this.id = id;
        this.username = username;
        this.posts = posts;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if(obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            User user = (User) obj;
            return ObjectsCompat.equals(getId(), user.getId()) &&
                    ObjectsCompat.equals(getUsername(), user.getUsername()) &&
                    ObjectsCompat.equals(getPosts(), user.getPosts());
        }
    }

    @Override
    public int hashCode() {
        return new StringBuilder()
                .append(getId())
                .append(getUsername())
                .append(getPosts())
                .hashCode();
    }

    public static UsernameStep builder() {
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
     **/
    public static User justId(String id) {
        try {
            UUID.fromString(id); // Check that ID is in the UUID format - if not an exception is thrown
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Model IDs must be unique in the format of UUID. This method is for creating instances " +
                            "of an existing object with only its ID field for sending as a mutation parameter. When " +
                            "creating a new object, use the standard builder method and leave the ID field blank."
            );
        }
        return new User(
                id,
                null,
                null
        );
    }

    public CopyOfBuilder copyOfBuilder() {
        return new CopyOfBuilder(id,
                username,
                posts);
    }
    public interface UsernameStep {
        BuildStep username(String username);
    }


    public interface BuildStep {
        User build();
        BuildStep id(String id) throws IllegalArgumentException;
        BuildStep posts(List<PostEditor> posts);
    }


    public static class Builder implements UsernameStep, BuildStep {
        private String id;
        private String username;
        private List<PostEditor> posts;
        @Override
        public User build() {
            String id = this.id != null ? this.id : UUID.randomUUID().toString();

            return new User(
                    id,
                    username,
                    posts);
        }

        @Override
        public BuildStep username(String username) {
            Objects.requireNonNull(username);
            this.username = username;
            return this;
        }

        @Override
        public BuildStep posts(List<PostEditor> posts) {
            this.posts = posts;
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


    public final class CopyOfBuilder extends Builder {
        private CopyOfBuilder(String id, String username, List<PostEditor> posts) {
            super.id(id);
            super.username(username)
                    .posts(posts);
        }

        @Override
        public CopyOfBuilder username(String username) {
            return (CopyOfBuilder) super.username(username);
        }

        @Override
        public CopyOfBuilder posts(List<PostEditor> posts) {
            return (CopyOfBuilder) super.posts(posts);
        }
    }

}
