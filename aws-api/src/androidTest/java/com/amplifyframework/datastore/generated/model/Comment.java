package com.amplifyframework.datastore.generated.model;

import static com.amplifyframework.core.model.query.predicate.QueryField.field;

import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.model.LoadedModelReferenceImpl;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelIdentifier;
import com.amplifyframework.core.model.ModelReference;
import com.amplifyframework.core.model.annotations.BelongsTo;
import com.amplifyframework.core.model.annotations.Index;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.core.model.annotations.ModelField;
import com.amplifyframework.core.model.query.predicate.QueryField;
import com.amplifyframework.core.model.temporal.Temporal;

import java.util.Objects;

/** This is an auto generated class representing the Comment type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "Comments", type = Model.Type.USER, version = 1, hasLazySupport = true)
@Index(name = "undefined", fields = {"commentId","content"})
public final class Comment implements Model {
  public static final CommentPath rootPath = new CommentPath("root", false, null);
  public static final QueryField COMMENT_ID = field("Comment", "commentId");
  public static final QueryField CONTENT = field("Comment", "content");
  public static final QueryField POST = field("Comment", "postCommentsPostId");
  private final @ModelField(targetType="ID", isRequired = true) String commentId;
  private final @ModelField(targetType="String", isRequired = true) String content;
  private final @ModelField(targetType="Post", isRequired = true) @BelongsTo(targetName = "postCommentsPostId", targetNames = {"postCommentsPostId", "postCommentsTitle"}, type = Post.class) ModelReference<Post> post;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime createdAt;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime updatedAt;
  private CommentIdentifier commentIdentifier;
  /** @deprecated This API is internal to Amplify and should not be used. */
  @Deprecated
   public CommentIdentifier resolveIdentifier() {
    if (commentIdentifier == null) {
      this.commentIdentifier = new CommentIdentifier(commentId, content);
    }
    return commentIdentifier;
  }
  
  public String getCommentId() {
      return commentId;
  }
  
  public String getContent() {
      return content;
  }
  
  public ModelReference<Post> getPost() {
      return post;
  }
  
  public Temporal.DateTime getCreatedAt() {
      return createdAt;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  private Comment(String commentId, String content, ModelReference<Post> post) {
    this.commentId = commentId;
    this.content = content;
    this.post = post;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      Comment comment = (Comment) obj;
      return ObjectsCompat.equals(getCommentId(), comment.getCommentId()) &&
              ObjectsCompat.equals(getContent(), comment.getContent()) &&
              ObjectsCompat.equals(getPost(), comment.getPost()) &&
              ObjectsCompat.equals(getCreatedAt(), comment.getCreatedAt()) &&
              ObjectsCompat.equals(getUpdatedAt(), comment.getUpdatedAt());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getCommentId())
      .append(getContent())
      .append(getPost())
      .append(getCreatedAt())
      .append(getUpdatedAt())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("Comment {")
      .append("commentId=" + String.valueOf(getCommentId()) + ", ")
      .append("content=" + String.valueOf(getContent()) + ", ")
      .append("post=" + String.valueOf(getPost()) + ", ")
      .append("createdAt=" + String.valueOf(getCreatedAt()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()))
      .append("}")
      .toString();
  }
  
  public static CommentIdStep builder() {
      return new Builder();
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(commentId,
      content,
      post);
  }
  public interface CommentIdStep {
    ContentStep commentId(String commentId);
  }
  

  public interface ContentStep {
    PostStep content(String content);
  }
  

  public interface PostStep {
    BuildStep post(Post post);
  }
  

  public interface BuildStep {
    Comment build();
  }
  

  public static class Builder implements CommentIdStep, ContentStep, PostStep, BuildStep {
    private String commentId;
    private String content;
    private ModelReference<Post> post;
    public Builder() {
      
    }
    
    private Builder(String commentId, String content, ModelReference<Post> post) {
      this.commentId = commentId;
      this.content = content;
      this.post = post;
    }
    
    @Override
     public Comment build() {
        
        return new Comment(
          commentId,
          content,
          post);
    }
    
    @Override
     public ContentStep commentId(String commentId) {
        Objects.requireNonNull(commentId);
        this.commentId = commentId;
        return this;
    }
    
    @Override
     public PostStep content(String content) {
        Objects.requireNonNull(content);
        this.content = content;
        return this;
    }
    
    @Override
     public BuildStep post(Post post) {
        Objects.requireNonNull(post);
        this.post = new LoadedModelReferenceImpl<>(post);
        return this;
    }
  }
  

  public final class CopyOfBuilder extends Builder {
    private CopyOfBuilder(String commentId, String content, ModelReference<Post> post) {
      super(commentId, content, post);
      Objects.requireNonNull(commentId);
      Objects.requireNonNull(content);
      Objects.requireNonNull(post);
    }
    
    @Override
     public CopyOfBuilder commentId(String commentId) {
      return (CopyOfBuilder) super.commentId(commentId);
    }
    
    @Override
     public CopyOfBuilder content(String content) {
      return (CopyOfBuilder) super.content(content);
    }
    
    @Override
     public CopyOfBuilder post(Post post) {
      return (CopyOfBuilder) super.post(post);
    }
  }
  

  public static class CommentIdentifier extends ModelIdentifier<Comment> {
    private static final long serialVersionUID = 1L;
    public CommentIdentifier(String commentId, String content) {
      super(commentId, content);
    }
  }
  
}
