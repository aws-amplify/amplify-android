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

package com.amplifyframework.testmodels.ratingsblog;

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.annotations.BelongsTo;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.core.model.annotations.ModelField;
import com.amplifyframework.core.model.query.predicate.QueryField;

import java.util.Objects;
import java.util.UUID;

/** This is an auto generated class representing the Rating type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "Ratings")
public final class Rating implements Model {
    public static final QueryField ID = QueryField.field("id");
    public static final QueryField STARS = QueryField.field("stars");
    public static final QueryField POST = QueryField.field("ratingPostId");
    private final @ModelField(targetType="ID", isRequired = true) String id;
    private final @ModelField(targetType="Int", isRequired = true) Integer stars;
    private final @ModelField(targetType="Post", isRequired = true) @BelongsTo(targetName = "ratingPostId", type = Post.class) Post post;
    public String getId() {
        return id;
    }

    @NonNull
    public String resolveIdentifier() {
        return id;
    }

    public Integer getStars() {
        return stars;
    }

    public Post getPost() {
        return post;
    }

    private Rating(String id, Integer stars, Post post) {
        this.id = id;
        this.stars = stars;
        this.post = post;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if(obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            Rating rating = (Rating) obj;
            return ObjectsCompat.equals(getId(), rating.getId()) &&
                    ObjectsCompat.equals(getStars(), rating.getStars()) &&
                    ObjectsCompat.equals(getPost(), rating.getPost());
        }
    }

    @Override
    public int hashCode() {
        return new StringBuilder()
                .append(getId())
                .append(getStars())
                .append(getPost())
                .hashCode();
    }

    public static StarsStep builder() {
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
    public static Rating justId(String id) {
        try {
            UUID.fromString(id); // Check that ID is in the UUID format - if not an exception is thrown
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Model IDs must be unique in the format of UUID. This method is for creating instances " +
                            "of an existing object with only its ID field for sending as a mutation parameter. When " +
                            "creating a new object, use the standard builder method and leave the ID field blank."
            );
        }
        return new Rating(
                id,
                null,
                null
        );
    }

    public CopyOfBuilder copyOfBuilder() {
        return new CopyOfBuilder(id,
                stars,
                post);
    }
    public interface StarsStep {
        PostStep stars(Integer stars);
    }


    public interface PostStep {
        BuildStep post(Post post);
    }


    public interface BuildStep {
        Rating build();
        BuildStep id(String id) throws IllegalArgumentException;
    }


    public static class Builder implements StarsStep, PostStep, BuildStep {
        private String id;
        private Integer stars;
        private Post post;
        @Override
        public Rating build() {
            String id = this.id != null ? this.id : UUID.randomUUID().toString();

            return new Rating(
                    id,
                    stars,
                    post);
        }

        @Override
        public PostStep stars(Integer stars) {
            Objects.requireNonNull(stars);
            this.stars = stars;
            return this;
        }

        @Override
        public BuildStep post(Post post) {
            Objects.requireNonNull(post);
            this.post = post;
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
        private CopyOfBuilder(String id, Integer stars, Post post) {
            super.id(id);
            super.stars(stars)
                    .post(post);
        }

        @Override
        public CopyOfBuilder stars(Integer stars) {
            return (CopyOfBuilder) super.stars(stars);
        }

        @Override
        public CopyOfBuilder post(Post post) {
            return (CopyOfBuilder) super.post(post);
        }
    }

}
