package com.amplifyframework.datastore.generated.model;

import static com.amplifyframework.core.model.query.predicate.QueryField.field;

import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelIdentifier;
import com.amplifyframework.core.model.ModelList;
import com.amplifyframework.core.model.ModelReference;
import com.amplifyframework.core.model.annotations.HasMany;
import com.amplifyframework.core.model.annotations.HasOne;
import com.amplifyframework.core.model.annotations.Index;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.core.model.annotations.ModelField;
import com.amplifyframework.core.model.query.predicate.QueryField;
import com.amplifyframework.core.model.temporal.Temporal;

import java.util.UUID;

/** This is an auto generated class representing the Parent type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "Parents", type = Model.Type.USER, version = 1, hasLazySupport = true)
@Index(name = "undefined", fields = {"id"})
public final class Parent implements Model {
  public static final ParentPath rootPath = new ParentPath("root", false, null);
  public static final QueryField ID = field("Parent", "id");
  public static final QueryField PARENT_CHILD_ID = field("Parent", "parentChildId");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="HasOneChild") @HasOne(associatedWith = "id", targetNames = {"parentChildId"}, type = HasOneChild.class) ModelReference<HasOneChild> child = null;
  private final @ModelField(targetType="HasManyChild") @HasMany(associatedWith = "parent", type = HasManyChild.class) ModelList<HasManyChild> children = null;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime createdAt;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime updatedAt;
  private final @ModelField(targetType="ID") String parentChildId;
  /** @deprecated This API is internal to Amplify and should not be used. */
  @Deprecated
   public String resolveIdentifier() {
    return id;
  }
  
  public String getId() {
      return id;
  }
  
  public ModelReference<HasOneChild> getChild() {
      return child;
  }
  
  public ModelList<HasManyChild> getChildren() {
      return children;
  }
  
  public Temporal.DateTime getCreatedAt() {
      return createdAt;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  public String getParentChildId() {
      return parentChildId;
  }
  
  private Parent(String id, String parentChildId) {
    this.id = id;
    this.parentChildId = parentChildId;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      Parent parent = (Parent) obj;
      return ObjectsCompat.equals(getId(), parent.getId()) &&
              ObjectsCompat.equals(getCreatedAt(), parent.getCreatedAt()) &&
              ObjectsCompat.equals(getUpdatedAt(), parent.getUpdatedAt()) &&
              ObjectsCompat.equals(getParentChildId(), parent.getParentChildId());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getCreatedAt())
      .append(getUpdatedAt())
      .append(getParentChildId())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("Parent {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("createdAt=" + String.valueOf(getCreatedAt()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()) + ", ")
      .append("parentChildId=" + String.valueOf(getParentChildId()))
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
  public static Parent justId(String id) {
    return new Parent(
      id,
      null
    );
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(id,
      parentChildId);
  }
  public interface BuildStep {
    Parent build();
    BuildStep id(String id);
    BuildStep parentChildId(String parentChildId);
  }
  

  public static class Builder implements BuildStep {
    private String id;
    private String parentChildId;
    public Builder() {
      
    }
    
    private Builder(String id, String parentChildId) {
      this.id = id;
      this.parentChildId = parentChildId;
    }
    
    @Override
     public Parent build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new Parent(
          id,
          parentChildId);
    }
    
    @Override
     public BuildStep parentChildId(String parentChildId) {
        this.parentChildId = parentChildId;
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
    private CopyOfBuilder(String id, String parentChildId) {
      super(id, parentChildId);
      
    }
    
    @Override
     public CopyOfBuilder parentChildId(String parentChildId) {
      return (CopyOfBuilder) super.parentChildId(parentChildId);
    }
  }
  

  public static class ParentIdentifier extends ModelIdentifier<Parent> {
    private static final long serialVersionUID = 1L;
    public ParentIdentifier(String id) {
      super(id);
    }
  }
  
}
