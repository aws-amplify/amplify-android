package com.amplifyframework.testmodels.lazy;

import static com.amplifyframework.core.model.query.predicate.QueryField.field;

import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.model.LoadedModelReferenceImpl;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelIdentifier;
import com.amplifyframework.core.model.ModelReference;
import com.amplifyframework.core.model.annotations.BelongsTo;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.core.model.annotations.ModelField;
import com.amplifyframework.core.model.query.predicate.QueryField;
import com.amplifyframework.core.model.temporal.Temporal;

import java.util.Objects;
import java.util.UUID;

/** This is an auto generated class representing the Comment type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "Comments", type = Model.Type.USER, version = 1, hasLazySupport = true)
public final class Comment implements Model {
  public static final CommentPath rootPath = new CommentPath("root", false, null);
  public static final QueryField ID = field("Comment", "id");
  public static final QueryField TEXT = field("Comment", "text");
  public static final QueryField POST = field("Comment", "postCommentsId");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String", isRequired = true) String text;
  private final @ModelField(targetType="Post", isRequired = true) @BelongsTo(targetName = "postCommentsId", targetNames = {"postCommentsId"}, type = Post.class) ModelReference<Post> post;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime createdAt;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime updatedAt;
  /** @deprecated This API is internal to Amplify and should not be used. */
  @Deprecated
   public String resolveIdentifier() {
    return id;
  }
  
  public String getId() {
      return id;
  }
  
  public String getText() {
      return text;
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
  
  private Comment(String id, String text, ModelReference<Post> post) {
    this.id = id;
    this.text = text;
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
      return ObjectsCompat.equals(getId(), comment.getId()) &&
              ObjectsCompat.equals(getText(), comment.getText()) &&
              ObjectsCompat.equals(getPost(), comment.getPost()) &&
              ObjectsCompat.equals(getCreatedAt(), comment.getCreatedAt()) &&
              ObjectsCompat.equals(getUpdatedAt(), comment.getUpdatedAt());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getText())
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
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("text=" + String.valueOf(getText()) + ", ")
      .append("post=" + String.valueOf(getPost()) + ", ")
      .append("createdAt=" + String.valueOf(getCreatedAt()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()))
      .append("}")
      .toString();
  }
  
  public static TextStep builder() {
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
  public static Comment justId(String id) {
    return new Comment(
      id,
      null,
      null
    );
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(id,
      text,
      post);
  }
  public interface TextStep {
    PostStep text(String text);
  }
  

  public interface PostStep {
    BuildStep post(Post post);
  }
  

  public interface BuildStep {
    Comment build();
    BuildStep id(String id);
  }
  

  public static class Builder implements TextStep, PostStep, BuildStep {
    private String id;
    private String text;
    private ModelReference<Post> post;
    public Builder() {
      
    }
    
    private Builder(String id, String text, ModelReference<Post> post) {
      this.id = id;
      this.text = text;
      this.post = post;
    }
    
    @Override
     public Comment build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new Comment(
          id,
          text,
          post);
    }
    
    @Override
     public PostStep text(String text) {
        Objects.requireNonNull(text);
        this.text = text;
        return this;
    }
    
    @Override
     public BuildStep post(Post post) {
        Objects.requireNonNull(post);
        this.post = new LoadedModelReferenceImpl<>(post);
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
    private CopyOfBuilder(String id, String text, ModelReference<Post> post) {
      super(id, text, post);
      Objects.requireNonNull(text);
      Objects.requireNonNull(post);
    }
    
    @Override
     public CopyOfBuilder text(String text) {
      return (CopyOfBuilder) super.text(text);
    }
    
    @Override
     public CopyOfBuilder post(Post post) {
      return (CopyOfBuilder) super.post(post);
    }
  }
  

  public static class CommentIdentifier extends ModelIdentifier<Comment> {
    private static final long serialVersionUID = 1L;
    public CommentIdentifier(String id) {
      super(id);
    }
  }
  
}
