package com.amplifyframework.testmodels.cpk;

import static com.amplifyframework.core.model.query.predicate.QueryField.field;

import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelIdentifier;
import com.amplifyframework.core.model.annotations.BelongsTo;
import com.amplifyframework.core.model.annotations.Index;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.core.model.annotations.ModelField;
import com.amplifyframework.core.model.query.predicate.QueryField;
import com.amplifyframework.core.model.temporal.Temporal;

import java.util.Objects;

/** This is an auto generated class representing the PostCPK type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "PostCPKS", type = Model.Type.USER, version = 1)
@Index(name = "undefined", fields = {"id","title"})
@Index(name = "byBlog3", fields = {"blogID"})
@Index(name = "byBlogOwner3", fields = {"blogOwnerID"})
public final class PostCPK implements Model {
  public static final QueryField ID = field("PostCPK", "id");
  public static final QueryField TITLE = field("PostCPK", "title");
  public static final QueryField RATING = field("PostCPK", "rating");
  public static final QueryField CREATED_AT = field("PostCPK", "createdAt");
  public static final QueryField BLOG = field("PostCPK", "blogID");
  public static final QueryField BLOG_OWNER = field("PostCPK", "blogOwnerID");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String", isRequired = true) String title;
  private final @ModelField(targetType="Int", isRequired = true) Integer rating;
  private final @ModelField(targetType="AWSDateTime") Temporal.DateTime createdAt;
  private final @ModelField(targetType="BlogCPK") @BelongsTo(targetName = "blogID", targetNames = {"blogID"}, type = BlogCPK.class) BlogCPK blog;
  private final @ModelField(targetType="BlogOwnerCPK") @BelongsTo(targetName = "blogOwnerID", targetNames = {"blogOwnerID"}, type = BlogOwnerCPK.class) BlogOwnerCPK blogOwner;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime updatedAt;
  private PostCPKIdentifier postCPKIdentifier;
  /** @deprecated This API is internal to Amplify and should not be used. */
  @Deprecated
   public PostCPKIdentifier resolveIdentifier() {
    if (postCPKIdentifier == null) {
      this.postCPKIdentifier = new PostCPKIdentifier(id, title);
    }
    return postCPKIdentifier;
  }
  
  public String getId() {
      return id;
  }
  
  public String getTitle() {
      return title;
  }
  
  public Integer getRating() {
      return rating;
  }
  
  public Temporal.DateTime getCreatedAt() {
      return createdAt;
  }
  
  public BlogCPK getBlog() {
      return blog;
  }
  
  public BlogOwnerCPK getBlogOwner() {
      return blogOwner;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  private PostCPK(String id, String title, Integer rating, Temporal.DateTime createdAt, BlogCPK blog, BlogOwnerCPK blogOwner) {
    this.id = id;
    this.title = title;
    this.rating = rating;
    this.createdAt = createdAt;
    this.blog = blog;
    this.blogOwner = blogOwner;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      PostCPK postCpk = (PostCPK) obj;
      return ObjectsCompat.equals(getId(), postCpk.getId()) &&
              ObjectsCompat.equals(getTitle(), postCpk.getTitle()) &&
              ObjectsCompat.equals(getRating(), postCpk.getRating()) &&
              ObjectsCompat.equals(getCreatedAt(), postCpk.getCreatedAt()) &&
              ObjectsCompat.equals(getBlog(), postCpk.getBlog()) &&
              ObjectsCompat.equals(getBlogOwner(), postCpk.getBlogOwner()) &&
              ObjectsCompat.equals(getUpdatedAt(), postCpk.getUpdatedAt());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getTitle())
      .append(getRating())
      .append(getCreatedAt())
      .append(getBlog())
      .append(getBlogOwner())
      .append(getUpdatedAt())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("PostCPK {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("title=" + String.valueOf(getTitle()) + ", ")
      .append("rating=" + String.valueOf(getRating()) + ", ")
      .append("createdAt=" + String.valueOf(getCreatedAt()) + ", ")
      .append("blog=" + String.valueOf(getBlog()) + ", ")
      .append("blogOwner=" + String.valueOf(getBlogOwner()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()))
      .append("}")
      .toString();
  }
  
  public static IdStep builder() {
      return new Builder();
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(id,
      title,
      rating,
      createdAt,
      blog,
      blogOwner);
  }
  public interface IdStep {
    TitleStep id(String id);
  }
  

  public interface TitleStep {
    RatingStep title(String title);
  }
  

  public interface RatingStep {
    BuildStep rating(Integer rating);
  }
  

  public interface BuildStep {
    PostCPK build();
    BuildStep createdAt(Temporal.DateTime createdAt);
    BuildStep blog(BlogCPK blog);
    BuildStep blogOwner(BlogOwnerCPK blogOwner);
  }
  

  public static class Builder implements IdStep, TitleStep, RatingStep, BuildStep {
    private String id;
    private String title;
    private Integer rating;
    private Temporal.DateTime createdAt;
    private BlogCPK blog;
    private BlogOwnerCPK blogOwner;
    public Builder() {
      
    }
    
    private Builder(String id, String title, Integer rating, Temporal.DateTime createdAt, BlogCPK blog, BlogOwnerCPK blogOwner) {
      this.id = id;
      this.title = title;
      this.rating = rating;
      this.createdAt = createdAt;
      this.blog = blog;
      this.blogOwner = blogOwner;
    }
    
    @Override
     public PostCPK build() {
        
        return new PostCPK(
          id,
          title,
          rating,
          createdAt,
          blog,
          blogOwner);
    }
    
    @Override
     public TitleStep id(String id) {
        Objects.requireNonNull(id);
        this.id = id;
        return this;
    }
    
    @Override
     public RatingStep title(String title) {
        Objects.requireNonNull(title);
        this.title = title;
        return this;
    }
    
    @Override
     public BuildStep rating(Integer rating) {
        Objects.requireNonNull(rating);
        this.rating = rating;
        return this;
    }
    
    @Override
     public BuildStep createdAt(Temporal.DateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }
    
    @Override
     public BuildStep blog(BlogCPK blog) {
        this.blog = blog;
        return this;
    }
    
    @Override
     public BuildStep blogOwner(BlogOwnerCPK blogOwner) {
        this.blogOwner = blogOwner;
        return this;
    }
  }
  

  public final class CopyOfBuilder extends Builder {
    private CopyOfBuilder(String id, String title, Integer rating, Temporal.DateTime createdAt, BlogCPK blog, BlogOwnerCPK blogOwner) {
      super(id, title, rating, createdAt, blog, blogOwner);
      Objects.requireNonNull(id);
      Objects.requireNonNull(title);
      Objects.requireNonNull(rating);
    }
    
    @Override
     public CopyOfBuilder id(String id) {
      return (CopyOfBuilder) super.id(id);
    }
    
    @Override
     public CopyOfBuilder title(String title) {
      return (CopyOfBuilder) super.title(title);
    }
    
    @Override
     public CopyOfBuilder rating(Integer rating) {
      return (CopyOfBuilder) super.rating(rating);
    }
    
    @Override
     public CopyOfBuilder createdAt(Temporal.DateTime createdAt) {
      return (CopyOfBuilder) super.createdAt(createdAt);
    }
    
    @Override
     public CopyOfBuilder blog(BlogCPK blog) {
      return (CopyOfBuilder) super.blog(blog);
    }
    
    @Override
     public CopyOfBuilder blogOwner(BlogOwnerCPK blogOwner) {
      return (CopyOfBuilder) super.blogOwner(blogOwner);
    }
  }
  

  public static class PostCPKIdentifier extends ModelIdentifier<PostCPK> {
    private static final long serialVersionUID = 1L;
    public PostCPKIdentifier(String id, String title) {
      super(id, title);
    }
  }
  
}
