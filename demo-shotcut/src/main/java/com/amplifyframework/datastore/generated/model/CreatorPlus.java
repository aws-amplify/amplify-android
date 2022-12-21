package com.amplifyframework.datastore.generated.model;

import com.amplifyframework.core.model.temporal.Temporal;

import java.util.List;
import java.util.UUID;
import java.util.Objects;

import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.model.AuthStrategy;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelOperation;
import com.amplifyframework.core.model.annotations.AuthRule;
import com.amplifyframework.core.model.annotations.Index;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.core.model.annotations.ModelField;
import com.amplifyframework.core.model.query.predicate.QueryField;

import static com.amplifyframework.core.model.query.predicate.QueryField.field;

/** This is an auto generated class representing the CreatorPlus type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "CreatorPluses", authRules = {
  @AuthRule(allow = AuthStrategy.PRIVATE, provider = "iam", operations = { ModelOperation.CREATE, ModelOperation.UPDATE, ModelOperation.DELETE, ModelOperation.READ }),
  @AuthRule(allow = AuthStrategy.PUBLIC, provider = "iam", operations = { ModelOperation.READ })
})
public final class CreatorPlus implements Model {
  public static final QueryField ID = field("CreatorPlus", "id");
  public static final QueryField ENTITLEMENT_ID = field("CreatorPlus", "entitlement_id");
  public static final QueryField UPDATED_AT = field("CreatorPlus", "updatedAt");
  public static final QueryField ENTITLEMENTS = field("CreatorPlus", "entitlements");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String") String entitlement_id;
  private final @ModelField(targetType="AWSDateTime") Temporal.DateTime updatedAt;
  private final @ModelField(targetType="String") String entitlements;
  public String getId() {
      return id;
  }
  
  public String getEntitlementId() {
      return entitlement_id;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  public String getEntitlements() {
      return entitlements;
  }
  
  private CreatorPlus(String id, String entitlement_id, Temporal.DateTime updatedAt, String entitlements) {
    this.id = id;
    this.entitlement_id = entitlement_id;
    this.updatedAt = updatedAt;
    this.entitlements = entitlements;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      CreatorPlus creatorPlus = (CreatorPlus) obj;
      return ObjectsCompat.equals(getId(), creatorPlus.getId()) &&
              ObjectsCompat.equals(getEntitlementId(), creatorPlus.getEntitlementId()) &&
              ObjectsCompat.equals(getUpdatedAt(), creatorPlus.getUpdatedAt()) &&
              ObjectsCompat.equals(getEntitlements(), creatorPlus.getEntitlements());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getEntitlementId())
      .append(getUpdatedAt())
      .append(getEntitlements())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("CreatorPlus {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("entitlement_id=" + String.valueOf(getEntitlementId()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()) + ", ")
      .append("entitlements=" + String.valueOf(getEntitlements()))
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
  public static CreatorPlus justId(String id) {
    return new CreatorPlus(
      id,
      null,
      null,
      null
    );
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(id,
      entitlement_id,
      updatedAt,
      entitlements);
  }
  public interface BuildStep {
    CreatorPlus build();
    BuildStep id(String id);
    BuildStep entitlementId(String entitlementId);
    BuildStep updatedAt(Temporal.DateTime updatedAt);
    BuildStep entitlements(String entitlements);
  }
  

  public static class Builder implements BuildStep {
    private String id;
    private String entitlement_id;
    private Temporal.DateTime updatedAt;
    private String entitlements;
    @Override
     public CreatorPlus build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new CreatorPlus(
          id,
          entitlement_id,
          updatedAt,
          entitlements);
    }
    
    @Override
     public BuildStep entitlementId(String entitlementId) {
        this.entitlement_id = entitlementId;
        return this;
    }
    
    @Override
     public BuildStep updatedAt(Temporal.DateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }
    
    @Override
     public BuildStep entitlements(String entitlements) {
        this.entitlements = entitlements;
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
    private CopyOfBuilder(String id, String entitlementId, Temporal.DateTime updatedAt, String entitlements) {
      super.id(id);
      super.entitlementId(entitlementId)
        .updatedAt(updatedAt)
        .entitlements(entitlements);
    }
    
    @Override
     public CopyOfBuilder entitlementId(String entitlementId) {
      return (CopyOfBuilder) super.entitlementId(entitlementId);
    }
    
    @Override
     public CopyOfBuilder updatedAt(Temporal.DateTime updatedAt) {
      return (CopyOfBuilder) super.updatedAt(updatedAt);
    }
    
    @Override
     public CopyOfBuilder entitlements(String entitlements) {
      return (CopyOfBuilder) super.entitlements(entitlements);
    }
  }
  
}
