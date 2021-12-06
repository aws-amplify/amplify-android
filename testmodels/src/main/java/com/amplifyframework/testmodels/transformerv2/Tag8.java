package com.amplifyframework.testmodels.transformerv2;

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

/** This is an auto generated class representing the Tag8 type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "Tag8s")
public final class Tag8 implements Model {
  public static final QueryField ID = field("Tag8", "id");
  public static final QueryField LABEL = field("Tag8", "label");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String", isRequired = true) String label;
  private final @ModelField(targetType="PostTags") @HasMany(associatedWith = "tag8", type = PostTags.class) List<PostTags> posts = null;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime createdAt;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime updatedAt;
  public String getId() {
      return id;
  }
  
  public String getLabel() {
      return label;
  }
  
  public List<PostTags> getPosts() {
      return posts;
  }
  
  public Temporal.DateTime getCreatedAt() {
      return createdAt;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  private Tag8(String id, String label) {
    this.id = id;
    this.label = label;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      Tag8 tag8 = (Tag8) obj;
      return ObjectsCompat.equals(getId(), tag8.getId()) &&
              ObjectsCompat.equals(getLabel(), tag8.getLabel()) &&
              ObjectsCompat.equals(getCreatedAt(), tag8.getCreatedAt()) &&
              ObjectsCompat.equals(getUpdatedAt(), tag8.getUpdatedAt());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getLabel())
      .append(getCreatedAt())
      .append(getUpdatedAt())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("Tag8 {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("label=" + String.valueOf(getLabel()) + ", ")
      .append("createdAt=" + String.valueOf(getCreatedAt()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()))
      .append("}")
      .toString();
  }
  
  public static LabelStep builder() {
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
  public static Tag8 justId(String id) {
    return new Tag8(
      id,
      null
    );
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(id,
      label);
  }
  public interface LabelStep {
    BuildStep label(String label);
  }
  

  public interface BuildStep {
    Tag8 build();
    BuildStep id(String id);
  }
  

  public static class Builder implements LabelStep, BuildStep {
    private String id;
    private String label;
    @Override
     public Tag8 build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new Tag8(
          id,
          label);
    }
    
    @Override
     public BuildStep label(String label) {
        Objects.requireNonNull(label);
        this.label = label;
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
    private CopyOfBuilder(String id, String label) {
      super.id(id);
      super.label(label);
    }
    
    @Override
     public CopyOfBuilder label(String label) {
      return (CopyOfBuilder) super.label(label);
    }
  }
  
}
