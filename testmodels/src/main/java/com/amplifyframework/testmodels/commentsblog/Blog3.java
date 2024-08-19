package com.amplifyframework.testmodels.commentsblog;

import static com.amplifyframework.core.model.query.predicate.QueryField.field;

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
import java.util.UUID;

/** This is an auto generated class representing the Blog3 type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "Blog3s", type = Model.Type.USER, version = 1)
@Index(name = "byBlogOwner3", fields = {"blogOwnerID"})
public final class Blog3 implements Model {
  public static final QueryField ID = field("Blog3", "id");
  public static final QueryField NAME = field("Blog3", "name");
  public static final QueryField CREATED_AT = field("Blog3", "createdAt");
  public static final QueryField OWNER = field("Blog3", "blogOwnerID");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String", isRequired = true) String name;
  private final @ModelField(targetType="AWSDateTime") Temporal.DateTime createdAt;
  private final @ModelField(targetType="Post2") @HasMany(associatedWith = "blog", type = Post2.class) List<Post2> posts = null;
  private final @ModelField(targetType="BlogOwner3") @BelongsTo(targetName = "blogOwnerID", targetNames = {"blogOwnerID"}, type = BlogOwner3.class) BlogOwner3 owner;
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
  
  public Temporal.DateTime getCreatedAt() {
      return createdAt;
  }
  
  public List<Post2> getPosts() {
      return posts;
  }
  
  public BlogOwner3 getOwner() {
      return owner;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  private Blog3(String id, String name, Temporal.DateTime createdAt, BlogOwner3 owner) {
    this.id = id;
    this.name = name;
    this.createdAt = createdAt;
    this.owner = owner;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      Blog3 blog3 = (Blog3) obj;
      return ObjectsCompat.equals(getId(), blog3.getId()) &&
              ObjectsCompat.equals(getName(), blog3.getName()) &&
              ObjectsCompat.equals(getCreatedAt(), blog3.getCreatedAt()) &&
              ObjectsCompat.equals(getOwner(), blog3.getOwner()) &&
              ObjectsCompat.equals(getUpdatedAt(), blog3.getUpdatedAt());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getName())
      .append(getCreatedAt())
      .append(getOwner())
      .append(getUpdatedAt())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("Blog3 {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("name=" + String.valueOf(getName()) + ", ")
      .append("createdAt=" + String.valueOf(getCreatedAt()) + ", ")
      .append("owner=" + String.valueOf(getOwner()) + ", ")
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
  public static Blog3 justId(String id) {
    return new Blog3(
      id,
      null,
      null,
      null
    );
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(id,
      name,
      createdAt,
      owner);
  }
  public interface NameStep {
    BuildStep name(String name);
  }
  

  public interface BuildStep {
    Blog3 build();
    BuildStep id(String id);
    BuildStep createdAt(Temporal.DateTime createdAt);
    BuildStep owner(BlogOwner3 owner);
  }
  

  public static class Builder implements NameStep, BuildStep {
    private String id;
    private String name;
    private Temporal.DateTime createdAt;
    private BlogOwner3 owner;
    public Builder() {
      
    }
    
    private Builder(String id, String name, Temporal.DateTime createdAt, BlogOwner3 owner) {
      this.id = id;
      this.name = name;
      this.createdAt = createdAt;
      this.owner = owner;
    }
    
    @Override
     public Blog3 build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new Blog3(
          id,
          name,
          createdAt,
          owner);
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
    
    @Override
     public BuildStep owner(BlogOwner3 owner) {
        this.owner = owner;
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
    private CopyOfBuilder(String id, String name, Temporal.DateTime createdAt, BlogOwner3 owner) {
      super(id, name, createdAt, owner);
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
    
    @Override
     public CopyOfBuilder owner(BlogOwner3 owner) {
      return (CopyOfBuilder) super.owner(owner);
    }
  }
  

  public static class Blog3Identifier extends ModelIdentifier<Blog3> {
    private static final long serialVersionUID = 1L;
    public Blog3Identifier(String id) {
      super(id);
    }
  }
  
}
