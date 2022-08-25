package com.amplifyframework.testmodels.commentsblog;

import com.amplifyframework.core.model.annotations.BelongsTo;
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

/** This is an auto generated class representing the Post type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "Posts")
public final class Post implements Model {
  public static final QueryField ID = field("Post", "id");
  public static final QueryField TITLE = field("Post", "title");
  public static final QueryField BLOG = field("Post", "blogPostsId");
  public static final QueryField AUTHOR = field("Post", "authorPostsId");
  public static final QueryField STATUS = field("Post", "status");
  public static final QueryField RATING = field("Post", "rating");
  public static final QueryField CREATED_AT = field("Post", "createdAt");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String", isRequired = true) String title;
  private final @ModelField(targetType="Blog") @BelongsTo(targetName = "blogPostsId", type = Blog.class) Blog blog;
  private final @ModelField(targetType="Comment") @HasMany(associatedWith = "post", type = Comment.class) List<Comment> comments = null;
  private final @ModelField(targetType="Author") @BelongsTo(targetName = "authorPostsId", type = Author.class) Author author;
  private final @ModelField(targetType="PostStatus", isRequired = true) PostStatus status;
  private final @ModelField(targetType="Int", isRequired = true) Integer rating;
  private final @ModelField(targetType="AWSDateTime") Temporal.DateTime createdAt;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime updatedAt;
  public String getId() {
      return id;
  }
  
  public String getTitle() {
      return title;
  }
  
  public Blog getBlog() {
      return blog;
  }
  
  public List<Comment> getComments() {
      return comments;
  }
  
  public Author getAuthor() {
      return author;
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
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  private Post(String id, String title, Blog blog, Author author, PostStatus status, Integer rating, Temporal.DateTime createdAt) {
    this.id = id;
    this.title = title;
    this.blog = blog;
    this.author = author;
    this.status = status;
    this.rating = rating;
    this.createdAt = createdAt;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      Post post = (Post) obj;
      return ObjectsCompat.equals(getId(), post.getId()) &&
              ObjectsCompat.equals(getTitle(), post.getTitle()) &&
              ObjectsCompat.equals(getBlog(), post.getBlog()) &&
              ObjectsCompat.equals(getAuthor(), post.getAuthor()) &&
              ObjectsCompat.equals(getStatus(), post.getStatus()) &&
              ObjectsCompat.equals(getRating(), post.getRating()) &&
              ObjectsCompat.equals(getCreatedAt(), post.getCreatedAt()) &&
              ObjectsCompat.equals(getUpdatedAt(), post.getUpdatedAt());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getTitle())
      .append(getBlog())
      .append(getAuthor())
      .append(getStatus())
      .append(getRating())
      .append(getCreatedAt())
      .append(getUpdatedAt())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("Post {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("title=" + String.valueOf(getTitle()) + ", ")
      .append("blog=" + String.valueOf(getBlog()) + ", ")
      .append("author=" + String.valueOf(getAuthor()) + ", ")
      .append("status=" + String.valueOf(getStatus()) + ", ")
      .append("rating=" + String.valueOf(getRating()) + ", ")
      .append("createdAt=" + String.valueOf(getCreatedAt()) + ", ")
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
  public static Post justId(String id) {
    return new Post(
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
      blog,
      author,
      status,
      rating,
      createdAt);
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
    Post build();
    BuildStep id(String id);
    BuildStep blog(Blog blog);
    BuildStep author(Author author);
    BuildStep createdAt(Temporal.DateTime createdAt);
  }
  

  public static class Builder implements TitleStep, StatusStep, RatingStep, BuildStep {
    private String id;
    private String title;
    private PostStatus status;
    private Integer rating;
    private Blog blog;
    private Author author;
    private Temporal.DateTime createdAt;
    @Override
     public Post build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new Post(
          id,
          title,
          blog,
          author,
          status,
          rating,
          createdAt);
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
     public BuildStep blog(Blog blog) {
        this.blog = blog;
        return this;
    }
    
    @Override
     public BuildStep author(Author author) {
        this.author = author;
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
    private CopyOfBuilder(String id, String title, Blog blog, Author author, PostStatus status, Integer rating, Temporal.DateTime createdAt) {
      super.id(id);
      super.title(title)
        .status(status)
        .rating(rating)
        .blog(blog)
        .author(author)
        .createdAt(createdAt);
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
     public CopyOfBuilder blog(Blog blog) {
      return (CopyOfBuilder) super.blog(blog);
    }
    
    @Override
     public CopyOfBuilder author(Author author) {
      return (CopyOfBuilder) super.author(author);
    }
    
    @Override
     public CopyOfBuilder createdAt(Temporal.DateTime createdAt) {
      return (CopyOfBuilder) super.createdAt(createdAt);
    }
  }
  
}
