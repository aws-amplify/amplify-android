package com.amplifyframework.testmodels.customprimarykey;

import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.annotations.HasOne;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.core.model.annotations.ModelField;
import com.amplifyframework.core.model.query.predicate.QueryField;
import com.amplifyframework.core.model.temporal.Temporal;

import java.util.UUID;

import static com.amplifyframework.core.model.query.predicate.QueryField.field;

/** This is an auto generated class representing the BlogWithDefaultHasOne type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "BlogWithDefaultHasOnes", type = Model.Type.USER, version = 1)
public final class BlogWithDefaultHasOne implements Model {
  public static final QueryField ID = field("BlogWithDefaultHasOne", "id");
  public static final QueryField TITLE = field("BlogWithDefaultHasOne", "title");
  public static final QueryField BLOG_WITH_DEFAULT_HAS_ONE_OWNER_ID = field("BlogWithDefaultHasOne", "blogWithDefaultHasOneOwnerId");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String") String title;
  private final @ModelField(targetType="User") @HasOne(associatedWith = "id", type = User.class) User owner = null;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime createdAt;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime updatedAt;
  private final @ModelField(targetType="ID") String blogWithDefaultHasOneOwnerId;
  public String resolveIdentifier() {
    return id;
  }
  
  public String getId() {
      return id;
  }
  
  public String getTitle() {
      return title;
  }
  
  public User getOwner() {
      return owner;
  }
  
  public Temporal.DateTime getCreatedAt() {
      return createdAt;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  public String getBlogWithDefaultHasOneOwnerId() {
      return blogWithDefaultHasOneOwnerId;
  }
  
  private BlogWithDefaultHasOne(String id, String title, String blogWithDefaultHasOneOwnerId) {
    this.id = id;
    this.title = title;
    this.blogWithDefaultHasOneOwnerId = blogWithDefaultHasOneOwnerId;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      BlogWithDefaultHasOne blogWithDefaultHasOne = (BlogWithDefaultHasOne) obj;
      return ObjectsCompat.equals(getId(), blogWithDefaultHasOne.getId()) &&
              ObjectsCompat.equals(getTitle(), blogWithDefaultHasOne.getTitle()) &&
              ObjectsCompat.equals(getCreatedAt(), blogWithDefaultHasOne.getCreatedAt()) &&
              ObjectsCompat.equals(getUpdatedAt(), blogWithDefaultHasOne.getUpdatedAt()) &&
              ObjectsCompat.equals(getBlogWithDefaultHasOneOwnerId(), blogWithDefaultHasOne.getBlogWithDefaultHasOneOwnerId());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getTitle())
      .append(getCreatedAt())
      .append(getUpdatedAt())
      .append(getBlogWithDefaultHasOneOwnerId())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("BlogWithDefaultHasOne {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("title=" + String.valueOf(getTitle()) + ", ")
      .append("createdAt=" + String.valueOf(getCreatedAt()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()) + ", ")
      .append("blogWithDefaultHasOneOwnerId=" + String.valueOf(getBlogWithDefaultHasOneOwnerId()))
      .append("}")
      .toString();
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
   */
  public static BlogWithDefaultHasOne justId(String id) {
    return new BlogWithDefaultHasOne(
      id,
      null,
      null
    );
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(id,
      title,
      blogWithDefaultHasOneOwnerId);
  }
  public interface BuildStep {
    BlogWithDefaultHasOne build();
    BuildStep id(String id);
    BuildStep title(String title);
    BuildStep blogWithDefaultHasOneOwnerId(String blogWithDefaultHasOneOwnerId);
  }
  

  public static class Builder implements BuildStep {
    private String id;
    private String title;
    private String blogWithDefaultHasOneOwnerId;
    @Override
     public BlogWithDefaultHasOne build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new BlogWithDefaultHasOne(
          id,
          title,
          blogWithDefaultHasOneOwnerId);
    }
    
    @Override
     public BuildStep title(String title) {
        this.title = title;
        return this;
    }
    
    @Override
     public BuildStep blogWithDefaultHasOneOwnerId(String blogWithDefaultHasOneOwnerId) {
        this.blogWithDefaultHasOneOwnerId = blogWithDefaultHasOneOwnerId;
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
    private CopyOfBuilder(String id, String title, String blogWithDefaultHasOneOwnerId) {
      super.id(id);
      super.title(title)
        .blogWithDefaultHasOneOwnerId(blogWithDefaultHasOneOwnerId);
    }
    
    @Override
     public CopyOfBuilder title(String title) {
      return (CopyOfBuilder) super.title(title);
    }
    
    @Override
     public CopyOfBuilder blogWithDefaultHasOneOwnerId(String blogWithDefaultHasOneOwnerId) {
      return (CopyOfBuilder) super.blogWithDefaultHasOneOwnerId(blogWithDefaultHasOneOwnerId);
    }
  }
  
}
