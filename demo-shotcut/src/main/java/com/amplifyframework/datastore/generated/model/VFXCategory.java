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

/** This is an auto generated class representing the VFXCategory type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "VFXCategories", authRules = {
  @AuthRule(allow = AuthStrategy.PUBLIC, provider = "iam", operations = { ModelOperation.READ }),
  @AuthRule(allow = AuthStrategy.GROUPS, groupClaim = "cognito:groups", groups = { "Editor" }, provider = "userPools", operations = { ModelOperation.READ, ModelOperation.CREATE, ModelOperation.UPDATE, ModelOperation.DELETE }),
  @AuthRule(allow = AuthStrategy.PRIVATE, provider = "iam", operations = { ModelOperation.CREATE, ModelOperation.UPDATE, ModelOperation.DELETE, ModelOperation.READ })
})
public final class VFXCategory implements Model {
  public static final QueryField ID = field("VFXCategory", "id");
  public static final QueryField NAME = field("VFXCategory", "name");
  public static final QueryField COVER_URL = field("VFXCategory", "coverUrl");
  public static final QueryField SORT = field("VFXCategory", "sort");
  public static final QueryField UPDATED_AT = field("VFXCategory", "updatedAt");
  public static final QueryField ONLINE = field("VFXCategory", "online");
  public static final QueryField DISPLAY_NAME = field("VFXCategory", "displayName");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String") String name;
  private final @ModelField(targetType="String") String coverUrl;
  private final @ModelField(targetType="Int") Integer sort;
  private final @ModelField(targetType="VFX") @HasMany(associatedWith = "vfxCategoryID", type = VFX.class) List<VFX> VfxSet = null;
  private final @ModelField(targetType="AWSDateTime", isRequired = true) Temporal.DateTime updatedAt;
  private final @ModelField(targetType="Int") Integer online;
  private final @ModelField(targetType="VFXCategoryLocale") @HasMany(associatedWith = "materialID", type = VFXCategoryLocale.class) List<VFXCategoryLocale> VFXCategoryLocales = null;
  private final @ModelField(targetType="String") String displayName;
  public String getId() {
      return id;
  }
  
  public String getName() {
      return name;
  }
  
  public String getCoverUrl() {
      return coverUrl;
  }
  
  public Integer getSort() {
      return sort;
  }
  
  public List<VFX> getVfxSet() {
      return VfxSet;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  public Integer getOnline() {
      return online;
  }
  
  public List<VFXCategoryLocale> getVfxCategoryLocales() {
      return VFXCategoryLocales;
  }
  
  public String getDisplayName() {
      return displayName;
  }
  
  private VFXCategory(String id, String name, String coverUrl, Integer sort, Temporal.DateTime updatedAt, Integer online, String displayName) {
    this.id = id;
    this.name = name;
    this.coverUrl = coverUrl;
    this.sort = sort;
    this.updatedAt = updatedAt;
    this.online = online;
    this.displayName = displayName;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      VFXCategory vfxCategory = (VFXCategory) obj;
      return ObjectsCompat.equals(getId(), vfxCategory.getId()) &&
              ObjectsCompat.equals(getName(), vfxCategory.getName()) &&
              ObjectsCompat.equals(getCoverUrl(), vfxCategory.getCoverUrl()) &&
              ObjectsCompat.equals(getSort(), vfxCategory.getSort()) &&
              ObjectsCompat.equals(getUpdatedAt(), vfxCategory.getUpdatedAt()) &&
              ObjectsCompat.equals(getOnline(), vfxCategory.getOnline()) &&
              ObjectsCompat.equals(getDisplayName(), vfxCategory.getDisplayName());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getName())
      .append(getCoverUrl())
      .append(getSort())
      .append(getUpdatedAt())
      .append(getOnline())
      .append(getDisplayName())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("VFXCategory {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("name=" + String.valueOf(getName()) + ", ")
      .append("coverUrl=" + String.valueOf(getCoverUrl()) + ", ")
      .append("sort=" + String.valueOf(getSort()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()) + ", ")
      .append("online=" + String.valueOf(getOnline()) + ", ")
      .append("displayName=" + String.valueOf(getDisplayName()))
      .append("}")
      .toString();
  }
  
  public static UpdatedAtStep builder() {
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
  public static VFXCategory justId(String id) {
    return new VFXCategory(
      id,
      null,
      null,
      null,
      null,
      null,
      null
    );
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(id,
      name,
      coverUrl,
      sort,
      updatedAt,
      online,
      displayName);
  }
  public interface UpdatedAtStep {
    BuildStep updatedAt(Temporal.DateTime updatedAt);
  }
  

  public interface BuildStep {
    VFXCategory build();
    BuildStep id(String id);
    BuildStep name(String name);
    BuildStep coverUrl(String coverUrl);
    BuildStep sort(Integer sort);
    BuildStep online(Integer online);
    BuildStep displayName(String displayName);
  }
  

  public static class Builder implements UpdatedAtStep, BuildStep {
    private String id;
    private Temporal.DateTime updatedAt;
    private String name;
    private String coverUrl;
    private Integer sort;
    private Integer online;
    private String displayName;
    @Override
     public VFXCategory build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new VFXCategory(
          id,
          name,
          coverUrl,
          sort,
          updatedAt,
          online,
          displayName);
    }
    
    @Override
     public BuildStep updatedAt(Temporal.DateTime updatedAt) {
        Objects.requireNonNull(updatedAt);
        this.updatedAt = updatedAt;
        return this;
    }
    
    @Override
     public BuildStep name(String name) {
        this.name = name;
        return this;
    }
    
    @Override
     public BuildStep coverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
        return this;
    }
    
    @Override
     public BuildStep sort(Integer sort) {
        this.sort = sort;
        return this;
    }
    
    @Override
     public BuildStep online(Integer online) {
        this.online = online;
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
    private CopyOfBuilder(String id, String name, String coverUrl, Integer sort, Temporal.DateTime updatedAt, Integer online, String displayName) {
      super.id(id);
      super.updatedAt(updatedAt)
        .name(name)
        .coverUrl(coverUrl)
        .sort(sort)
        .online(online)
        .displayName(displayName);
    }
    
    @Override
     public CopyOfBuilder updatedAt(Temporal.DateTime updatedAt) {
      return (CopyOfBuilder) super.updatedAt(updatedAt);
    }
    
    @Override
     public CopyOfBuilder name(String name) {
      return (CopyOfBuilder) super.name(name);
    }
    
    @Override
     public CopyOfBuilder coverUrl(String coverUrl) {
      return (CopyOfBuilder) super.coverUrl(coverUrl);
    }
    
    @Override
     public CopyOfBuilder sort(Integer sort) {
      return (CopyOfBuilder) super.sort(sort);
    }
    
    @Override
     public CopyOfBuilder online(Integer online) {
      return (CopyOfBuilder) super.online(online);
    }
    
    @Override
     public CopyOfBuilder displayName(String displayName) {
      return (CopyOfBuilder) super.displayName(displayName);
    }
  }
  
}
