package com.amplifyframework.testmodels.transformerv2;

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

/** This is an auto generated class representing the RecipePost12 type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "RecipePost12s")
public final class RecipePost12 implements Model {
  public static final QueryField ID = field("RecipePost12", "id");
  public static final QueryField TITLE = field("RecipePost12", "title");
  public static final QueryField BLOG = field("RecipePost12", "cookingBlog12PostsId");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String", isRequired = true) String title;
  private final @ModelField(targetType="CookingBlog12") @BelongsTo(targetName = "cookingBlog12PostsId", type = CookingBlog12.class) CookingBlog12 blog;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime createdAt;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime updatedAt;
  public String getId() {
      return id;
  }
  
  public String getTitle() {
      return title;
  }
  
  public CookingBlog12 getBlog() {
      return blog;
  }
  
  public Temporal.DateTime getCreatedAt() {
      return createdAt;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  private RecipePost12(String id, String title, CookingBlog12 blog) {
    this.id = id;
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
      RecipePost12 recipePost12 = (RecipePost12) obj;
      return ObjectsCompat.equals(getId(), recipePost12.getId()) &&
              ObjectsCompat.equals(getTitle(), recipePost12.getTitle()) &&
              ObjectsCompat.equals(getBlog(), recipePost12.getBlog()) &&
              ObjectsCompat.equals(getCreatedAt(), recipePost12.getCreatedAt()) &&
              ObjectsCompat.equals(getUpdatedAt(), recipePost12.getUpdatedAt());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
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
      .append("RecipePost12 {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("title=" + String.valueOf(getTitle()) + ", ")
      .append("blog=" + String.valueOf(getBlog()) + ", ")
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
  public static RecipePost12 justId(String id) {
    return new RecipePost12(
      id,
      null,
      null
    );
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(id,
      title,
      blog);
  }
  public interface TitleStep {
    BuildStep title(String title);
  }
  

  public interface BuildStep {
    RecipePost12 build();
    BuildStep id(String id);
    BuildStep blog(CookingBlog12 blog);
  }
  

  public static class Builder implements TitleStep, BuildStep {
    private String id;
    private String title;
    private CookingBlog12 blog;
    @Override
     public RecipePost12 build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new RecipePost12(
          id,
          title,
          blog);
    }
    
    @Override
     public BuildStep title(String title) {
        Objects.requireNonNull(title);
        this.title = title;
        return this;
    }
    
    @Override
     public BuildStep blog(CookingBlog12 blog) {
        this.blog = blog;
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
    private CopyOfBuilder(String id, String title, CookingBlog12 blog) {
      super.id(id);
      super.title(title)
        .blog(blog);
    }
    
    @Override
     public CopyOfBuilder title(String title) {
      return (CopyOfBuilder) super.title(title);
    }
    
    @Override
     public CopyOfBuilder blog(CookingBlog12 blog) {
      return (CopyOfBuilder) super.blog(blog);
    }
  }
  
}
