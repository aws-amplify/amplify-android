package com.amplifyframework.datastore.generated.model;

import static com.amplifyframework.core.model.query.predicate.QueryField.field;

import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelIdentifier;
import com.amplifyframework.core.model.annotations.Index;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.core.model.annotations.ModelField;
import com.amplifyframework.core.model.query.predicate.QueryField;
import com.amplifyframework.core.model.temporal.Temporal;

import java.util.UUID;

/** This is an auto generated class representing the HasOneChild type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "HasOneChildren", type = Model.Type.USER, version = 1, hasLazySupport = true)
@Index(name = "undefined", fields = {"id"})
public final class HasOneChild implements Model {
  public static final HasOneChildPath rootPath = new HasOneChildPath("root", false, null);
  public static final QueryField ID = field("HasOneChild", "id");
  public static final QueryField CONTENT = field("HasOneChild", "content");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String") String content;
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
  
  public String getContent() {
      return content;
  }
  
  public Temporal.DateTime getCreatedAt() {
      return createdAt;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  private HasOneChild(String id, String content) {
    this.id = id;
    this.content = content;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      HasOneChild hasOneChild = (HasOneChild) obj;
      return ObjectsCompat.equals(getId(), hasOneChild.getId()) &&
              ObjectsCompat.equals(getContent(), hasOneChild.getContent()) &&
              ObjectsCompat.equals(getCreatedAt(), hasOneChild.getCreatedAt()) &&
              ObjectsCompat.equals(getUpdatedAt(), hasOneChild.getUpdatedAt());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getContent())
      .append(getCreatedAt())
      .append(getUpdatedAt())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("HasOneChild {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("content=" + String.valueOf(getContent()) + ", ")
      .append("createdAt=" + String.valueOf(getCreatedAt()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()))
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
   */
  public static HasOneChild justId(String id) {
    return new HasOneChild(
      id,
      null
    );
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(id,
      content);
  }
  public interface BuildStep {
    HasOneChild build();
    BuildStep id(String id);
    BuildStep content(String content);
  }
  

  public static class Builder implements BuildStep {
    private String id;
    private String content;
    public Builder() {
      
    }
    
    private Builder(String id, String content) {
      this.id = id;
      this.content = content;
    }
    
    @Override
     public HasOneChild build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new HasOneChild(
          id,
          content);
    }
    
    @Override
     public BuildStep content(String content) {
        this.content = content;
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
    private CopyOfBuilder(String id, String content) {
      super(id, content);
      
    }
    
    @Override
     public CopyOfBuilder content(String content) {
      return (CopyOfBuilder) super.content(content);
    }
  }
  

  public static class HasOneChildIdentifier extends ModelIdentifier<HasOneChild> {
    private static final long serialVersionUID = 1L;
    public HasOneChildIdentifier(String id) {
      super(id);
    }
  }
  
}
