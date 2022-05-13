package com.amplifyframework.testmodels.commentsblog;

import com.amplifyframework.core.model.annotations.HasMany;
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

/** This is an auto generated class representing the BlogOwnerWithCustomPK type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "BlogOwnerWithCustomPKS")
@Index(name = "undefined", fields = {"name","wea"})
public final class BlogOwnerWithCustomPK implements Model {
  public static final QueryField ID = field("BlogOwnerWithCustomPK", "id");
  public static final QueryField NAME = field("BlogOwnerWithCustomPK", "name");
  public static final QueryField WEA = field("BlogOwnerWithCustomPK", "wea");
  public static final QueryField CREATED_AT = field("BlogOwnerWithCustomPK", "createdAt");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String", isRequired = true) String name;
  private final @ModelField(targetType="OtherBlog") @HasMany(associatedWith = "owner", type = OtherBlog.class) List<OtherBlog> blogs = null;
  private final @ModelField(targetType="String", isRequired = true) String wea;
  private final @ModelField(targetType="AWSDateTime") Temporal.DateTime createdAt;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime updatedAt;
  public String getId() {
      return id;
  }
  
  public String getName() {
      return name;
  }
  
  public List<OtherBlog> getBlogs() {
      return blogs;
  }
  
  public String getWea() {
      return wea;
  }
  
  public Temporal.DateTime getCreatedAt() {
      return createdAt;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  private BlogOwnerWithCustomPK(String id, String name, String wea, Temporal.DateTime createdAt) {
    this.id = id;
    this.name = name;
    this.wea = wea;
    this.createdAt = createdAt;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      BlogOwnerWithCustomPK blogOwnerWithCustomPk = (BlogOwnerWithCustomPK) obj;
      return ObjectsCompat.equals(getId(), blogOwnerWithCustomPk.getId()) &&
              ObjectsCompat.equals(getName(), blogOwnerWithCustomPk.getName()) &&
              ObjectsCompat.equals(getWea(), blogOwnerWithCustomPk.getWea()) &&
              ObjectsCompat.equals(getCreatedAt(), blogOwnerWithCustomPk.getCreatedAt()) &&
              ObjectsCompat.equals(getUpdatedAt(), blogOwnerWithCustomPk.getUpdatedAt());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getName())
      .append(getWea())
      .append(getCreatedAt())
      .append(getUpdatedAt())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("BlogOwnerWithCustomPK {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("name=" + String.valueOf(getName()) + ", ")
      .append("wea=" + String.valueOf(getWea()) + ", ")
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
  public static BlogOwnerWithCustomPK justId(String id) {
    return new BlogOwnerWithCustomPK(
      id,
      null,
      null,
      null
    );
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(id,
      name,
      wea,
      createdAt);
  }
  public interface NameStep {
    WeaStep name(String name);
  }
  

  public interface WeaStep {
    BuildStep wea(String wea);
  }
  

  public interface BuildStep {
    BlogOwnerWithCustomPK build();
    BuildStep id(String id);
    BuildStep createdAt(Temporal.DateTime createdAt);
  }
  

  public static class Builder implements NameStep, WeaStep, BuildStep {
    private String id;
    private String name;
    private String wea;
    private Temporal.DateTime createdAt;
    @Override
     public BlogOwnerWithCustomPK build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new BlogOwnerWithCustomPK(
          id,
          name,
          wea,
          createdAt);
    }
    
    @Override
     public WeaStep name(String name) {
        Objects.requireNonNull(name);
        this.name = name;
        return this;
    }
    
    @Override
     public BuildStep wea(String wea) {
        Objects.requireNonNull(wea);
        this.wea = wea;
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
    private CopyOfBuilder(String id, String name, String wea, Temporal.DateTime createdAt) {
      super.id(id);
      super.name(name)
        .wea(wea)
        .createdAt(createdAt);
    }
    
    @Override
     public CopyOfBuilder name(String name) {
      return (CopyOfBuilder) super.name(name);
    }
    
    @Override
     public CopyOfBuilder wea(String wea) {
      return (CopyOfBuilder) super.wea(wea);
    }
    
    @Override
     public CopyOfBuilder createdAt(Temporal.DateTime createdAt) {
      return (CopyOfBuilder) super.createdAt(createdAt);
    }
  }
  
}
