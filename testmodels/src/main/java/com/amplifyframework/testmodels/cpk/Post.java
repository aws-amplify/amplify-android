/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.testmodels.cpk;

import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelIdentifier;
import com.amplifyframework.core.model.annotations.BelongsTo;
import com.amplifyframework.core.model.annotations.HasMany;
import com.amplifyframework.core.model.annotations.Index;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.core.model.annotations.ModelField;
import com.amplifyframework.core.model.query.predicate.QueryField;
import com.amplifyframework.core.model.temporal.Temporal;

import java.util.List;
import java.util.Objects;

import static com.amplifyframework.core.model.query.predicate.QueryField.field;

/** This is an auto generated class representing the Post type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "Posts", type = Model.Type.USER, version = 1)
@Index(name = "undefined", fields = {"postId","title","createdAt","rating"})
public final class Post implements Model {
  public static final QueryField POST_ID = field("Post", "postId");
  public static final QueryField TITLE = field("Post", "title");
  public static final QueryField CREATED_AT = field("Post", "createdAt");
  public static final QueryField RATING = field("Post", "rating");
  public static final QueryField BLOG = field("Post", "blogPostsBlogId");
  private final @ModelField(targetType="ID", isRequired = true) String postId;
  private final @ModelField(targetType="String", isRequired = true) String title;
  private final @ModelField(targetType="AWSDateTime", isRequired = true) Temporal.DateTime createdAt;
  private final @ModelField(targetType="Float", isRequired = true) Double rating;
  private final @ModelField(targetType="Blog") @BelongsTo(targetName = "blogPostsBlogId", targetNames = {"blogPostsBlogId", "blogPostsSiteId"}, type = Blog.class) Blog blog;
  private final @ModelField(targetType="Comment") @HasMany(associatedWith = "post", type = Comment.class) List<Comment> comments = null;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime updatedAt;
  private PostIdentifier postIdentifier;
  /** @deprecated This API is internal to Amplify and should not be used. */
  @Deprecated
   public PostIdentifier resolveIdentifier() {
    if (postIdentifier == null) {
      this.postIdentifier = new PostIdentifier(postId, title, createdAt, rating);
    }
    return postIdentifier;
  }
  
  public String getPostId() {
      return postId;
  }
  
  public String getTitle() {
      return title;
  }
  
  public Temporal.DateTime getCreatedAt() {
      return createdAt;
  }
  
  public Double getRating() {
      return rating;
  }
  
  public Blog getBlog() {
      return blog;
  }
  
  public List<Comment> getComments() {
      return comments;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  private Post(String postId, String title, Temporal.DateTime createdAt, Double rating, Blog blog) {
    this.postId = postId;
    this.title = title;
    this.createdAt = createdAt;
    this.rating = rating;
    this.blog = blog;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      Post post = (Post) obj;
      return ObjectsCompat.equals(getPostId(), post.getPostId()) &&
              ObjectsCompat.equals(getTitle(), post.getTitle()) &&
              ObjectsCompat.equals(getCreatedAt(), post.getCreatedAt()) &&
              ObjectsCompat.equals(getRating(), post.getRating()) &&
              ObjectsCompat.equals(getBlog(), post.getBlog()) &&
              ObjectsCompat.equals(getUpdatedAt(), post.getUpdatedAt());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getPostId())
      .append(getTitle())
      .append(getCreatedAt())
      .append(getRating())
      .append(getBlog())
      .append(getUpdatedAt())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("Post {")
      .append("postId=" + String.valueOf(getPostId()) + ", ")
      .append("title=" + String.valueOf(getTitle()) + ", ")
      .append("createdAt=" + String.valueOf(getCreatedAt()) + ", ")
      .append("rating=" + String.valueOf(getRating()) + ", ")
      .append("blog=" + String.valueOf(getBlog()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()))
      .append("}")
      .toString();
  }
  
  public static PostIdStep builder() {
      return new Builder();
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(postId,
      title,
      createdAt,
      rating,
      blog);
  }
  public interface PostIdStep {
    TitleStep postId(String postId);
  }
  

  public interface TitleStep {
    CreatedAtStep title(String title);
  }
  

  public interface CreatedAtStep {
    RatingStep createdAt(Temporal.DateTime createdAt);
  }
  

  public interface RatingStep {
    BuildStep rating(Double rating);
  }
  

  public interface BuildStep {
    Post build();
    BuildStep blog(Blog blog);
  }
  

  public static class Builder implements PostIdStep, TitleStep, CreatedAtStep, RatingStep, BuildStep {
    private String postId;
    private String title;
    private Temporal.DateTime createdAt;
    private Double rating;
    private Blog blog;
    @Override
     public Post build() {
        
        return new Post(
          postId,
          title,
          createdAt,
          rating,
          blog);
    }
    
    @Override
     public TitleStep postId(String postId) {
        Objects.requireNonNull(postId);
        this.postId = postId;
        return this;
    }
    
    @Override
     public CreatedAtStep title(String title) {
        Objects.requireNonNull(title);
        this.title = title;
        return this;
    }
    
    @Override
     public RatingStep createdAt(Temporal.DateTime createdAt) {
        Objects.requireNonNull(createdAt);
        this.createdAt = createdAt;
        return this;
    }
    
    @Override
     public BuildStep rating(Double rating) {
        Objects.requireNonNull(rating);
        this.rating = rating;
        return this;
    }
    
    @Override
     public BuildStep blog(Blog blog) {
        this.blog = blog;
        return this;
    }
  }
  

  public final class CopyOfBuilder extends Builder {
    private CopyOfBuilder(String postId, String title, Temporal.DateTime createdAt, Double rating, Blog blog) {
      super.postId(postId)
        .title(title)
        .createdAt(createdAt)
        .rating(rating)
        .blog(blog);
    }
    
    @Override
     public CopyOfBuilder postId(String postId) {
      return (CopyOfBuilder) super.postId(postId);
    }
    
    @Override
     public CopyOfBuilder title(String title) {
      return (CopyOfBuilder) super.title(title);
    }
    
    @Override
     public CopyOfBuilder createdAt(Temporal.DateTime createdAt) {
      return (CopyOfBuilder) super.createdAt(createdAt);
    }
    
    @Override
     public CopyOfBuilder rating(Double rating) {
      return (CopyOfBuilder) super.rating(rating);
    }
    
    @Override
     public CopyOfBuilder blog(Blog blog) {
      return (CopyOfBuilder) super.blog(blog);
    }
  }
  

  public static class PostIdentifier extends ModelIdentifier<Post> {
    private static final long serialVersionUID = 1L;
    public PostIdentifier(String postId, String title, Temporal.DateTime createdAt, Double rating) {
      super(postId, title, createdAt, rating);
    }
  }
  
}
