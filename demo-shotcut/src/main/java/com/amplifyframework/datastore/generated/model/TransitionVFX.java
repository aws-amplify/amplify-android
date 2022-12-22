package com.amplifyframework.datastore.generated.model;

import com.amplifyframework.core.model.temporal.Temporal;
import com.amplifyframework.core.model.annotations.HasMany;

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

/** This is an auto generated class representing the TransitionVFX type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "TransitionVFXES", authRules = {
  @AuthRule(allow = AuthStrategy.PUBLIC, provider = "iam", operations = { ModelOperation.READ }),
  @AuthRule(allow = AuthStrategy.GROUPS, groupClaim = "cognito:groups", groups = { "Editor" }, provider = "userPools", operations = { ModelOperation.READ, ModelOperation.CREATE, ModelOperation.UPDATE, ModelOperation.DELETE }),
  @AuthRule(allow = AuthStrategy.PRIVATE, provider = "iam", operations = { ModelOperation.CREATE, ModelOperation.UPDATE, ModelOperation.DELETE, ModelOperation.READ })
})
@Index(name = "byTransitionVFXCategory", fields = {"transitionVfxCategoryID"})
public final class TransitionVFX implements Model {
  public static final QueryField ID = field("TransitionVFX", "id");
  public static final QueryField NAME = field("TransitionVFX", "name");
  public static final QueryField COVER_URL = field("TransitionVFX", "coverUrl");
  public static final QueryField DOWNLOAD_URL = field("TransitionVFX", "downloadUrl");
  public static final QueryField SORT = field("TransitionVFX", "sort");
  public static final QueryField VFX_ENGINE_MIN_VERSION_CODE = field("TransitionVFX", "vfxEngineMinVersionCode");
  public static final QueryField ONLINE = field("TransitionVFX", "online");
  public static final QueryField TRANSITION_VFX_CATEGORY_ID = field("TransitionVFX", "transitionVfxCategoryID");
  public static final QueryField UPDATED_AT = field("TransitionVFX", "updatedAt");
  public static final QueryField GET_METHOD = field("TransitionVFX", "getMethod");
  public static final QueryField DISPLAY_NAME = field("TransitionVFX", "displayName");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String") String name;
  private final @ModelField(targetType="String") String coverUrl;
  private final @ModelField(targetType="String") String downloadUrl;
  private final @ModelField(targetType="Int") Integer sort;
  private final @ModelField(targetType="Int") Integer vfxEngineMinVersionCode;
  private final @ModelField(targetType="Int") Integer online;
  private final @ModelField(targetType="ID") String transitionVfxCategoryID;
  private final @ModelField(targetType="AWSDateTime", isRequired = true) Temporal.DateTime updatedAt;
  private final @ModelField(targetType="Int") Integer getMethod;
  private final @ModelField(targetType="TransitionVFXLocale") @HasMany(associatedWith = "materialID", type = TransitionVFXLocale.class) List<TransitionVFXLocale> TransitionVFXLocales = null;
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
  
  public String getDownloadUrl() {
      return downloadUrl;
  }
  
  public Integer getSort() {
      return sort;
  }
  
  public Integer getVfxEngineMinVersionCode() {
      return vfxEngineMinVersionCode;
  }
  
  public Integer getOnline() {
      return online;
  }
  
  public String getTransitionVfxCategoryId() {
      return transitionVfxCategoryID;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  public Integer getGetMethod() {
      return getMethod;
  }
  
  public List<TransitionVFXLocale> getTransitionVfxLocales() {
      return TransitionVFXLocales;
  }
  
  public String getDisplayName() {
      return displayName;
  }
  
  private TransitionVFX(String id, String name, String coverUrl, String downloadUrl, Integer sort, Integer vfxEngineMinVersionCode, Integer online, String transitionVfxCategoryID, Temporal.DateTime updatedAt, Integer getMethod, String displayName) {
    this.id = id;
    this.name = name;
    this.coverUrl = coverUrl;
    this.downloadUrl = downloadUrl;
    this.sort = sort;
    this.vfxEngineMinVersionCode = vfxEngineMinVersionCode;
    this.online = online;
    this.transitionVfxCategoryID = transitionVfxCategoryID;
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
      TransitionVFX transitionVfx = (TransitionVFX) obj;
      return ObjectsCompat.equals(getId(), transitionVfx.getId()) &&
              ObjectsCompat.equals(getName(), transitionVfx.getName()) &&
              ObjectsCompat.equals(getCoverUrl(), transitionVfx.getCoverUrl()) &&
              ObjectsCompat.equals(getDownloadUrl(), transitionVfx.getDownloadUrl()) &&
              ObjectsCompat.equals(getSort(), transitionVfx.getSort()) &&
              ObjectsCompat.equals(getVfxEngineMinVersionCode(), transitionVfx.getVfxEngineMinVersionCode()) &&
              ObjectsCompat.equals(getOnline(), transitionVfx.getOnline()) &&
              ObjectsCompat.equals(getTransitionVfxCategoryId(), transitionVfx.getTransitionVfxCategoryId()) &&
              ObjectsCompat.equals(getUpdatedAt(), transitionVfx.getUpdatedAt()) &&
              ObjectsCompat.equals(getGetMethod(), transitionVfx.getGetMethod()) &&
              ObjectsCompat.equals(getDisplayName(), transitionVfx.getDisplayName());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getName())
      .append(getCoverUrl())
      .append(getDownloadUrl())
      .append(getSort())
      .append(getVfxEngineMinVersionCode())
      .append(getOnline())
      .append(getTransitionVfxCategoryId())
      .append(getUpdatedAt())
      .append(getGetMethod())
      .append(getDisplayName())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("TransitionVFX {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("name=" + String.valueOf(getName()) + ", ")
      .append("coverUrl=" + String.valueOf(getCoverUrl()) + ", ")
      .append("downloadUrl=" + String.valueOf(getDownloadUrl()) + ", ")
      .append("sort=" + String.valueOf(getSort()) + ", ")
      .append("vfxEngineMinVersionCode=" + String.valueOf(getVfxEngineMinVersionCode()) + ", ")
      .append("online=" + String.valueOf(getOnline()) + ", ")
      .append("transitionVfxCategoryID=" + String.valueOf(getTransitionVfxCategoryId()) + ", ")
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
  public static TransitionVFX justId(String id) {
    return new TransitionVFX(
      id,
      null,
      null,
      null,
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
      downloadUrl,
      sort,
      vfxEngineMinVersionCode,
      online,
      transitionVfxCategoryID,
      updatedAt,
      getMethod,
      displayName);
  }
  public interface UpdatedAtStep {
    BuildStep updatedAt(Temporal.DateTime updatedAt);
  }
  

  public interface BuildStep {
    TransitionVFX build();
    BuildStep id(String id);
    BuildStep name(String name);
    BuildStep coverUrl(String coverUrl);
    BuildStep downloadUrl(String downloadUrl);
    BuildStep sort(Integer sort);
    BuildStep vfxEngineMinVersionCode(Integer vfxEngineMinVersionCode);
    BuildStep online(Integer online);
    BuildStep transitionVfxCategoryId(String transitionVfxCategoryId);
    BuildStep getMethod(Integer getMethod);
    BuildStep displayName(String displayName);
  }
  

  public static class Builder implements UpdatedAtStep, BuildStep {
    private String id;
    private Temporal.DateTime updatedAt;
    private String name;
    private String coverUrl;
    private String downloadUrl;
    private Integer sort;
    private Integer vfxEngineMinVersionCode;
    private Integer online;
    private String transitionVfxCategoryID;
    private Integer getMethod;
    private String displayName;
    @Override
     public TransitionVFX build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new TransitionVFX(
          id,
          name,
          coverUrl,
          downloadUrl,
          sort,
          vfxEngineMinVersionCode,
          online,
          transitionVfxCategoryID,
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
     public BuildStep downloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
        return this;
    }
    
    @Override
     public BuildStep sort(Integer sort) {
        this.sort = sort;
        return this;
    }
    
    @Override
     public BuildStep vfxEngineMinVersionCode(Integer vfxEngineMinVersionCode) {
        this.vfxEngineMinVersionCode = vfxEngineMinVersionCode;
        return this;
    }
    
    @Override
     public BuildStep online(Integer online) {
        this.online = online;
        return this;
    }
    
    @Override
     public BuildStep transitionVfxCategoryId(String transitionVfxCategoryId) {
        this.transitionVfxCategoryID = transitionVfxCategoryId;
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
    private CopyOfBuilder(String id, String name, String coverUrl, String downloadUrl, Integer sort, Integer vfxEngineMinVersionCode, Integer online, String transitionVfxCategoryId, Temporal.DateTime updatedAt, Integer getMethod, String displayName) {
      super.id(id);
      super.updatedAt(updatedAt)
        .name(name)
        .coverUrl(coverUrl)
        .downloadUrl(downloadUrl)
        .sort(sort)
        .vfxEngineMinVersionCode(vfxEngineMinVersionCode)
        .online(online)
        .transitionVfxCategoryId(transitionVfxCategoryId)
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
     public CopyOfBuilder downloadUrl(String downloadUrl) {
      return (CopyOfBuilder) super.downloadUrl(downloadUrl);
    }
    
    @Override
     public CopyOfBuilder sort(Integer sort) {
      return (CopyOfBuilder) super.sort(sort);
    }
    
    @Override
     public CopyOfBuilder vfxEngineMinVersionCode(Integer vfxEngineMinVersionCode) {
      return (CopyOfBuilder) super.vfxEngineMinVersionCode(vfxEngineMinVersionCode);
    }
    
    @Override
     public CopyOfBuilder online(Integer online) {
      return (CopyOfBuilder) super.online(online);
    }
    
    @Override
     public CopyOfBuilder transitionVfxCategoryId(String transitionVfxCategoryId) {
      return (CopyOfBuilder) super.transitionVfxCategoryId(transitionVfxCategoryId);
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
