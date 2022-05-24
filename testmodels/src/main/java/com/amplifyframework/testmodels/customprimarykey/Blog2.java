package com.amplifyframework.testmodels.customprimarykey;

import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.annotations.BelongsTo;
import com.amplifyframework.core.model.annotations.HasMany;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.core.model.annotations.ModelField;
import com.amplifyframework.core.model.query.predicate.QueryField;
import com.amplifyframework.core.model.temporal.Temporal;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.amplifyframework.core.model.query.predicate.QueryField.field;

/** This is an auto generated class representing the Blog type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "Blogs", version = "V2")
public final class Blog2 implements Model {
  public static final QueryField ID = field("Blog", "id");
  public static final QueryField NAME = field("Blog", "name");
  public static final QueryField OWNER = field("Blog", "blogOwnerId");
  public static final QueryField CREATED_AT = field("Blog", "createdAt");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String", isRequired = true) String name;
  private final @ModelField(targetType="Post") @HasMany(associatedWith = "blog", type = Post.class) List<Post> posts = null;
  private final @ModelField(targetType="BlogOwnerWithCustomPK", isRequired = true) @BelongsTo(targetName= "", targetNames = {"blogOwnerName","blogOwnerWea" },
          type = BlogOwnerWithCustomPK.class)
  BlogOwnerWithCustomPK owner;
  private final @ModelField(targetType="AWSDateTime") Temporal.DateTime createdAt;


  public String getId() {
      return id;
  }

  public String getName() {
      return name;
  }
  
  public List<Post> getPosts() {
      return posts;
  }
  
  public BlogOwnerWithCustomPK getOwner() {
      return owner;
  }
  
  public Temporal.DateTime getCreatedAt() {
      return createdAt;
  }
  
  private Blog2(String id, String name, BlogOwnerWithCustomPK owner, Temporal.DateTime createdAt) {
    this.id = id;
    this.name = name;
    this.owner = owner;
    this.createdAt = createdAt;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
          Blog2 blog = (Blog2) obj;
      return ObjectsCompat.equals(getId(), blog.getId()) &&
              ObjectsCompat.equals(getName(), blog.getName()) &&
              ObjectsCompat.equals(getOwner(), blog.getOwner()) &&
              ObjectsCompat.equals(getCreatedAt(), blog.getCreatedAt());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getName())
      .append(getOwner())
      .append(getCreatedAt())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("Blog {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("name=" + String.valueOf(getName()) + ", ")
      .append("owner=" + String.valueOf(getOwner()) + ", ")
      .append("createdAt=" + String.valueOf(getCreatedAt()))
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
   * @throws IllegalArgumentException Checks that ID is in the proper format
   */
  public static Blog2 justId(String id) {
    try {
      UUID.fromString(id); // Check that ID is in the UUID format - if not an exception is thrown
    } catch (Exception exception) {
      throw new IllegalArgumentException(
              "Model IDs must be unique in the format of UUID. This method is for creating instances " +
              "of an existing object with only its ID field for sending as a mutation parameter. When " +
              "creating a new object, use the standard builder method and leave the ID field blank."
      );
    }
    return new Blog2(
      id,
      null,
      null,
      null
    );
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(id,
      name,
      owner,
      createdAt);
  }
  public interface NameStep {
    OwnerStep name(String name);
  }
  

  public interface OwnerStep {
    BuildStep owner(BlogOwnerWithCustomPK owner);
  }
  

  public interface BuildStep {
    Blog2 build();
    BuildStep id(String id) throws IllegalArgumentException;
    BuildStep createdAt(Temporal.DateTime createdAt);
  }
  

  public static class Builder implements NameStep, OwnerStep, BuildStep {
    private String id;
    private String name;
    private BlogOwnerWithCustomPK owner;
    private Temporal.DateTime createdAt;
    @Override
     public Blog2 build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new Blog2(
          id,
          name,
          owner,
          createdAt);
    }
    
    @Override
     public OwnerStep name(String name) {
        Objects.requireNonNull(name);
        this.name = name;
        return this;
    }
    
    @Override
     public BuildStep owner(BlogOwnerWithCustomPK owner) {
        Objects.requireNonNull(owner);
        this.owner = owner;
        return this;
    }
    
    @Override
     public BuildStep createdAt(Temporal.DateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }
    
    /** 
     * WARNING: Do not set ID when creating a new object. Leave this blank and one will be auto generated for you.
     * This should only be set when referring to an already existing object.
     * @param id id
     * @return Current Builder instance, for fluent method chaining
     * @throws IllegalArgumentException Checks that ID is in the proper format
     */
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
    private CopyOfBuilder(String id, String name, BlogOwnerWithCustomPK owner, Temporal.DateTime createdAt) {
      super.id(id);
      super.name(name)
        .owner(owner)
        .createdAt(createdAt);
    }
    
    @Override
     public CopyOfBuilder name(String name) {
      return (CopyOfBuilder) super.name(name);
    }
    
    @Override
     public CopyOfBuilder owner(BlogOwnerWithCustomPK owner) {
      return (CopyOfBuilder) super.owner(owner);
    }
    
    @Override
     public CopyOfBuilder createdAt(Temporal.DateTime createdAt) {
      return (CopyOfBuilder) super.createdAt(createdAt);
    }
  }
  
}
