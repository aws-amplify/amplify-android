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
import com.amplifyframework.core.model.annotations.BelongsTo;
import com.amplifyframework.core.model.annotations.HasMany;
import com.amplifyframework.core.model.annotations.HasOne;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.core.model.annotations.ModelField;
import com.amplifyframework.core.model.query.predicate.QueryField;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** This is an auto generated class representing the Post type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "Posts")
public final class Post implements Model {
    public static final QueryField ID = QueryField.field("id");
    public static final QueryField TITLE = QueryField.field("title");
    public static final QueryField BLOG = QueryField.field("blog");
    public static final QueryField RATING = QueryField.field("rating");
    public static final QueryField EDITORS = QueryField.field("editors");
    private final @ModelField(targetType="ID", isRequired = true) String id;
    private final @ModelField(targetType="String", isRequired = true) String title;
    private final @ModelField(targetType="Blog", isRequired = true) @BelongsTo(targetName = "postBlogId", type = Blog.class) Blog blog;
    private final @ModelField(targetType="Rating") @HasOne(associatedWith = "post", type = Rating.class) Rating rating;
    private final @ModelField(targetType="PostEditor") @HasMany(associatedWith = "post", type = PostEditor.class) List<PostEditor> editors;
    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Blog getBlog() {
        return blog;
    }

    public Rating getRating() {
        return rating;
    }

    public List<PostEditor> getEditors() {
        return editors;
    }

    private Post(String id, String title, Blog blog, Rating rating, List<PostEditor> editors) {
        this.id = id;
        this.title = title;
        this.blog = blog;
        this.rating = rating;
        this.editors = editors;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if(obj == null || getClass() != obj.getClass()) {
            return false;
        } else {
            Post post = (Post) obj;
            return ObjectsCompat.equals(getId(), post.getId()) &&
                    ObjectsCompat.equals(getTitle(), post.getTitle()) &&
                    ObjectsCompat.equals(getBlog(), post.getBlog()) &&
                    ObjectsCompat.equals(getRating(), post.getRating()) &&
                    ObjectsCompat.equals(getEditors(), post.getEditors());
        }
    }

    @Override
    public int hashCode() {
        return new StringBuilder()
                .append(getId())
                .append(getTitle())
                .append(getBlog())
                .append(getRating())
                .append(getEditors())
                .hashCode();
    }

    public static TitleStep builder() {
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
    public static Post justId(String id) {
        try {
            UUID.fromString(id); // Check that ID is in the UUID format - if not an exception is thrown
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Model IDs must be unique in the format of UUID. This method is for creating instances " +
                            "of an existing object with only its ID field for sending as a mutation parameter. When " +
                            "creating a new object, use the standard builder method and leave the ID field blank."
            );
        }
        return new Post(
                id,
                null,
                null,
                null,
                null
        );
    }

    public CopyOfBuilder copyOfBuilder() {
        return new CopyOfBuilder(id,
                title,
                blog,
                rating,
                editors);
    }
    public interface TitleStep {
        BlogStep title(String title);
    }


    public interface BlogStep {
        BuildStep blog(Blog blog);
    }


    public interface BuildStep {
        Post build();
        BuildStep id(String id) throws IllegalArgumentException;
        BuildStep rating(Rating rating);
        BuildStep editors(List<PostEditor> editors);
    }


    public static class Builder implements TitleStep, BlogStep, BuildStep {
        private String id;
        private String title;
        private Blog blog;
        private Rating rating;
        private List<PostEditor> editors;
        @Override
        public Post build() {
            String id = this.id != null ? this.id : UUID.randomUUID().toString();

            return new Post(
                    id,
                    title,
                    blog,
                    rating,
                    editors);
        }

        @Override
        public BlogStep title(String title) {
            Objects.requireNonNull(title);
            this.title = title;
            return this;
        }

        @Override
        public BuildStep blog(Blog blog) {
            Objects.requireNonNull(blog);
            this.blog = blog;
            return this;
        }

        @Override
        public BuildStep rating(Rating rating) {
            this.rating = rating;
            return this;
        }

        @Override
        public BuildStep editors(List<PostEditor> editors) {
            this.editors = editors;
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
        private CopyOfBuilder(String id, String title, Blog blog, Rating rating, List<PostEditor> editors) {
            super.id(id);
            super.title(title)
                    .blog(blog)
                    .rating(rating)
                    .editors(editors);
        }

        @Override
        public CopyOfBuilder title(String title) {
            return (CopyOfBuilder) super.title(title);
        }

        @Override
        public CopyOfBuilder blog(Blog blog) {
            return (CopyOfBuilder) super.blog(blog);
        }

        @Override
        public CopyOfBuilder rating(Rating rating) {
            return (CopyOfBuilder) super.rating(rating);
        }

        @Override
        public CopyOfBuilder editors(List<PostEditor> editors) {
            return (CopyOfBuilder) super.editors(editors);
        }
    }

}

