package com.amplifyframework.testmodels.commentsblog;

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
import java.util.UUID;

/** This is an auto generated class representing the Post2 type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "Post2s", type = Model.Type.USER, version = 1)
@Index(name = "byBlog3", fields = {"blogID"})
@Index(name = "byBlogOwner3", fields = {"blogOwnerID"})
public final class Post2 implements Model {
  public static final QueryField ID = field("Post2", "id");
  public static final QueryField TITLE = field("Post2", "title");
  public static final QueryField STATUS = field("Post2", "status");
  public static final QueryField RATING = field("Post2", "rating");
  public static final QueryField CREATED_AT = field("Post2", "createdAt");
  public static final QueryField BLOG = field("Post2", "blogID");
  public static final QueryField BLOG_OWNER = field("Post2", "blogOwnerID");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String", isRequired = true) String title;
  private final @ModelField(targetType="PostStatus", isRequired = true) PostStatus status;
  private final @ModelField(targetType="Int", isRequired = true) Integer rating;
  private final @ModelField(targetType="AWSDateTime") Temporal.DateTime createdAt;
  private final @ModelField(targetType="Blog3") @BelongsTo(targetName = "blogID", targetNames = {"blogID"}, type = Blog3.class) Blog3 blog;
  private final @ModelField(targetType="BlogOwner3") @BelongsTo(targetName = "blogOwnerID", targetNames = {"blogOwnerID"}, type = BlogOwner3.class) BlogOwner3 blogOwner;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime updatedAt;
  /** @deprecated This API is internal to Amplify and should not be used. */
  @Deprecated
   public String resolveIdentifier() {
    return id;
  }
  
  public String getId() {
      return id;
  }
  
  public String getTitle() {
      return title;
  }
  
  public PostStatus getStatus() {
      return status;
  }
  
  public Integer getRating() {
      return rating;
  }
  
  public Temporal.DateTime getCreatedAt() {
      return createdAt;
  }
  
  public Blog3 getBlog() {
      return blog;
  }
  
  public BlogOwner3 getBlogOwner() {
      return blogOwner;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  private Post2(String id, String title, PostStatus status, Integer rating, Temporal.DateTime createdAt, Blog3 blog, BlogOwner3 blogOwner) {
    this.id = id;
    this.title = title;
    this.status = status;
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
      Post2 post2 = (Post2) obj;
      return ObjectsCompat.equals(getId(), post2.getId()) &&
              ObjectsCompat.equals(getTitle(), post2.getTitle()) &&
              ObjectsCompat.equals(getStatus(), post2.getStatus()) &&
              ObjectsCompat.equals(getRating(), post2.getRating()) &&
              ObjectsCompat.equals(getCreatedAt(), post2.getCreatedAt()) &&
              ObjectsCompat.equals(getBlog(), post2.getBlog()) &&
              ObjectsCompat.equals(getBlogOwner(), post2.getBlogOwner()) &&
              ObjectsCompat.equals(getUpdatedAt(), post2.getUpdatedAt());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getTitle())
      .append(getStatus())
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
      .append("Post2 {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("title=" + String.valueOf(getTitle()) + ", ")
      .append("status=" + String.valueOf(getStatus()) + ", ")
      .append("rating=" + String.valueOf(getRating()) + ", ")
      .append("createdAt=" + String.valueOf(getCreatedAt()) + ", ")
      .append("blog=" + String.valueOf(getBlog()) + ", ")
      .append("blogOwner=" + String.valueOf(getBlogOwner()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()))
      .append("}")
      .toString();
  }
  
  public static TitleStep builder() {
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
  public static Post2 justId(String id) {
    return new Post2(
      id,
      null,
      null,
      null,
      null,
      null,
      null
    );
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(id,
      title,
      status,
      rating,
      createdAt,
      blog,
      blogOwner);
  }
  public interface TitleStep {
    StatusStep title(String title);
  }
  

  public interface StatusStep {
    RatingStep status(PostStatus status);
  }
  

  public interface RatingStep {
    BuildStep rating(Integer rating);
  }
  

  public interface BuildStep {
    Post2 build();
    BuildStep id(String id);
    BuildStep createdAt(Temporal.DateTime createdAt);
    BuildStep blog(Blog3 blog);
    BuildStep blogOwner(BlogOwner3 blogOwner);
  }
  

  public static class Builder implements TitleStep, StatusStep, RatingStep, BuildStep {
    private String id;
    private String title;
    private PostStatus status;
    private Integer rating;
    private Temporal.DateTime createdAt;
    private Blog3 blog;
    private BlogOwner3 blogOwner;
    public Builder() {
      
    }
    
    private Builder(String id, String title, PostStatus status, Integer rating, Temporal.DateTime createdAt, Blog3 blog, BlogOwner3 blogOwner) {
      this.id = id;
      this.title = title;
      this.status = status;
      this.rating = rating;
      this.createdAt = createdAt;
      this.blog = blog;
      this.blogOwner = blogOwner;
    }
    
    @Override
     public Post2 build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new Post2(
          id,
          title,
          status,
          rating,
          createdAt,
          blog,
          blogOwner);
    }
    
    @Override
     public StatusStep title(String title) {
        Objects.requireNonNull(title);
        this.title = title;
        return this;
    }
    
    @Override
     public RatingStep status(PostStatus status) {
        Objects.requireNonNull(status);
        this.status = status;
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
     public BuildStep blog(Blog3 blog) {
        this.blog = blog;
        return this;
    }
    
    @Override
     public BuildStep blogOwner(BlogOwner3 blogOwner) {
        this.blogOwner = blogOwner;
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
    private CopyOfBuilder(String id, String title, PostStatus status, Integer rating, Temporal.DateTime createdAt, Blog3 blog, BlogOwner3 blogOwner) {
      super(id, title, status, rating, createdAt, blog, blogOwner);
      Objects.requireNonNull(title);
      Objects.requireNonNull(status);
      Objects.requireNonNull(rating);
    }
    
    @Override
     public CopyOfBuilder title(String title) {
      return (CopyOfBuilder) super.title(title);
    }
    
    @Override
     public CopyOfBuilder status(PostStatus status) {
      return (CopyOfBuilder) super.status(status);
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
     public CopyOfBuilder blog(Blog3 blog) {
      return (CopyOfBuilder) super.blog(blog);
    }
    
    @Override
     public CopyOfBuilder blogOwner(BlogOwner3 blogOwner) {
      return (CopyOfBuilder) super.blogOwner(blogOwner);
    }
  }
  

  public static class Post2Identifier extends ModelIdentifier<Post2> {
    private static final long serialVersionUID = 1L;
    public Post2Identifier(String id) {
      super(id);
    }
  }
  
}
