package com.amplifyframework.datastore.generated.model;

import com.amplifyframework.core.model.annotations.HasMany;
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

/** This is an auto generated class representing the OverlayMediaCategory type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "OverlayMediaCategories", authRules = {
  @AuthRule(allow = AuthStrategy.PUBLIC, provider = "iam", operations = { ModelOperation.READ }),
  @AuthRule(allow = AuthStrategy.PRIVATE, provider = "iam", operations = { ModelOperation.CREATE, ModelOperation.UPDATE, ModelOperation.DELETE, ModelOperation.READ })
})
public final class OverlayMediaCategory implements Model {
  public static final QueryField ID = field("OverlayMediaCategory", "id");
  public static final QueryField NAME = field("OverlayMediaCategory", "name");
  public static final QueryField SORT = field("OverlayMediaCategory", "sort");
  public static final QueryField UPDATED_AT = field("OverlayMediaCategory", "updatedAt");
  public static final QueryField DISPLAY_NAME = field("OverlayMediaCategory", "displayName");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String") String name;
  private final @ModelField(targetType="Int") Integer sort;
  private final @ModelField(targetType="OverlayMedia") @HasMany(associatedWith = "overlaymediacategoryID", type = OverlayMedia.class) List<OverlayMedia> OverlayMedias = null;
  private final @ModelField(targetType="AWSDateTime") Temporal.DateTime updatedAt;
  private final @ModelField(targetType="OverlayMediaCategoryLocale") @HasMany(associatedWith = "materialID", type = OverlayMediaCategoryLocale.class) List<OverlayMediaCategoryLocale> OverlayMediaCategoryLocales = null;
  private final @ModelField(targetType="String") String displayName;
  public String getId() {
      return id;
  }
  
  public String getName() {
      return name;
  }
  
  public Integer getSort() {
      return sort;
  }
  
  public List<OverlayMedia> getOverlayMedias() {
      return OverlayMedias;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  public List<OverlayMediaCategoryLocale> getOverlayMediaCategoryLocales() {
      return OverlayMediaCategoryLocales;
  }
  
  public String getDisplayName() {
      return displayName;
  }
  
  private OverlayMediaCategory(String id, String name, Integer sort, Temporal.DateTime updatedAt, String displayName) {
    this.id = id;
    this.name = name;
    this.sort = sort;
    this.updatedAt = updatedAt;
    this.displayName = displayName;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      OverlayMediaCategory overlayMediaCategory = (OverlayMediaCategory) obj;
      return ObjectsCompat.equals(getId(), overlayMediaCategory.getId()) &&
              ObjectsCompat.equals(getName(), overlayMediaCategory.getName()) &&
              ObjectsCompat.equals(getSort(), overlayMediaCategory.getSort()) &&
              ObjectsCompat.equals(getUpdatedAt(), overlayMediaCategory.getUpdatedAt()) &&
              ObjectsCompat.equals(getDisplayName(), overlayMediaCategory.getDisplayName());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getName())
      .append(getSort())
      .append(getUpdatedAt())
      .append(getDisplayName())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("OverlayMediaCategory {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("name=" + String.valueOf(getName()) + ", ")
      .append("sort=" + String.valueOf(getSort()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()) + ", ")
      .append("displayName=" + String.valueOf(getDisplayName()))
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
  public static OverlayMediaCategory justId(String id) {
    return new OverlayMediaCategory(
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
      sort,
      updatedAt,
      displayName);
  }
  public interface BuildStep {
    OverlayMediaCategory build();
    BuildStep id(String id);
    BuildStep name(String name);
    BuildStep sort(Integer sort);
    BuildStep updatedAt(Temporal.DateTime updatedAt);
    BuildStep displayName(String displayName);
  }
  

  public static class Builder implements BuildStep {
    private String id;
    private String name;
    private Integer sort;
    private Temporal.DateTime updatedAt;
    private String displayName;
    @Override
     public OverlayMediaCategory build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new OverlayMediaCategory(
          id,
          name,
          sort,
          updatedAt,
          displayName);
    }
    
    @Override
     public BuildStep name(String name) {
        this.name = name;
        return this;
    }
    
    @Override
     public BuildStep sort(Integer sort) {
        this.sort = sort;
        return this;
    }
    
    @Override
     public BuildStep updatedAt(Temporal.DateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }
    
    @Override
     public BuildStep displayName(String displayName) {
        this.displayName = displayName;
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
    private CopyOfBuilder(String id, String name, Integer sort, Temporal.DateTime updatedAt, String displayName) {
      super.id(id);
      super.name(name)
        .sort(sort)
        .updatedAt(updatedAt)
        .displayName(displayName);
    }
    
    @Override
     public CopyOfBuilder name(String name) {
      return (CopyOfBuilder) super.name(name);
    }
    
    @Override
     public CopyOfBuilder sort(Integer sort) {
      return (CopyOfBuilder) super.sort(sort);
    }
    
    @Override
     public CopyOfBuilder updatedAt(Temporal.DateTime updatedAt) {
      return (CopyOfBuilder) super.updatedAt(updatedAt);
    }
    
    @Override
     public CopyOfBuilder displayName(String displayName) {
      return (CopyOfBuilder) super.displayName(displayName);
    }
  }
  
}
