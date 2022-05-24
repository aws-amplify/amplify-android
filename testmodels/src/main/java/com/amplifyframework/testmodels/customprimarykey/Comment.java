package com.amplifyframework.testmodels.customprimarykey;

import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelPrimaryKey;
import com.amplifyframework.core.model.annotations.BelongsTo;
import com.amplifyframework.core.model.annotations.Index;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.core.model.annotations.ModelField;
import com.amplifyframework.core.model.query.predicate.QueryField;
import com.amplifyframework.core.model.temporal.Temporal;

import java.util.Objects;

import static com.amplifyframework.core.model.query.predicate.QueryField.field;

/** This is an auto generated class representing the Comment type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "Comments", type = Model.Type.USER, version = "V1")
@Index(name = "undefined", fields = {"title","content","likes"})
public final class Comment implements Model {
  public static final QueryField POST = field("Comment", "postCommentsId");
  public static final QueryField TITLE = field("Comment", "title");
  public static final QueryField CONTENT = field("Comment", "content");
  public static final QueryField LIKES = field("Comment", "likes");
  public static final QueryField DESCRIPTION = field("Comment", "description");
  private final @ModelField(targetType="Post") @BelongsTo(targetName = "postCommentsId", type = Post.class) Post post;
  private final @ModelField(targetType="String", isRequired = true) String title;
  private final @ModelField(targetType="String", isRequired = true) String content;
  private final @ModelField(targetType="Int", isRequired = true) Integer likes;
  private final @ModelField(targetType="String") String description;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime createdAt;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime updatedAt;
  private CommentPrimaryKey commentPrimaryKey;
  public CommentPrimaryKey resolveIdentifier() {
    if (commentPrimaryKey == null) {
      this.commentPrimaryKey = new CommentPrimaryKey(title, content, likes);
    }
    return commentPrimaryKey;
  }
  
  public Post getPost() {
      return post;
  }
  
  public String getTitle() {
      return title;
  }
  
  public String getContent() {
      return content;
  }
  
  public Integer getLikes() {
      return likes;
  }
  
  public String getDescription() {
      return description;
  }
  
  public Temporal.DateTime getCreatedAt() {
      return createdAt;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  private Comment(Post post, String title, String content, Integer likes, String description) {
    this.post = post;
    this.title = title;
    this.content = content;
    this.likes = likes;
    this.description = description;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      Comment comment = (Comment) obj;
      return ObjectsCompat.equals(getPost(), comment.getPost()) &&
              ObjectsCompat.equals(getTitle(), comment.getTitle()) &&
              ObjectsCompat.equals(getContent(), comment.getContent()) &&
              ObjectsCompat.equals(getLikes(), comment.getLikes()) &&
              ObjectsCompat.equals(getDescription(), comment.getDescription()) &&
              ObjectsCompat.equals(getCreatedAt(), comment.getCreatedAt()) &&
              ObjectsCompat.equals(getUpdatedAt(), comment.getUpdatedAt());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getPost())
      .append(getTitle())
      .append(getContent())
      .append(getLikes())
      .append(getDescription())
      .append(getCreatedAt())
      .append(getUpdatedAt())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("Comment {")
      .append("post=" + String.valueOf(getPost()) + ", ")
      .append("title=" + String.valueOf(getTitle()) + ", ")
      .append("content=" + String.valueOf(getContent()) + ", ")
      .append("likes=" + String.valueOf(getLikes()) + ", ")
      .append("description=" + String.valueOf(getDescription()) + ", ")
      .append("createdAt=" + String.valueOf(getCreatedAt()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()))
      .append("}")
      .toString();
  }
  
  public static TitleStep builder() {
      return new Builder();
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(post,
      title,
      content,
      likes,
      description);
  }
  public interface TitleStep {
    ContentStep title(String title);
  }
  

  public interface ContentStep {
    LikesStep content(String content);
  }
  

  public interface LikesStep {
    BuildStep likes(Integer likes);
  }
  

  public interface BuildStep {
    Comment build();
    BuildStep post(Post post);
    BuildStep description(String description);
  }
  

  public static class Builder implements TitleStep, ContentStep, LikesStep, BuildStep {
    private String title;
    private String content;
    private Integer likes;
    private Post post;
    private String description;
    @Override
     public Comment build() {
        
        return new Comment(
          post,
          title,
          content,
          likes,
          description);
    }
    
    @Override
     public ContentStep title(String title) {
        Objects.requireNonNull(title);
        this.title = title;
        return this;
    }
    
    @Override
     public LikesStep content(String content) {
        Objects.requireNonNull(content);
        this.content = content;
        return this;
    }
    
    @Override
     public BuildStep likes(Integer likes) {
        Objects.requireNonNull(likes);
        this.likes = likes;
        return this;
    }
    
    @Override
     public BuildStep post(Post post) {
        this.post = post;
        return this;
    }
    
    @Override
     public BuildStep description(String description) {
        this.description = description;
        return this;
    }
  }
  

  public final class CopyOfBuilder extends Builder {
    private CopyOfBuilder(Post post, String title, String content, Integer likes, String description) {
      super.title(title)
        .content(content)
        .likes(likes)
        .post(post)
        .description(description);
    }
    
    @Override
     public CopyOfBuilder title(String title) {
      return (CopyOfBuilder) super.title(title);
    }
    
    @Override
     public CopyOfBuilder content(String content) {
      return (CopyOfBuilder) super.content(content);
    }
    
    @Override
     public CopyOfBuilder likes(Integer likes) {
      return (CopyOfBuilder) super.likes(likes);
    }
    
    @Override
     public CopyOfBuilder post(Post post) {
      return (CopyOfBuilder) super.post(post);
    }
    
    @Override
     public CopyOfBuilder description(String description) {
      return (CopyOfBuilder) super.description(description);
    }
  }
  

  public static class CommentPrimaryKey extends ModelPrimaryKey<Comment> {
    private static final long serialVersionUID = 1L;
    public CommentPrimaryKey(String title, String content, Integer likes) {
      super(title, content, likes);
    }
  }
  
}
