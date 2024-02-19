package com.amplifyframework.testmodels.cpk;

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

/** This is an auto generated class representing the BlogCPK type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "BlogCPKS", type = Model.Type.USER, version = 1)
@Index(name = "undefined", fields = {"id","name"})
@Index(name = "byBlogOwner3", fields = {"blogOwnerID"})
public final class BlogCPK implements Model {
  public static final QueryField ID = field("BlogCPK", "id");
  public static final QueryField NAME = field("BlogCPK", "name");
  public static final QueryField CREATED_AT = field("BlogCPK", "createdAt");
  public static final QueryField OWNER = field("BlogCPK", "blogOwnerID");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String", isRequired = true) String name;
  private final @ModelField(targetType="AWSDateTime") Temporal.DateTime createdAt;
  private final @ModelField(targetType="PostCPK") @HasMany(associatedWith = "blog", type = PostCPK.class) List<PostCPK> posts = null;
  private final @ModelField(targetType="BlogOwnerCPK") @BelongsTo(targetName = "blogOwnerID", targetNames = {"blogOwnerID"}, type = BlogOwnerCPK.class) BlogOwnerCPK owner;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime updatedAt;
  private BlogCPKIdentifier blogCPKIdentifier;
  /** @deprecated This API is internal to Amplify and should not be used. */
  @Deprecated
   public BlogCPKIdentifier resolveIdentifier() {
    if (blogCPKIdentifier == null) {
      this.blogCPKIdentifier = new BlogCPKIdentifier(id, name);
    }
    return blogCPKIdentifier;
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
  
  public List<PostCPK> getPosts() {
      return posts;
  }
  
  public BlogOwnerCPK getOwner() {
      return owner;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  private BlogCPK(String id, String name, Temporal.DateTime createdAt, BlogOwnerCPK owner) {
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
      BlogCPK blogCpk = (BlogCPK) obj;
      return ObjectsCompat.equals(getId(), blogCpk.getId()) &&
              ObjectsCompat.equals(getName(), blogCpk.getName()) &&
              ObjectsCompat.equals(getCreatedAt(), blogCpk.getCreatedAt()) &&
              ObjectsCompat.equals(getOwner(), blogCpk.getOwner()) &&
              ObjectsCompat.equals(getUpdatedAt(), blogCpk.getUpdatedAt());
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
      .append("BlogCPK {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("name=" + String.valueOf(getName()) + ", ")
      .append("createdAt=" + String.valueOf(getCreatedAt()) + ", ")
      .append("owner=" + String.valueOf(getOwner()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()))
      .append("}")
      .toString();
  }
  
  public static IdStep builder() {
      return new Builder();
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(id,
      name,
      createdAt,
      owner);
  }
  public interface IdStep {
    NameStep id(String id);
  }
  

  public interface NameStep {
    BuildStep name(String name);
  }
  

  public interface BuildStep {
    BlogCPK build();
    BuildStep createdAt(Temporal.DateTime createdAt);
    BuildStep owner(BlogOwnerCPK owner);
  }
  

  public static class Builder implements IdStep, NameStep, BuildStep {
    private String id;
    private String name;
    private Temporal.DateTime createdAt;
    private BlogOwnerCPK owner;
    public Builder() {
      
    }
    
    private Builder(String id, String name, Temporal.DateTime createdAt, BlogOwnerCPK owner) {
      this.id = id;
      this.name = name;
      this.createdAt = createdAt;
      this.owner = owner;
    }
    
    @Override
     public BlogCPK build() {
        
        return new BlogCPK(
          id,
          name,
          createdAt,
          owner);
    }
    
    @Override
     public NameStep id(String id) {
        Objects.requireNonNull(id);
        this.id = id;
        return this;
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
     public BuildStep owner(BlogOwnerCPK owner) {
        this.owner = owner;
        return this;
    }
  }
  

  public final class CopyOfBuilder extends Builder {
    private CopyOfBuilder(String id, String name, Temporal.DateTime createdAt, BlogOwnerCPK owner) {
      super(id, name, createdAt, owner);
      Objects.requireNonNull(id);
      Objects.requireNonNull(name);
    }
    
    @Override
     public CopyOfBuilder id(String id) {
      return (CopyOfBuilder) super.id(id);
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
     public CopyOfBuilder owner(BlogOwnerCPK owner) {
      return (CopyOfBuilder) super.owner(owner);
    }
  }
  

  public static class BlogCPKIdentifier extends ModelIdentifier<BlogCPK> {
    private static final long serialVersionUID = 1L;
    public BlogCPKIdentifier(String id, String name) {
      super(id, name);
    }
  }
  
}
