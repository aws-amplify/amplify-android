package com.amplifyframework.datastore.generated.model;

import static com.amplifyframework.core.model.query.predicate.QueryField.field;

import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.model.LoadedModelReferenceImpl;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelIdentifier;
import com.amplifyframework.core.model.ModelList;
import com.amplifyframework.core.model.ModelReference;
import com.amplifyframework.core.model.annotations.BelongsTo;
import com.amplifyframework.core.model.annotations.HasMany;
import com.amplifyframework.core.model.annotations.Index;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.core.model.annotations.ModelField;
import com.amplifyframework.core.model.query.predicate.QueryField;
import com.amplifyframework.core.model.temporal.Temporal;

import java.util.Objects;

/** This is an auto generated class representing the Post type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "Posts", type = Model.Type.USER, version = 1, hasLazySupport = true)
@Index(name = "undefined", fields = {"postId","title"})
public final class Post implements Model {
  public static final PostPath rootPath = new PostPath("root", false, null);
  public static final QueryField POST_ID = field("Post", "postId");
  public static final QueryField TITLE = field("Post", "title");
  public static final QueryField BLOG = field("Post", "blogPostsBlogId");
  private final @ModelField(targetType="ID", isRequired = true) String postId;
  private final @ModelField(targetType="String", isRequired = true) String title;
  private final @ModelField(targetType="Blog", isRequired = true) @BelongsTo(targetName = "blogPostsBlogId", targetNames = {"blogPostsBlogId"}, type = Blog.class) ModelReference<Blog> blog;
  private final @ModelField(targetType="Comment", isRequired = true) @HasMany(associatedWith = "post", type = Comment.class) ModelList<Comment> comments = null;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime createdAt;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime updatedAt;
  private PostIdentifier postIdentifier;
  /** @deprecated This API is internal to Amplify and should not be used. */
  @Deprecated
   public PostIdentifier resolveIdentifier() {
    if (postIdentifier == null) {
      this.postIdentifier = new PostIdentifier(postId, title);
    }
    return postIdentifier;
  }
  
  public String getPostId() {
      return postId;
  }
  
  public String getTitle() {
      return title;
  }
  
  public ModelReference<Blog> getBlog() {
      return blog;
  }
  
  public ModelList<Comment> getComments() {
      return comments;
  }
  
  public Temporal.DateTime getCreatedAt() {
      return createdAt;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  private Post(String postId, String title, ModelReference<Blog> blog) {
    this.postId = postId;
    this.title = title;
    this.blog = blog;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      Post post = (Post) obj;
      return ObjectsCompat.equals(getPostId(), post.getPostId()) &&
              ObjectsCompat.equals(getTitle(), post.getTitle()) &&
              ObjectsCompat.equals(getBlog(), post.getBlog()) &&
              ObjectsCompat.equals(getCreatedAt(), post.getCreatedAt()) &&
              ObjectsCompat.equals(getUpdatedAt(), post.getUpdatedAt());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getPostId())
      .append(getTitle())
      .append(getBlog())
      .append(getCreatedAt())
      .append(getUpdatedAt())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("Post {")
      .append("postId=" + String.valueOf(getPostId()) + ", ")
      .append("title=" + String.valueOf(getTitle()) + ", ")
      .append("blog=" + String.valueOf(getBlog()) + ", ")
      .append("createdAt=" + String.valueOf(getCreatedAt()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()))
      .append("}")
      .toString();
  }
  
  public static PostIdStep builder() {
      return new Builder();
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(postId,
      title,
      blog);
  }
  public interface PostIdStep {
    TitleStep postId(String postId);
  }
  

  public interface TitleStep {
    BlogStep title(String title);
  }
  

  public interface BlogStep {
    BuildStep blog(Blog blog);
  }
  

  public interface BuildStep {
    Post build();
  }
  

  public static class Builder implements PostIdStep, TitleStep, BlogStep, BuildStep {
    private String postId;
    private String title;
    private ModelReference<Blog> blog;
    public Builder() {
      
    }
    
    private Builder(String postId, String title, ModelReference<Blog> blog) {
      this.postId = postId;
      this.title = title;
      this.blog = blog;
    }
    
    @Override
     public Post build() {
        
        return new Post(
          postId,
          title,
          blog);
    }
    
    @Override
     public TitleStep postId(String postId) {
        Objects.requireNonNull(postId);
        this.postId = postId;
        return this;
    }
    
    @Override
     public BlogStep title(String title) {
        Objects.requireNonNull(title);
        this.title = title;
        return this;
    }
    
    @Override
     public BuildStep blog(Blog blog) {
        Objects.requireNonNull(blog);
        this.blog = new LoadedModelReferenceImpl<>(blog);
        return this;
    }
  }
  

  public final class CopyOfBuilder extends Builder {
    private CopyOfBuilder(String postId, String title, ModelReference<Blog> blog) {
      super(postId, title, blog);
      Objects.requireNonNull(postId);
      Objects.requireNonNull(title);
      Objects.requireNonNull(blog);
    }
    
    @Override
     public CopyOfBuilder postId(String postId) {
      return (CopyOfBuilder) super.postId(postId);
    }
    
    @Override
     public CopyOfBuilder title(String title) {
      return (CopyOfBuilder) super.title(title);
    }
    
    @Override
     public CopyOfBuilder blog(Blog blog) {
      return (CopyOfBuilder) super.blog(blog);
    }
  }
  

  public static class PostIdentifier extends ModelIdentifier<Post> {
    private static final long serialVersionUID = 1L;
    public PostIdentifier(String postId, String title) {
      super(postId, title);
    }
  }
  
}
