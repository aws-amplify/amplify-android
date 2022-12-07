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

/** This is an auto generated class representing the VideoFilterCategoryLocale type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "VideoFilterCategoryLocales", authRules = {
  @AuthRule(allow = AuthStrategy.PUBLIC, provider = "iam", operations = { ModelOperation.READ }),
  @AuthRule(allow = AuthStrategy.GROUPS, groupClaim = "cognito:groups", groups = { "Editor" }, provider = "userPools", operations = { ModelOperation.READ, ModelOperation.CREATE, ModelOperation.UPDATE, ModelOperation.DELETE }),
  @AuthRule(allow = AuthStrategy.PRIVATE, provider = "iam", operations = { ModelOperation.CREATE, ModelOperation.UPDATE, ModelOperation.DELETE, ModelOperation.READ })
})
@Index(name = "byVideoFilterCategory", fields = {"materialID"})
public final class VideoFilterCategoryLocale implements Model {
  public static final QueryField ID = field("VideoFilterCategoryLocale", "id");
  public static final QueryField LOCALE = field("VideoFilterCategoryLocale", "locale");
  public static final QueryField NAME = field("VideoFilterCategoryLocale", "name");
  public static final QueryField UPDATED_AT = field("VideoFilterCategoryLocale", "updatedAt");
  public static final QueryField MATERIAL_ID = field("VideoFilterCategoryLocale", "materialID");
  public static final QueryField SORT = field("VideoFilterCategoryLocale", "sort");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String") String locale;
  private final @ModelField(targetType="String") String name;
  private final @ModelField(targetType="AWSDateTime") Temporal.DateTime updatedAt;
  private final @ModelField(targetType="ID") String materialID;
  private final @ModelField(targetType="Int") Integer sort;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime createdAt;
  public String getId() {
      return id;
  }
  
  public String getLocale() {
      return locale;
  }
  
  public String getName() {
      return name;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  public String getMaterialId() {
      return materialID;
  }
  
  public Integer getSort() {
      return sort;
  }
  
  public Temporal.DateTime getCreatedAt() {
      return createdAt;
  }
  
  private VideoFilterCategoryLocale(String id, String locale, String name, Temporal.DateTime updatedAt, String materialID, Integer sort) {
    this.id = id;
    this.locale = locale;
    this.name = name;
    this.updatedAt = updatedAt;
    this.materialID = materialID;
    this.sort = sort;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      VideoFilterCategoryLocale videoFilterCategoryLocale = (VideoFilterCategoryLocale) obj;
      return ObjectsCompat.equals(getId(), videoFilterCategoryLocale.getId()) &&
              ObjectsCompat.equals(getLocale(), videoFilterCategoryLocale.getLocale()) &&
              ObjectsCompat.equals(getName(), videoFilterCategoryLocale.getName()) &&
              ObjectsCompat.equals(getUpdatedAt(), videoFilterCategoryLocale.getUpdatedAt()) &&
              ObjectsCompat.equals(getMaterialId(), videoFilterCategoryLocale.getMaterialId()) &&
              ObjectsCompat.equals(getSort(), videoFilterCategoryLocale.getSort()) &&
              ObjectsCompat.equals(getCreatedAt(), videoFilterCategoryLocale.getCreatedAt());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getLocale())
      .append(getName())
      .append(getUpdatedAt())
      .append(getMaterialId())
      .append(getSort())
      .append(getCreatedAt())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("VideoFilterCategoryLocale {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("locale=" + String.valueOf(getLocale()) + ", ")
      .append("name=" + String.valueOf(getName()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()) + ", ")
      .append("materialID=" + String.valueOf(getMaterialId()) + ", ")
      .append("sort=" + String.valueOf(getSort()) + ", ")
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
   */
  public static VideoFilterCategoryLocale justId(String id) {
    return new VideoFilterCategoryLocale(
      id,
      null,
      null,
      null,
      null,
      null
    );
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(id,
      locale,
      name,
      updatedAt,
      materialID,
      sort);
  }
  public interface BuildStep {
    VideoFilterCategoryLocale build();
    BuildStep id(String id);
    BuildStep locale(String locale);
    BuildStep name(String name);
    BuildStep updatedAt(Temporal.DateTime updatedAt);
    BuildStep materialId(String materialId);
    BuildStep sort(Integer sort);
  }
  

  public static class Builder implements BuildStep {
    private String id;
    private String locale;
    private String name;
    private Temporal.DateTime updatedAt;
    private String materialID;
    private Integer sort;
    @Override
     public VideoFilterCategoryLocale build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new VideoFilterCategoryLocale(
          id,
          locale,
          name,
          updatedAt,
          materialID,
          sort);
    }
    
    @Override
     public BuildStep locale(String locale) {
        this.locale = locale;
        return this;
    }
    
    @Override
     public BuildStep name(String name) {
        this.name = name;
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
    
    @Override
     public BuildStep sort(Integer sort) {
        this.sort = sort;
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
    private CopyOfBuilder(String id, String locale, String name, Temporal.DateTime updatedAt, String materialId, Integer sort) {
      super.id(id);
      super.locale(locale)
        .name(name)
        .updatedAt(updatedAt)
        .materialId(materialId)
        .sort(sort);
    }
    
    @Override
     public CopyOfBuilder locale(String locale) {
      return (CopyOfBuilder) super.locale(locale);
    }
    
    @Override
     public CopyOfBuilder name(String name) {
      return (CopyOfBuilder) super.name(name);
    }
    
    @Override
     public CopyOfBuilder updatedAt(Temporal.DateTime updatedAt) {
      return (CopyOfBuilder) super.updatedAt(updatedAt);
    }
    
    @Override
     public CopyOfBuilder materialId(String materialId) {
      return (CopyOfBuilder) super.materialId(materialId);
    }
    
    @Override
     public CopyOfBuilder sort(Integer sort) {
      return (CopyOfBuilder) super.sort(sort);
    }
  }
  
}
