package com.amplifyframework.testmodels.commentsblog;

import com.amplifyframework.core.model.annotations.BelongsTo;
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

/** This is an auto generated class representing the PostAuthorJoin type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "PostAuthorJoins")
@Index(name = "byAuthor", fields = {"authorId"})
@Index(name = "byPost", fields = {"postId"})
public final class PostAuthorJoin implements Model {
  public static final QueryField ID = field("PostAuthorJoin", "id");
  public static final QueryField AUTHOR = field("PostAuthorJoin", "authorId");
  public static final QueryField POST = field("PostAuthorJoin", "postId");
  public static final QueryField CREATED_AT = field("PostAuthorJoin", "createdAt");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="Author") @BelongsTo(targetName = "authorId", type = Author.class) Author author;
  private final @ModelField(targetType="Post") @BelongsTo(targetName = "postId", type = Post.class) Post post;
  private final @ModelField(targetType="AWSDateTime") Temporal.DateTime createdAt;
  public String getId() {
      return id;
  }

  public String resolveIdentifier() {
      return id;
  }
  
  public Author getAuthor() {
      return author;
  }
  
  public Post getPost() {
      return post;
  }
  
  public Temporal.DateTime getCreatedAt() {
      return createdAt;
  }
  
  private PostAuthorJoin(String id, Author author, Post post, Temporal.DateTime createdAt) {
    this.id = id;
    this.author = author;
    this.post = post;
    this.createdAt = createdAt;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      PostAuthorJoin postAuthorJoin = (PostAuthorJoin) obj;
      return ObjectsCompat.equals(getId(), postAuthorJoin.getId()) &&
              ObjectsCompat.equals(getAuthor(), postAuthorJoin.getAuthor()) &&
              ObjectsCompat.equals(getPost(), postAuthorJoin.getPost()) &&
              ObjectsCompat.equals(getCreatedAt(), postAuthorJoin.getCreatedAt());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getAuthor())
      .append(getPost())
      .append(getCreatedAt())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("PostAuthorJoin {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("author=" + String.valueOf(getAuthor()) + ", ")
      .append("post=" + String.valueOf(getPost()) + ", ")
      .append("createdAt=" + String.valueOf(getCreatedAt()))
      .append("}")
      .toString();
  }
  
  public static BuildStep builder() {
      return new Builder();
  }
  
  /** 
   * WARNING: This method should not be used to build an instance of this object for a CREATE mutation.
   * This is a convenience method to return an instance of the object with only its ID populated
   * to be used in the context of a parameter in a delete mutation or referencing a foreign key
   * in a relationship.
   * @param id the id of the existing item this instance will represent
   * @return an instance of this model with only ID populated
   * @throws IllegalArgumentException Checks that ID is in the proper format
   */
  public static PostAuthorJoin justId(String id) {
    try {
      UUID.fromString(id); // Check that ID is in the UUID format - if not an exception is thrown
    } catch (Exception exception) {
      throw new IllegalArgumentException(
              "Model IDs must be unique in the format of UUID. This method is for creating instances " +
              "of an existing object with only its ID field for sending as a mutation parameter. When " +
              "creating a new object, use the standard builder method and leave the ID field blank."
      );
    }
    return new PostAuthorJoin(
      id,
      null,
      null,
      null
    );
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(id,
      author,
      post,
      createdAt);
  }
  public interface BuildStep {
    PostAuthorJoin build();
    BuildStep id(String id) throws IllegalArgumentException;
    BuildStep author(Author author);
    BuildStep post(Post post);
    BuildStep createdAt(Temporal.DateTime createdAt);
  }
  

  public static class Builder implements BuildStep {
    private String id;
    private Author author;
    private Post post;
    private Temporal.DateTime createdAt;
    @Override
     public PostAuthorJoin build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new PostAuthorJoin(
          id,
          author,
          post,
          createdAt);
    }
    
    @Override
     public BuildStep author(Author author) {
        this.author = author;
        return this;
    }
    
    @Override
     public BuildStep post(Post post) {
        this.post = post;
        return this;
    }
    
    @Override
     public BuildStep createdAt(Temporal.DateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }
    
    /** 
     * WARNING: Do not set ID when creating a new object. Leave this blank and one will be auto generated for you.
     * This should only be set when referring to an already existing object.
     * @param id id
     * @return Current Builder instance, for fluent method chaining
     * @throws IllegalArgumentException Checks that ID is in the proper format
     */
    public BuildStep id(String id) throws IllegalArgumentException {
        this.id = id;
        
        try {
            UUID.fromString(id); // Check that ID is in the UUID format - if not an exception is thrown
        } catch (Exception exception) {
          throw new IllegalArgumentException("Model IDs must be unique in the format of UUID.",
                    exception);
        }
        
        return this;
    }
  }
  

  public final class CopyOfBuilder extends Builder {
    private CopyOfBuilder(String id, Author author, Post post, Temporal.DateTime createdAt) {
      super.id(id);
      super.author(author)
        .post(post)
        .createdAt(createdAt);
    }
    
    @Override
     public CopyOfBuilder author(Author author) {
      return (CopyOfBuilder) super.author(author);
    }
    
    @Override
     public CopyOfBuilder post(Post post) {
      return (CopyOfBuilder) super.post(post);
    }
    
    @Override
     public CopyOfBuilder createdAt(Temporal.DateTime createdAt) {
      return (CopyOfBuilder) super.createdAt(createdAt);
    }
  }
  
}
