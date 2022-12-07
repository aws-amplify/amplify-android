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

/** This is an auto generated class representing the VFX type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "VFXES", authRules = {
  @AuthRule(allow = AuthStrategy.PUBLIC, provider = "iam", operations = { ModelOperation.READ }),
  @AuthRule(allow = AuthStrategy.GROUPS, groupClaim = "cognito:groups", groups = { "Editor" }, provider = "userPools", operations = { ModelOperation.READ, ModelOperation.CREATE, ModelOperation.UPDATE, ModelOperation.DELETE }),
  @AuthRule(allow = AuthStrategy.PRIVATE, provider = "iam", operations = { ModelOperation.CREATE, ModelOperation.UPDATE, ModelOperation.DELETE, ModelOperation.READ })
})
@Index(name = "byVFXCategory", fields = {"vfxCategoryID"})
public final class VFX implements Model {
  public static final QueryField ID = field("VFX", "id");
  public static final QueryField NAME = field("VFX", "name");
  public static final QueryField COVER_URL = field("VFX", "coverUrl");
  public static final QueryField DOWNLOAD_URL = field("VFX", "downloadUrl");
  public static final QueryField SORT = field("VFX", "sort");
  public static final QueryField VFX_ENGINE_MIN_VERSION_CODE = field("VFX", "vfxEngineMinVersionCode");
  public static final QueryField VFX_CATEGORY_ID = field("VFX", "vfxCategoryID");
  public static final QueryField STAGED_ROLLOUT = field("VFX", "stagedRollout");
  public static final QueryField ONLINE = field("VFX", "online");
  public static final QueryField UPDATED_AT = field("VFX", "updatedAt");
  public static final QueryField TEST_TAG = field("VFX", "testTag");
  public static final QueryField GET_METHOD = field("VFX", "getMethod");
  public static final QueryField LANG_CODE = field("VFX", "langCode");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String") String name;
  private final @ModelField(targetType="String") String coverUrl;
  private final @ModelField(targetType="String") String downloadUrl;
  private final @ModelField(targetType="Int") Integer sort;
  private final @ModelField(targetType="Int") Integer vfxEngineMinVersionCode;
  private final @ModelField(targetType="ID") String vfxCategoryID;
  private final @ModelField(targetType="String") String stagedRollout;
  private final @ModelField(targetType="Int") Integer online;
  private final @ModelField(targetType="AWSDateTime", isRequired = true) Temporal.DateTime updatedAt;
  private final @ModelField(targetType="String") String testTag;
  private final @ModelField(targetType="Int") Integer getMethod;
  private final @ModelField(targetType="String") String langCode;
  private final @ModelField(targetType="VFXLocale") @HasMany(associatedWith = "vfxID", type = VFXLocale.class) List<VFXLocale> VFXLocales = null;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime createdAt;
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
  
  public String getVfxCategoryId() {
      return vfxCategoryID;
  }
  
  public String getStagedRollout() {
      return stagedRollout;
  }
  
  public Integer getOnline() {
      return online;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  public String getTestTag() {
      return testTag;
  }
  
  public Integer getGetMethod() {
      return getMethod;
  }
  
  public String getLangCode() {
      return langCode;
  }
  
  public List<VFXLocale> getVfxLocales() {
      return VFXLocales;
  }
  
  public Temporal.DateTime getCreatedAt() {
      return createdAt;
  }
  
  private VFX(String id, String name, String coverUrl, String downloadUrl, Integer sort, Integer vfxEngineMinVersionCode, String vfxCategoryID, String stagedRollout, Integer online, Temporal.DateTime updatedAt, String testTag, Integer getMethod, String langCode) {
    this.id = id;
    this.name = name;
    this.coverUrl = coverUrl;
    this.downloadUrl = downloadUrl;
    this.sort = sort;
    this.vfxEngineMinVersionCode = vfxEngineMinVersionCode;
    this.vfxCategoryID = vfxCategoryID;
    this.stagedRollout = stagedRollout;
    this.online = online;
    this.updatedAt = updatedAt;
    this.testTag = testTag;
    this.getMethod = getMethod;
    this.langCode = langCode;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      VFX vfx = (VFX) obj;
      return ObjectsCompat.equals(getId(), vfx.getId()) &&
              ObjectsCompat.equals(getName(), vfx.getName()) &&
              ObjectsCompat.equals(getCoverUrl(), vfx.getCoverUrl()) &&
              ObjectsCompat.equals(getDownloadUrl(), vfx.getDownloadUrl()) &&
              ObjectsCompat.equals(getSort(), vfx.getSort()) &&
              ObjectsCompat.equals(getVfxEngineMinVersionCode(), vfx.getVfxEngineMinVersionCode()) &&
              ObjectsCompat.equals(getVfxCategoryId(), vfx.getVfxCategoryId()) &&
              ObjectsCompat.equals(getStagedRollout(), vfx.getStagedRollout()) &&
              ObjectsCompat.equals(getOnline(), vfx.getOnline()) &&
              ObjectsCompat.equals(getUpdatedAt(), vfx.getUpdatedAt()) &&
              ObjectsCompat.equals(getTestTag(), vfx.getTestTag()) &&
              ObjectsCompat.equals(getGetMethod(), vfx.getGetMethod()) &&
              ObjectsCompat.equals(getLangCode(), vfx.getLangCode()) &&
              ObjectsCompat.equals(getCreatedAt(), vfx.getCreatedAt());
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
      .append(getVfxCategoryId())
      .append(getStagedRollout())
      .append(getOnline())
      .append(getUpdatedAt())
      .append(getTestTag())
      .append(getGetMethod())
      .append(getLangCode())
      .append(getCreatedAt())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("VFX {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("name=" + String.valueOf(getName()) + ", ")
      .append("coverUrl=" + String.valueOf(getCoverUrl()) + ", ")
      .append("downloadUrl=" + String.valueOf(getDownloadUrl()) + ", ")
      .append("sort=" + String.valueOf(getSort()) + ", ")
      .append("vfxEngineMinVersionCode=" + String.valueOf(getVfxEngineMinVersionCode()) + ", ")
      .append("vfxCategoryID=" + String.valueOf(getVfxCategoryId()) + ", ")
      .append("stagedRollout=" + String.valueOf(getStagedRollout()) + ", ")
      .append("online=" + String.valueOf(getOnline()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()) + ", ")
      .append("testTag=" + String.valueOf(getTestTag()) + ", ")
      .append("getMethod=" + String.valueOf(getGetMethod()) + ", ")
      .append("langCode=" + String.valueOf(getLangCode()) + ", ")
      .append("createdAt=" + String.valueOf(getCreatedAt()))
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
  public static VFX justId(String id) {
    return new VFX(
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
      vfxCategoryID,
      stagedRollout,
      online,
      updatedAt,
      testTag,
      getMethod,
      langCode);
  }
  public interface UpdatedAtStep {
    BuildStep updatedAt(Temporal.DateTime updatedAt);
  }
  

  public interface BuildStep {
    VFX build();
    BuildStep id(String id);
    BuildStep name(String name);
    BuildStep coverUrl(String coverUrl);
    BuildStep downloadUrl(String downloadUrl);
    BuildStep sort(Integer sort);
    BuildStep vfxEngineMinVersionCode(Integer vfxEngineMinVersionCode);
    BuildStep vfxCategoryId(String vfxCategoryId);
    BuildStep stagedRollout(String stagedRollout);
    BuildStep online(Integer online);
    BuildStep testTag(String testTag);
    BuildStep getMethod(Integer getMethod);
    BuildStep langCode(String langCode);
  }
  

  public static class Builder implements UpdatedAtStep, BuildStep {
    private String id;
    private Temporal.DateTime updatedAt;
    private String name;
    private String coverUrl;
    private String downloadUrl;
    private Integer sort;
    private Integer vfxEngineMinVersionCode;
    private String vfxCategoryID;
    private String stagedRollout;
    private Integer online;
    private String testTag;
    private Integer getMethod;
    private String langCode;
    @Override
     public VFX build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new VFX(
          id,
          name,
          coverUrl,
          downloadUrl,
          sort,
          vfxEngineMinVersionCode,
          vfxCategoryID,
          stagedRollout,
          online,
          updatedAt,
          testTag,
          getMethod,
          langCode);
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
     public BuildStep vfxCategoryId(String vfxCategoryId) {
        this.vfxCategoryID = vfxCategoryId;
        return this;
    }
    
    @Override
     public BuildStep stagedRollout(String stagedRollout) {
        this.stagedRollout = stagedRollout;
        return this;
    }
    
    @Override
     public BuildStep online(Integer online) {
        this.online = online;
        return this;
    }
    
    @Override
     public BuildStep testTag(String testTag) {
        this.testTag = testTag;
        return this;
    }
    
    @Override
     public BuildStep getMethod(Integer getMethod) {
        this.getMethod = getMethod;
        return this;
    }
    
    @Override
     public BuildStep langCode(String langCode) {
        this.langCode = langCode;
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
    private CopyOfBuilder(String id, String name, String coverUrl, String downloadUrl, Integer sort, Integer vfxEngineMinVersionCode, String vfxCategoryId, String stagedRollout, Integer online, Temporal.DateTime updatedAt, String testTag, Integer getMethod, String langCode) {
      super.id(id);
      super.updatedAt(updatedAt)
        .name(name)
        .coverUrl(coverUrl)
        .downloadUrl(downloadUrl)
        .sort(sort)
        .vfxEngineMinVersionCode(vfxEngineMinVersionCode)
        .vfxCategoryId(vfxCategoryId)
        .stagedRollout(stagedRollout)
        .online(online)
        .testTag(testTag)
        .getMethod(getMethod)
        .langCode(langCode);
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
     public CopyOfBuilder vfxCategoryId(String vfxCategoryId) {
      return (CopyOfBuilder) super.vfxCategoryId(vfxCategoryId);
    }
    
    @Override
     public CopyOfBuilder stagedRollout(String stagedRollout) {
      return (CopyOfBuilder) super.stagedRollout(stagedRollout);
    }
    
    @Override
     public CopyOfBuilder online(Integer online) {
      return (CopyOfBuilder) super.online(online);
    }
    
    @Override
     public CopyOfBuilder testTag(String testTag) {
      return (CopyOfBuilder) super.testTag(testTag);
    }
    
    @Override
     public CopyOfBuilder getMethod(Integer getMethod) {
      return (CopyOfBuilder) super.getMethod(getMethod);
    }
    
    @Override
     public CopyOfBuilder langCode(String langCode) {
      return (CopyOfBuilder) super.langCode(langCode);
    }
  }
  
}
