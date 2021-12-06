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

/** This is an auto generated class representing the PostTags type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "PostTags")
public final class PostTags implements Model {
  public static final QueryField ID = field("PostTags", "id");
  public static final QueryField POST8 = field("PostTags", "post8ID");
  public static final QueryField TAG8 = field("PostTags", "tag8ID");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="Post8", isRequired = true) @BelongsTo(targetName = "post8ID", type = Post8.class) Post8 post8;
  private final @ModelField(targetType="Tag8", isRequired = true) @BelongsTo(targetName = "tag8ID", type = Tag8.class) Tag8 tag8;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime createdAt;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime updatedAt;
  public String getId() {
      return id;
  }
  
  public Post8 getPost8() {
      return post8;
  }
  
  public Tag8 getTag8() {
      return tag8;
  }
  
  public Temporal.DateTime getCreatedAt() {
      return createdAt;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  private PostTags(String id, Post8 post8, Tag8 tag8) {
    this.id = id;
    this.post8 = post8;
    this.tag8 = tag8;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      PostTags postTags = (PostTags) obj;
      return ObjectsCompat.equals(getId(), postTags.getId()) &&
              ObjectsCompat.equals(getPost8(), postTags.getPost8()) &&
              ObjectsCompat.equals(getTag8(), postTags.getTag8()) &&
              ObjectsCompat.equals(getCreatedAt(), postTags.getCreatedAt()) &&
              ObjectsCompat.equals(getUpdatedAt(), postTags.getUpdatedAt());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getPost8())
      .append(getTag8())
      .append(getCreatedAt())
      .append(getUpdatedAt())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("PostTags {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("post8=" + String.valueOf(getPost8()) + ", ")
      .append("tag8=" + String.valueOf(getTag8()) + ", ")
      .append("createdAt=" + String.valueOf(getCreatedAt()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()))
      .append("}")
      .toString();
  }
  
  public static Post8Step builder() {
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
  public static PostTags justId(String id) {
    return new PostTags(
      id,
      null,
      null
    );
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(id,
      post8,
      tag8);
  }
  public interface Post8Step {
    Tag8Step post8(Post8 post8);
  }
  

  public interface Tag8Step {
    BuildStep tag8(Tag8 tag8);
  }
  

  public interface BuildStep {
    PostTags build();
    BuildStep id(String id);
  }
  

  public static class Builder implements Post8Step, Tag8Step, BuildStep {
    private String id;
    private Post8 post8;
    private Tag8 tag8;
    @Override
     public PostTags build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new PostTags(
          id,
          post8,
          tag8);
    }
    
    @Override
     public Tag8Step post8(Post8 post8) {
        Objects.requireNonNull(post8);
        this.post8 = post8;
        return this;
    }
    
    @Override
     public BuildStep tag8(Tag8 tag8) {
        Objects.requireNonNull(tag8);
        this.tag8 = tag8;
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
    private CopyOfBuilder(String id, Post8 post8, Tag8 tag8) {
      super.id(id);
      super.post8(post8)
        .tag8(tag8);
    }
    
    @Override
     public CopyOfBuilder post8(Post8 post8) {
      return (CopyOfBuilder) super.post8(post8);
    }
    
    @Override
     public CopyOfBuilder tag8(Tag8 tag8) {
      return (CopyOfBuilder) super.tag8(tag8);
    }
  }
  
}
