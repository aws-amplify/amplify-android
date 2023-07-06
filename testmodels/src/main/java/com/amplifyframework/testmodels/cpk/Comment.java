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
import com.amplifyframework.core.model.annotations.Index;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.core.model.annotations.ModelField;
import com.amplifyframework.core.model.query.predicate.QueryField;
import com.amplifyframework.core.model.temporal.Temporal;

import java.util.Objects;

import static com.amplifyframework.core.model.query.predicate.QueryField.field;

/** This is an auto generated class representing the Comment type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "Comments", type = Model.Type.USER, version = 1)
@Index(name = "undefined", fields = {"commentId"})
public final class Comment implements Model {
  public static final QueryField COMMENT_ID = field("Comment", "commentId");
  public static final QueryField POST = field("Comment", "postCommentsPostId");
  public static final QueryField CONTENT = field("Comment", "content");
  private final @ModelField(targetType="ID", isRequired = true) String commentId;
  private final @ModelField(targetType="Post", isRequired = true) @BelongsTo(targetName = "postCommentsPostId", targetNames = {"postCommentsPostId", "postCommentsTitle", "postCommentsCreatedAt", "postCommentsRating"}, type = Post.class) Post post;
  private final @ModelField(targetType="String", isRequired = true) String content;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime createdAt;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime updatedAt;
  /** @deprecated This API is internal to Amplify and should not be used. */
  @Deprecated
   public String resolveIdentifier() {
    return commentId;
  }
  
  public String getCommentId() {
      return commentId;
  }
  
  public Post getPost() {
      return post;
  }
  
  public String getContent() {
      return content;
  }
  
  public Temporal.DateTime getCreatedAt() {
      return createdAt;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  private Comment(String commentId, Post post, String content) {
    this.commentId = commentId;
    this.post = post;
    this.content = content;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      Comment comment = (Comment) obj;
      return ObjectsCompat.equals(getCommentId(), comment.getCommentId()) &&
              ObjectsCompat.equals(getPost(), comment.getPost()) &&
              ObjectsCompat.equals(getContent(), comment.getContent()) &&
              ObjectsCompat.equals(getCreatedAt(), comment.getCreatedAt()) &&
              ObjectsCompat.equals(getUpdatedAt(), comment.getUpdatedAt());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getCommentId())
      .append(getPost())
      .append(getContent())
      .append(getCreatedAt())
      .append(getUpdatedAt())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("Comment {")
      .append("commentId=" + String.valueOf(getCommentId()) + ", ")
      .append("post=" + String.valueOf(getPost()) + ", ")
      .append("content=" + String.valueOf(getContent()) + ", ")
      .append("createdAt=" + String.valueOf(getCreatedAt()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()))
      .append("}")
      .toString();
  }
  
  public static CommentIdStep builder() {
      return new Builder();
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(commentId,
      post,
      content);
  }
  public interface CommentIdStep {
    PostStep commentId(String commentId);
  }
  

  public interface PostStep {
    ContentStep post(Post post);
  }
  

  public interface ContentStep {
    BuildStep content(String content);
  }
  

  public interface BuildStep {
    Comment build();
  }
  

  public static class Builder implements CommentIdStep, PostStep, ContentStep, BuildStep {
    private String commentId;
    private Post post;
    private String content;
    @Override
     public Comment build() {
        
        return new Comment(
          commentId,
          post,
          content);
    }
    
    @Override
     public PostStep commentId(String commentId) {
        Objects.requireNonNull(commentId);
        this.commentId = commentId;
        return this;
    }
    
    @Override
     public ContentStep post(Post post) {
        Objects.requireNonNull(post);
        this.post = post;
        return this;
    }
    
    @Override
     public BuildStep content(String content) {
        Objects.requireNonNull(content);
        this.content = content;
        return this;
    }
  }
  

  public final class CopyOfBuilder extends Builder {
    private CopyOfBuilder(String commentId, Post post, String content) {
      super.commentId(commentId)
        .post(post)
        .content(content);
    }
    
    @Override
     public CopyOfBuilder commentId(String commentId) {
      return (CopyOfBuilder) super.commentId(commentId);
    }
    
    @Override
     public CopyOfBuilder post(Post post) {
      return (CopyOfBuilder) super.post(post);
    }
    
    @Override
     public CopyOfBuilder content(String content) {
      return (CopyOfBuilder) super.content(content);
    }
  }
  

  public static class CommentIdentifier extends ModelIdentifier<Comment> {
    private static final long serialVersionUID = 1L;
    public CommentIdentifier(String commentId) {
      super(commentId);
    }
  }
  
}
