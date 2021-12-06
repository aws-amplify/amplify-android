package com.amplifyframework.testmodels.transformerv2;

import com.amplifyframework.core.model.temporal.Temporal;

import java.util.List;
import java.util.UUID;
import java.util.Objects;

import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.annotations.Index;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.core.model.annotations.ModelField;
import com.amplifyframework.core.model.query.predicate.QueryField;

import static com.amplifyframework.core.model.query.predicate.QueryField.field;

/** This is an auto generated class representing the Comment7 type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "Comment7s")
@Index(name = "byPost", fields = {"postID","content"})
public final class Comment7 implements Model {
  public static final QueryField ID = field("Comment7", "id");
  public static final QueryField POST_ID = field("Comment7", "postID");
  public static final QueryField CONTENT = field("Comment7", "content");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="ID", isRequired = true) String postID;
  private final @ModelField(targetType="String", isRequired = true) String content;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime createdAt;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime updatedAt;
  public String getId() {
      return id;
  }
  
  public String getPostId() {
      return postID;
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
  
  private Comment7(String id, String postID, String content) {
    this.id = id;
    this.postID = postID;
    this.content = content;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      Comment7 comment7 = (Comment7) obj;
      return ObjectsCompat.equals(getId(), comment7.getId()) &&
              ObjectsCompat.equals(getPostId(), comment7.getPostId()) &&
              ObjectsCompat.equals(getContent(), comment7.getContent()) &&
              ObjectsCompat.equals(getCreatedAt(), comment7.getCreatedAt()) &&
              ObjectsCompat.equals(getUpdatedAt(), comment7.getUpdatedAt());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getPostId())
      .append(getContent())
      .append(getCreatedAt())
      .append(getUpdatedAt())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("Comment7 {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("postID=" + String.valueOf(getPostId()) + ", ")
      .append("content=" + String.valueOf(getContent()) + ", ")
      .append("createdAt=" + String.valueOf(getCreatedAt()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()))
      .append("}")
      .toString();
  }
  
  public static PostIdStep builder() {
      return new Builder();
  }
  
  /** 
   * WARNING: This method should not be used to build an instance of this object for a CREATE mutation.
   * This is a convenience method to return an instance of the object with only its ID populated
   * to be used in the context of a parameter in a delete mutation or referencing a foreign key
   * in a relationship.
   * @param id the id of the existing item this instance will represent
   * @return an instance of this model with only ID populated
   */
  public static Comment7 justId(String id) {
    return new Comment7(
      id,
      null,
      null
    );
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(id,
      postID,
      content);
  }
  public interface PostIdStep {
    ContentStep postId(String postId);
  }
  

  public interface ContentStep {
    BuildStep content(String content);
  }
  

  public interface BuildStep {
    Comment7 build();
    BuildStep id(String id);
  }
  

  public static class Builder implements PostIdStep, ContentStep, BuildStep {
    private String id;
    private String postID;
    private String content;
    @Override
     public Comment7 build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new Comment7(
          id,
          postID,
          content);
    }
    
    @Override
     public ContentStep postId(String postId) {
        Objects.requireNonNull(postId);
        this.postID = postId;
        return this;
    }
    
    @Override
     public BuildStep content(String content) {
        Objects.requireNonNull(content);
        this.content = content;
        return this;
    }
    
    /** 
     * @param id id
     * @return Current Builder instance, for fluent method chaining
     */
    public BuildStep id(String id) {
        this.id = id;
        return this;
    }
  }
  

  public final class CopyOfBuilder extends Builder {
    private CopyOfBuilder(String id, String postId, String content) {
      super.id(id);
      super.postId(postId)
        .content(content);
    }
    
    @Override
     public CopyOfBuilder postId(String postId) {
      return (CopyOfBuilder) super.postId(postId);
    }
    
    @Override
     public CopyOfBuilder content(String content) {
      return (CopyOfBuilder) super.content(content);
    }
  }
  
}
