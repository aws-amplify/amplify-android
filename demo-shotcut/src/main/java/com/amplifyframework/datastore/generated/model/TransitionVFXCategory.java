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

/** This is an auto generated class representing the TransitionVFXCategory type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "TransitionVFXCategories", authRules = {
  @AuthRule(allow = AuthStrategy.PUBLIC, provider = "iam", operations = { ModelOperation.READ }),
  @AuthRule(allow = AuthStrategy.GROUPS, groupClaim = "cognito:groups", groups = { "Editor" }, provider = "userPools", operations = { ModelOperation.READ, ModelOperation.CREATE, ModelOperation.UPDATE, ModelOperation.DELETE }),
  @AuthRule(allow = AuthStrategy.PRIVATE, provider = "iam", operations = { ModelOperation.CREATE, ModelOperation.UPDATE, ModelOperation.DELETE, ModelOperation.READ })
})
public final class TransitionVFXCategory implements Model {
  public static final QueryField ID = field("TransitionVFXCategory", "id");
  public static final QueryField NAME = field("TransitionVFXCategory", "name");
  public static final QueryField COVER_URL = field("TransitionVFXCategory", "coverUrl");
  public static final QueryField SORT = field("TransitionVFXCategory", "sort");
  public static final QueryField ONLINE = field("TransitionVFXCategory", "online");
  public static final QueryField UPDATED_AT = field("TransitionVFXCategory", "updatedAt");
  public static final QueryField GET_METHOD = field("TransitionVFXCategory", "getMethod");
  public static final QueryField DISPLAY_NAME = field("TransitionVFXCategory", "displayName");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String") String name;
  private final @ModelField(targetType="String") String coverUrl;
  private final @ModelField(targetType="Int") Integer sort;
  private final @ModelField(targetType="Int") Integer online;
  private final @ModelField(targetType="TransitionVFX") @HasMany(associatedWith = "transitionVfxCategoryID", type = TransitionVFX.class) List<TransitionVFX> TransitionVFXSet = null;
  private final @ModelField(targetType="AWSDateTime", isRequired = true) Temporal.DateTime updatedAt;
  private final @ModelField(targetType="Int") Integer getMethod;
  private final @ModelField(targetType="TransitionVFXCategoryLocale") @HasMany(associatedWith = "materialID", type = TransitionVFXCategoryLocale.class) List<TransitionVFXCategoryLocale> TransitionVFXCategoryLocales = null;
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
  
  public Integer getOnline() {
      return online;
  }
  
  public List<TransitionVFX> getTransitionVfxSet() {
      return TransitionVFXSet;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  public Integer getGetMethod() {
      return getMethod;
  }
  
  public List<TransitionVFXCategoryLocale> getTransitionVfxCategoryLocales() {
      return TransitionVFXCategoryLocales;
  }
  
  public String getDisplayName() {
      return displayName;
  }
  
  private TransitionVFXCategory(String id, String name, String coverUrl, Integer sort, Integer online, Temporal.DateTime updatedAt, Integer getMethod, String displayName) {
    this.id = id;
    this.name = name;
    this.coverUrl = coverUrl;
    this.sort = sort;
    this.online = online;
    this.updatedAt = updatedAt;
    this.getMethod = getMethod;
    this.displayName = displayName;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      TransitionVFXCategory transitionVfxCategory = (TransitionVFXCategory) obj;
      return ObjectsCompat.equals(getId(), transitionVfxCategory.getId()) &&
              ObjectsCompat.equals(getName(), transitionVfxCategory.getName()) &&
              ObjectsCompat.equals(getCoverUrl(), transitionVfxCategory.getCoverUrl()) &&
              ObjectsCompat.equals(getSort(), transitionVfxCategory.getSort()) &&
              ObjectsCompat.equals(getOnline(), transitionVfxCategory.getOnline()) &&
              ObjectsCompat.equals(getUpdatedAt(), transitionVfxCategory.getUpdatedAt()) &&
              ObjectsCompat.equals(getGetMethod(), transitionVfxCategory.getGetMethod()) &&
              ObjectsCompat.equals(getDisplayName(), transitionVfxCategory.getDisplayName());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getName())
      .append(getCoverUrl())
      .append(getSort())
      .append(getOnline())
      .append(getUpdatedAt())
      .append(getGetMethod())
      .append(getDisplayName())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("TransitionVFXCategory {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("name=" + String.valueOf(getName()) + ", ")
      .append("coverUrl=" + String.valueOf(getCoverUrl()) + ", ")
      .append("sort=" + String.valueOf(getSort()) + ", ")
      .append("online=" + String.valueOf(getOnline()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()) + ", ")
      .append("getMethod=" + String.valueOf(getGetMethod()) + ", ")
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
  public static TransitionVFXCategory justId(String id) {
    return new TransitionVFXCategory(
      id,
      null,
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
      online,
      updatedAt,
      getMethod,
      displayName);
  }
  public interface UpdatedAtStep {
    BuildStep updatedAt(Temporal.DateTime updatedAt);
  }
  

  public interface BuildStep {
    TransitionVFXCategory build();
    BuildStep id(String id);
    BuildStep name(String name);
    BuildStep coverUrl(String coverUrl);
    BuildStep sort(Integer sort);
    BuildStep online(Integer online);
    BuildStep getMethod(Integer getMethod);
    BuildStep displayName(String displayName);
  }
  

  public static class Builder implements UpdatedAtStep, BuildStep {
    private String id;
    private Temporal.DateTime updatedAt;
    private String name;
    private String coverUrl;
    private Integer sort;
    private Integer online;
    private Integer getMethod;
    private String displayName;
    @Override
     public TransitionVFXCategory build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new TransitionVFXCategory(
          id,
          name,
          coverUrl,
          sort,
          online,
          updatedAt,
          getMethod,
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
     public BuildStep getMethod(Integer getMethod) {
        this.getMethod = getMethod;
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
    private CopyOfBuilder(String id, String name, String coverUrl, Integer sort, Integer online, Temporal.DateTime updatedAt, Integer getMethod, String displayName) {
      super.id(id);
      super.updatedAt(updatedAt)
        .name(name)
        .coverUrl(coverUrl)
        .sort(sort)
        .online(online)
        .getMethod(getMethod)
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
     public CopyOfBuilder getMethod(Integer getMethod) {
      return (CopyOfBuilder) super.getMethod(getMethod);
    }
    
    @Override
     public CopyOfBuilder displayName(String displayName) {
      return (CopyOfBuilder) super.displayName(displayName);
    }
  }
  
}
