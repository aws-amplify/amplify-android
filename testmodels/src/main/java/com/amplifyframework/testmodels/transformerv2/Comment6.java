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

/** This is an auto generated class representing the Comment6 type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "Comment6s")
public final class Comment6 implements Model {
  public static final QueryField ID = field("Comment6", "id");
  public static final QueryField CONTENT = field("Comment6", "content");
  public static final QueryField POST6_COMMENTS_ID = field("Comment6", "post6CommentsId");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String", isRequired = true) String content;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime createdAt;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime updatedAt;
  private final @ModelField(targetType="ID") String post6CommentsId;
  public String getId() {
      return id;
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
  
  public String getPost6CommentsId() {
      return post6CommentsId;
  }
  
  private Comment6(String id, String content, String post6CommentsId) {
    this.id = id;
    this.content = content;
    this.post6CommentsId = post6CommentsId;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      Comment6 comment6 = (Comment6) obj;
      return ObjectsCompat.equals(getId(), comment6.getId()) &&
              ObjectsCompat.equals(getContent(), comment6.getContent()) &&
              ObjectsCompat.equals(getCreatedAt(), comment6.getCreatedAt()) &&
              ObjectsCompat.equals(getUpdatedAt(), comment6.getUpdatedAt()) &&
              ObjectsCompat.equals(getPost6CommentsId(), comment6.getPost6CommentsId());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getContent())
      .append(getCreatedAt())
      .append(getUpdatedAt())
      .append(getPost6CommentsId())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("Comment6 {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("content=" + String.valueOf(getContent()) + ", ")
      .append("createdAt=" + String.valueOf(getCreatedAt()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()) + ", ")
      .append("post6CommentsId=" + String.valueOf(getPost6CommentsId()))
      .append("}")
      .toString();
  }
  
  public static ContentStep builder() {
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
  public static Comment6 justId(String id) {
    return new Comment6(
      id,
      null,
      null
    );
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(id,
      content,
      post6CommentsId);
  }
  public interface ContentStep {
    BuildStep content(String content);
  }
  

  public interface BuildStep {
    Comment6 build();
    BuildStep id(String id);
    BuildStep post6CommentsId(String post6CommentsId);
  }
  

  public static class Builder implements ContentStep, BuildStep {
    private String id;
    private String content;
    private String post6CommentsId;
    @Override
     public Comment6 build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new Comment6(
          id,
          content,
          post6CommentsId);
    }
    
    @Override
     public BuildStep content(String content) {
        Objects.requireNonNull(content);
        this.content = content;
        return this;
    }
    
    @Override
     public BuildStep post6CommentsId(String post6CommentsId) {
        this.post6CommentsId = post6CommentsId;
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
    private CopyOfBuilder(String id, String content, String post6CommentsId) {
      super.id(id);
      super.content(content)
        .post6CommentsId(post6CommentsId);
    }
    
    @Override
     public CopyOfBuilder content(String content) {
      return (CopyOfBuilder) super.content(content);
    }
    
    @Override
     public CopyOfBuilder post6CommentsId(String post6CommentsId) {
      return (CopyOfBuilder) super.post6CommentsId(post6CommentsId);
    }
  }
  
}
