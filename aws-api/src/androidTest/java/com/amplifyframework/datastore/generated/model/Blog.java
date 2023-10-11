package com.amplifyframework.datastore.generated.model;

import static com.amplifyframework.core.model.query.predicate.QueryField.field;

import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelIdentifier;
import com.amplifyframework.core.model.ModelList;
import com.amplifyframework.core.model.annotations.HasMany;
import com.amplifyframework.core.model.annotations.Index;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.core.model.annotations.ModelField;
import com.amplifyframework.core.model.query.predicate.QueryField;
import com.amplifyframework.core.model.temporal.Temporal;

import java.util.Objects;

/** This is an auto generated class representing the Blog type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "Blogs", type = Model.Type.USER, version = 1, hasLazySupport = true)
@Index(name = "undefined", fields = {"blogId"})
public final class Blog implements Model {
  public static final BlogPath rootPath = new BlogPath("root", false, null);
  public static final QueryField BLOG_ID = field("Blog", "blogId");
  public static final QueryField NAME = field("Blog", "name");
  private final @ModelField(targetType="String", isRequired = true) String blogId;
  private final @ModelField(targetType="String", isRequired = true) String name;
  private final @ModelField(targetType="Post", isRequired = true) @HasMany(associatedWith = "blog", type = Post.class) ModelList<Post> posts = null;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime createdAt;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime updatedAt;
  /** @deprecated This API is internal to Amplify and should not be used. */
  @Deprecated
   public String resolveIdentifier() {
    return blogId;
  }
  
  public String getBlogId() {
      return blogId;
  }
  
  public String getName() {
      return name;
  }
  
  public ModelList<Post> getPosts() {
      return posts;
  }
  
  public Temporal.DateTime getCreatedAt() {
      return createdAt;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  private Blog(String blogId, String name) {
    this.blogId = blogId;
    this.name = name;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      Blog blog = (Blog) obj;
      return ObjectsCompat.equals(getBlogId(), blog.getBlogId()) &&
              ObjectsCompat.equals(getName(), blog.getName()) &&
              ObjectsCompat.equals(getCreatedAt(), blog.getCreatedAt()) &&
              ObjectsCompat.equals(getUpdatedAt(), blog.getUpdatedAt());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getBlogId())
      .append(getName())
      .append(getCreatedAt())
      .append(getUpdatedAt())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("Blog {")
      .append("blogId=" + String.valueOf(getBlogId()) + ", ")
      .append("name=" + String.valueOf(getName()) + ", ")
      .append("createdAt=" + String.valueOf(getCreatedAt()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()))
      .append("}")
      .toString();
  }
  
  public static BlogIdStep builder() {
      return new Builder();
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(blogId,
      name);
  }
  public interface BlogIdStep {
    NameStep blogId(String blogId);
  }
  

  public interface NameStep {
    BuildStep name(String name);
  }
  

  public interface BuildStep {
    Blog build();
  }
  

  public static class Builder implements BlogIdStep, NameStep, BuildStep {
    private String blogId;
    private String name;
    public Builder() {
      
    }
    
    private Builder(String blogId, String name) {
      this.blogId = blogId;
      this.name = name;
    }
    
    @Override
     public Blog build() {
        
        return new Blog(
          blogId,
          name);
    }
    
    @Override
     public NameStep blogId(String blogId) {
        Objects.requireNonNull(blogId);
        this.blogId = blogId;
        return this;
    }
    
    @Override
     public BuildStep name(String name) {
        Objects.requireNonNull(name);
        this.name = name;
        return this;
    }
  }
  

  public final class CopyOfBuilder extends Builder {
    private CopyOfBuilder(String blogId, String name) {
      super(blogId, name);
      Objects.requireNonNull(blogId);
      Objects.requireNonNull(name);
    }
    
    @Override
     public CopyOfBuilder blogId(String blogId) {
      return (CopyOfBuilder) super.blogId(blogId);
    }
    
    @Override
     public CopyOfBuilder name(String name) {
      return (CopyOfBuilder) super.name(name);
    }
  }
  

  public static class BlogIdentifier extends ModelIdentifier<Blog> {
    private static final long serialVersionUID = 1L;
    public BlogIdentifier(String blogId) {
      super(blogId);
    }
  }
  
}
