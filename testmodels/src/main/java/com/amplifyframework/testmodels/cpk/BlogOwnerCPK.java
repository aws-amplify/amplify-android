package com.amplifyframework.testmodels.cpk;

import static com.amplifyframework.core.model.query.predicate.QueryField.field;

import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelIdentifier;
import com.amplifyframework.core.model.annotations.HasMany;
import com.amplifyframework.core.model.annotations.Index;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.core.model.annotations.ModelField;
import com.amplifyframework.core.model.query.predicate.QueryField;
import com.amplifyframework.core.model.temporal.Temporal;

import java.util.List;
import java.util.Objects;

/** This is an auto generated class representing the BlogOwnerCPK type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "BlogOwnerCPKS", type = Model.Type.USER, version = 1)
@Index(name = "undefined", fields = {"id","name"})
public final class BlogOwnerCPK implements Model {
  public static final QueryField ID = field("BlogOwnerCPK", "id");
  public static final QueryField NAME = field("BlogOwnerCPK", "name");
  public static final QueryField CREATED_AT = field("BlogOwnerCPK", "createdAt");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String", isRequired = true) String name;
  private final @ModelField(targetType="BlogCPK") @HasMany(associatedWith = "owner", type = BlogCPK.class) List<BlogCPK> blogs = null;
  private final @ModelField(targetType="PostCPK") @HasMany(associatedWith = "blogOwner", type = PostCPK.class) List<PostCPK> posts = null;
  private final @ModelField(targetType="AWSDateTime") Temporal.DateTime createdAt;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime updatedAt;
  private BlogOwnerCPKIdentifier blogOwnerCPKIdentifier;
  /** @deprecated This API is internal to Amplify and should not be used. */
  @Deprecated
   public BlogOwnerCPKIdentifier resolveIdentifier() {
    if (blogOwnerCPKIdentifier == null) {
      this.blogOwnerCPKIdentifier = new BlogOwnerCPKIdentifier(id, name);
    }
    return blogOwnerCPKIdentifier;
  }
  
  public String getId() {
      return id;
  }
  
  public String getName() {
      return name;
  }
  
  public List<BlogCPK> getBlogs() {
      return blogs;
  }
  
  public List<PostCPK> getPosts() {
      return posts;
  }
  
  public Temporal.DateTime getCreatedAt() {
      return createdAt;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  private BlogOwnerCPK(String id, String name, Temporal.DateTime createdAt) {
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
      BlogOwnerCPK blogOwnerCpk = (BlogOwnerCPK) obj;
      return ObjectsCompat.equals(getId(), blogOwnerCpk.getId()) &&
              ObjectsCompat.equals(getName(), blogOwnerCpk.getName()) &&
              ObjectsCompat.equals(getCreatedAt(), blogOwnerCpk.getCreatedAt()) &&
              ObjectsCompat.equals(getUpdatedAt(), blogOwnerCpk.getUpdatedAt());
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
      .append("BlogOwnerCPK {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("name=" + String.valueOf(getName()) + ", ")
      .append("createdAt=" + String.valueOf(getCreatedAt()) + ", ")
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
      createdAt);
  }
  public interface IdStep {
    NameStep id(String id);
  }
  

  public interface NameStep {
    BuildStep name(String name);
  }
  

  public interface BuildStep {
    BlogOwnerCPK build();
    BuildStep createdAt(Temporal.DateTime createdAt);
  }
  

  public static class Builder implements IdStep, NameStep, BuildStep {
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
     public BlogOwnerCPK build() {
        
        return new BlogOwnerCPK(
          id,
          name,
          createdAt);
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
  }
  

  public final class CopyOfBuilder extends Builder {
    private CopyOfBuilder(String id, String name, Temporal.DateTime createdAt) {
      super(id, name, createdAt);
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
  }
  

  public static class BlogOwnerCPKIdentifier extends ModelIdentifier<BlogOwnerCPK> {
    private static final long serialVersionUID = 1L;
    public BlogOwnerCPKIdentifier(String id, String name) {
      super(id, name);
    }
  }
  
}
