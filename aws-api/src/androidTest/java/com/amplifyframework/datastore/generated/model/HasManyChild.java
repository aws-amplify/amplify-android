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

import java.util.UUID;

/** This is an auto generated class representing the HasManyChild type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "HasManyChildren", type = Model.Type.USER, version = 1, hasLazySupport = true)
@Index(name = "undefined", fields = {"id"})
public final class HasManyChild implements Model {
  public static final HasManyChildPath rootPath = new HasManyChildPath("root", false, null);
  public static final QueryField ID = field("HasManyChild", "id");
  public static final QueryField CONTENT = field("HasManyChild", "content");
  public static final QueryField PARENT = field("HasManyChild", "parentChildrenId");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String") String content;
  private final @ModelField(targetType="Parent") @BelongsTo(targetName = "parentChildrenId", targetNames = {"parentChildrenId"}, type = Parent.class) ModelReference<Parent> parent;
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
  
  public ModelReference<Parent> getParent() {
      return parent;
  }
  
  public Temporal.DateTime getCreatedAt() {
      return createdAt;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  private HasManyChild(String id, String content, ModelReference<Parent> parent) {
    this.id = id;
    this.content = content;
    this.parent = parent;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      HasManyChild hasManyChild = (HasManyChild) obj;
      return ObjectsCompat.equals(getId(), hasManyChild.getId()) &&
              ObjectsCompat.equals(getContent(), hasManyChild.getContent()) &&
              ObjectsCompat.equals(getParent(), hasManyChild.getParent()) &&
              ObjectsCompat.equals(getCreatedAt(), hasManyChild.getCreatedAt()) &&
              ObjectsCompat.equals(getUpdatedAt(), hasManyChild.getUpdatedAt());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getContent())
      .append(getParent())
      .append(getCreatedAt())
      .append(getUpdatedAt())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("HasManyChild {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("content=" + String.valueOf(getContent()) + ", ")
      .append("parent=" + String.valueOf(getParent()) + ", ")
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
  public static HasManyChild justId(String id) {
    return new HasManyChild(
      id,
      null,
      null
    );
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(id,
      content,
      parent);
  }
  public interface BuildStep {
    HasManyChild build();
    BuildStep id(String id);
    BuildStep content(String content);
    BuildStep parent(Parent parent);
  }
  

  public static class Builder implements BuildStep {
    private String id;
    private String content;
    private ModelReference<Parent> parent;
    public Builder() {
      
    }
    
    private Builder(String id, String content, ModelReference<Parent> parent) {
      this.id = id;
      this.content = content;
      this.parent = parent;
    }
    
    @Override
     public HasManyChild build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new HasManyChild(
          id,
          content,
          parent);
    }
    
    @Override
     public BuildStep content(String content) {
        this.content = content;
        return this;
    }
    
    @Override
     public BuildStep parent(Parent parent) {
        this.parent = new LoadedModelReferenceImpl<>(parent);
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
    private CopyOfBuilder(String id, String content, ModelReference<Parent> parent) {
      super(id, content, parent);
      
    }
    
    @Override
     public CopyOfBuilder content(String content) {
      return (CopyOfBuilder) super.content(content);
    }
    
    @Override
     public CopyOfBuilder parent(Parent parent) {
      return (CopyOfBuilder) super.parent(parent);
    }
  }
  

  public static class HasManyChildIdentifier extends ModelIdentifier<HasManyChild> {
    private static final long serialVersionUID = 1L;
    public HasManyChildIdentifier(String id) {
      super(id);
    }
  }
  
}
