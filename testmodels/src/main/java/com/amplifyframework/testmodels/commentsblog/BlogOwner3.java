package com.amplifyframework.testmodels.commentsblog;

import static com.amplifyframework.core.model.query.predicate.QueryField.field;

import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelIdentifier;
import com.amplifyframework.core.model.annotations.HasMany;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.core.model.annotations.ModelField;
import com.amplifyframework.core.model.query.predicate.QueryField;
import com.amplifyframework.core.model.temporal.Temporal;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** This is an auto generated class representing the BlogOwner3 type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "BlogOwner3s", type = Model.Type.USER, version = 1)
public final class BlogOwner3 implements Model {
  public static final QueryField ID = field("BlogOwner3", "id");
  public static final QueryField NAME = field("BlogOwner3", "name");
  public static final QueryField CREATED_AT = field("BlogOwner3", "createdAt");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String", isRequired = true) String name;
  private final @ModelField(targetType="Blog3") @HasMany(associatedWith = "owner", type = Blog3.class) List<Blog3> blogs = null;
  private final @ModelField(targetType="Post2") @HasMany(associatedWith = "blogOwner", type = Post2.class) List<Post2> posts = null;
  private final @ModelField(targetType="AWSDateTime") Temporal.DateTime createdAt;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime updatedAt;
  /** @deprecated This API is internal to Amplify and should not be used. */
  @Deprecated
   public String resolveIdentifier() {
    return id;
  }
  
  public String getId() {
      return id;
  }
  
  public String getName() {
      return name;
  }
  
  public List<Blog3> getBlogs() {
      return blogs;
  }
  
  public List<Post2> getPosts() {
      return posts;
  }
  
  public Temporal.DateTime getCreatedAt() {
      return createdAt;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  private BlogOwner3(String id, String name, Temporal.DateTime createdAt) {
    this.id = id;
    this.name = name;
    this.createdAt = createdAt;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      BlogOwner3 blogOwner3 = (BlogOwner3) obj;
      return ObjectsCompat.equals(getId(), blogOwner3.getId()) &&
              ObjectsCompat.equals(getName(), blogOwner3.getName()) &&
              ObjectsCompat.equals(getCreatedAt(), blogOwner3.getCreatedAt()) &&
              ObjectsCompat.equals(getUpdatedAt(), blogOwner3.getUpdatedAt());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getName())
      .append(getCreatedAt())
      .append(getUpdatedAt())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("BlogOwner3 {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("name=" + String.valueOf(getName()) + ", ")
      .append("createdAt=" + String.valueOf(getCreatedAt()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()))
      .append("}")
      .toString();
  }
  
  public static NameStep builder() {
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
  public static BlogOwner3 justId(String id) {
    return new BlogOwner3(
      id,
      null,
      null
    );
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(id,
      name,
      createdAt);
  }
  public interface NameStep {
    BuildStep name(String name);
  }
  

  public interface BuildStep {
    BlogOwner3 build();
    BuildStep id(String id);
    BuildStep createdAt(Temporal.DateTime createdAt);
  }
  

  public static class Builder implements NameStep, BuildStep {
    private String id;
    private String name;
    private Temporal.DateTime createdAt;
    public Builder() {
      
    }
    
    private Builder(String id, String name, Temporal.DateTime createdAt) {
      this.id = id;
      this.name = name;
      this.createdAt = createdAt;
    }
    
    @Override
     public BlogOwner3 build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new BlogOwner3(
          id,
          name,
          createdAt);
    }
    
    @Override
     public BuildStep name(String name) {
        Objects.requireNonNull(name);
        this.name = name;
        return this;
    }
    
    @Override
     public BuildStep createdAt(Temporal.DateTime createdAt) {
        this.createdAt = createdAt;
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
    private CopyOfBuilder(String id, String name, Temporal.DateTime createdAt) {
      super(id, name, createdAt);
      Objects.requireNonNull(name);
    }
    
    @Override
     public CopyOfBuilder name(String name) {
      return (CopyOfBuilder) super.name(name);
    }
    
    @Override
     public CopyOfBuilder createdAt(Temporal.DateTime createdAt) {
      return (CopyOfBuilder) super.createdAt(createdAt);
    }
  }
  

  public static class BlogOwner3Identifier extends ModelIdentifier<BlogOwner3> {
    private static final long serialVersionUID = 1L;
    public BlogOwner3Identifier(String id) {
      super(id);
    }
  }
  
}
