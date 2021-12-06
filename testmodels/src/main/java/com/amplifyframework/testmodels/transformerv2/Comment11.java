package com.amplifyframework.testmodels.transformerv2;

import com.amplifyframework.core.model.annotations.BelongsTo;
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

/** This is an auto generated class representing the Comment11 type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "Comment11s")
@Index(name = "byPost", fields = {"postID","content"})
public final class Comment11 implements Model {
  public static final QueryField ID = field("Comment11", "id");
  public static final QueryField CONTENT = field("Comment11", "content");
  public static final QueryField POST = field("Comment11", "postID");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String", isRequired = true) String content;
  private final @ModelField(targetType="Post11") @BelongsTo(targetName = "postID", type = Post11.class) Post11 post;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime createdAt;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime updatedAt;
  public String getId() {
      return id;
  }
  
  public String getContent() {
      return content;
  }
  
  public Post11 getPost() {
      return post;
  }
  
  public Temporal.DateTime getCreatedAt() {
      return createdAt;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  private Comment11(String id, String content, Post11 post) {
    this.id = id;
    this.content = content;
    this.post = post;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      Comment11 comment11 = (Comment11) obj;
      return ObjectsCompat.equals(getId(), comment11.getId()) &&
              ObjectsCompat.equals(getContent(), comment11.getContent()) &&
              ObjectsCompat.equals(getPost(), comment11.getPost()) &&
              ObjectsCompat.equals(getCreatedAt(), comment11.getCreatedAt()) &&
              ObjectsCompat.equals(getUpdatedAt(), comment11.getUpdatedAt());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getContent())
      .append(getPost())
      .append(getCreatedAt())
      .append(getUpdatedAt())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("Comment11 {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("content=" + String.valueOf(getContent()) + ", ")
      .append("post=" + String.valueOf(getPost()) + ", ")
      .append("createdAt=" + String.valueOf(getCreatedAt()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()))
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
  public static Comment11 justId(String id) {
    return new Comment11(
      id,
      null,
      null
    );
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(id,
      content,
      post);
  }
  public interface ContentStep {
    BuildStep content(String content);
  }
  

  public interface BuildStep {
    Comment11 build();
    BuildStep id(String id);
    BuildStep post(Post11 post);
  }
  

  public static class Builder implements ContentStep, BuildStep {
    private String id;
    private String content;
    private Post11 post;
    @Override
     public Comment11 build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new Comment11(
          id,
          content,
          post);
    }
    
    @Override
     public BuildStep content(String content) {
        Objects.requireNonNull(content);
        this.content = content;
        return this;
    }
    
    @Override
     public BuildStep post(Post11 post) {
        this.post = post;
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
    private CopyOfBuilder(String id, String content, Post11 post) {
      super.id(id);
      super.content(content)
        .post(post);
    }
    
    @Override
     public CopyOfBuilder content(String content) {
      return (CopyOfBuilder) super.content(content);
    }
    
    @Override
     public CopyOfBuilder post(Post11 post) {
      return (CopyOfBuilder) super.post(post);
    }
  }
  
}
