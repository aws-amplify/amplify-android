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

/** This is an auto generated class representing the OverlayMediaCategoryLocale type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "OverlayMediaCategoryLocales", authRules = {
  @AuthRule(allow = AuthStrategy.PRIVATE, provider = "iam", operations = { ModelOperation.CREATE, ModelOperation.UPDATE, ModelOperation.DELETE, ModelOperation.READ }),
  @AuthRule(allow = AuthStrategy.PUBLIC, provider = "iam", operations = { ModelOperation.READ })
})
@Index(name = "byOverlayMediaCategory", fields = {"materialID"})
public final class OverlayMediaCategoryLocale implements Model {
  public static final QueryField ID = field("OverlayMediaCategoryLocale", "id");
  public static final QueryField NAME = field("OverlayMediaCategoryLocale", "name");
  public static final QueryField LOCALE = field("OverlayMediaCategoryLocale", "locale");
  public static final QueryField UPDATED_AT = field("OverlayMediaCategoryLocale", "updatedAt");
  public static final QueryField MATERIAL_ID = field("OverlayMediaCategoryLocale", "materialID");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String") String name;
  private final @ModelField(targetType="String") String locale;
  private final @ModelField(targetType="AWSDateTime") Temporal.DateTime updatedAt;
  private final @ModelField(targetType="ID") String materialID;
  public String getId() {
      return id;
  }
  
  public String getName() {
      return name;
  }
  
  public String getLocale() {
      return locale;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  public String getMaterialId() {
      return materialID;
  }
  
  private OverlayMediaCategoryLocale(String id, String name, String locale, Temporal.DateTime updatedAt, String materialID) {
    this.id = id;
    this.name = name;
    this.locale = locale;
    this.updatedAt = updatedAt;
    this.materialID = materialID;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      OverlayMediaCategoryLocale overlayMediaCategoryLocale = (OverlayMediaCategoryLocale) obj;
      return ObjectsCompat.equals(getId(), overlayMediaCategoryLocale.getId()) &&
              ObjectsCompat.equals(getName(), overlayMediaCategoryLocale.getName()) &&
              ObjectsCompat.equals(getLocale(), overlayMediaCategoryLocale.getLocale()) &&
              ObjectsCompat.equals(getUpdatedAt(), overlayMediaCategoryLocale.getUpdatedAt()) &&
              ObjectsCompat.equals(getMaterialId(), overlayMediaCategoryLocale.getMaterialId());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getName())
      .append(getLocale())
      .append(getUpdatedAt())
      .append(getMaterialId())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("OverlayMediaCategoryLocale {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("name=" + String.valueOf(getName()) + ", ")
      .append("locale=" + String.valueOf(getLocale()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()) + ", ")
      .append("materialID=" + String.valueOf(getMaterialId()))
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
  public static OverlayMediaCategoryLocale justId(String id) {
    return new OverlayMediaCategoryLocale(
      id,
      null,
      null,
      null,
      null
    );
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(id,
      name,
      locale,
      updatedAt,
      materialID);
  }
  public interface BuildStep {
    OverlayMediaCategoryLocale build();
    BuildStep id(String id);
    BuildStep name(String name);
    BuildStep locale(String locale);
    BuildStep updatedAt(Temporal.DateTime updatedAt);
    BuildStep materialId(String materialId);
  }
  

  public static class Builder implements BuildStep {
    private String id;
    private String name;
    private String locale;
    private Temporal.DateTime updatedAt;
    private String materialID;
    @Override
     public OverlayMediaCategoryLocale build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new OverlayMediaCategoryLocale(
          id,
          name,
          locale,
          updatedAt,
          materialID);
    }
    
    @Override
     public BuildStep name(String name) {
        this.name = name;
        return this;
    }
    
    @Override
     public BuildStep locale(String locale) {
        this.locale = locale;
        return this;
    }
    
    @Override
     public BuildStep updatedAt(Temporal.DateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }
    
    @Override
     public BuildStep materialId(String materialId) {
        this.materialID = materialId;
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
    private CopyOfBuilder(String id, String name, String locale, Temporal.DateTime updatedAt, String materialId) {
      super.id(id);
      super.name(name)
        .locale(locale)
        .updatedAt(updatedAt)
        .materialId(materialId);
    }
    
    @Override
     public CopyOfBuilder name(String name) {
      return (CopyOfBuilder) super.name(name);
    }
    
    @Override
     public CopyOfBuilder locale(String locale) {
      return (CopyOfBuilder) super.locale(locale);
    }
    
    @Override
     public CopyOfBuilder updatedAt(Temporal.DateTime updatedAt) {
      return (CopyOfBuilder) super.updatedAt(updatedAt);
    }
    
    @Override
     public CopyOfBuilder materialId(String materialId) {
      return (CopyOfBuilder) super.materialId(materialId);
    }
  }
  
}
