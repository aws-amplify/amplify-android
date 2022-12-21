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
  public static final QueryField ONLINE = field("VFX", "online");
  public static final QueryField UPDATED_AT = field("VFX", "updatedAt");
  public static final QueryField GET_METHOD = field("VFX", "getMethod");
  public static final QueryField LANG_CODE = field("VFX", "langCode");
  public static final QueryField GRAY_RELEASE = field("VFX", "grayRelease");
  public static final QueryField REQUIRE_GPU_SCORE = field("VFX", "requireGPUScore");
  public static final QueryField REQUIRE_CPU_SCORE = field("VFX", "requireCPUScore");
  public static final QueryField REQUIRE_MEM_SCORE = field("VFX", "requireMemScore");
  public static final QueryField DISPLAY_NAME = field("VFX", "displayName");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String") String name;
  private final @ModelField(targetType="String") String coverUrl;
  private final @ModelField(targetType="String") String downloadUrl;
  private final @ModelField(targetType="Int") Integer sort;
  private final @ModelField(targetType="Int") Integer vfxEngineMinVersionCode;
  private final @ModelField(targetType="ID") String vfxCategoryID;
  private final @ModelField(targetType="Int") Integer online;
  private final @ModelField(targetType="AWSDateTime", isRequired = true) Temporal.DateTime updatedAt;
  private final @ModelField(targetType="Int") Integer getMethod;
  private final @ModelField(targetType="String") String langCode;
  private final @ModelField(targetType="VFXLocale") @HasMany(associatedWith = "vfxID", type = VFXLocale.class) List<VFXLocale> VFXLocales = null;
  private final @ModelField(targetType="Int") Integer grayRelease;
  private final @ModelField(targetType="Int") Integer requireGPUScore;
  private final @ModelField(targetType="Int") Integer requireCPUScore;
  private final @ModelField(targetType="Int") Integer requireMemScore;
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
  
  public String getVfxCategoryId() {
      return vfxCategoryID;
  }
  
  public Integer getOnline() {
      return online;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
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
  
  public Integer getGrayRelease() {
      return grayRelease;
  }
  
  public Integer getRequireGpuScore() {
      return requireGPUScore;
  }
  
  public Integer getRequireCpuScore() {
      return requireCPUScore;
  }
  
  public Integer getRequireMemScore() {
      return requireMemScore;
  }
  
  public String getDisplayName() {
      return displayName;
  }
  
  private VFX(String id, String name, String coverUrl, String downloadUrl, Integer sort, Integer vfxEngineMinVersionCode, String vfxCategoryID, Integer online, Temporal.DateTime updatedAt, Integer getMethod, String langCode, Integer grayRelease, Integer requireGPUScore, Integer requireCPUScore, Integer requireMemScore, String displayName) {
    this.id = id;
    this.name = name;
    this.coverUrl = coverUrl;
    this.downloadUrl = downloadUrl;
    this.sort = sort;
    this.vfxEngineMinVersionCode = vfxEngineMinVersionCode;
    this.vfxCategoryID = vfxCategoryID;
    this.online = online;
    this.updatedAt = updatedAt;
    this.getMethod = getMethod;
    this.langCode = langCode;
    this.grayRelease = grayRelease;
    this.requireGPUScore = requireGPUScore;
    this.requireCPUScore = requireCPUScore;
    this.requireMemScore = requireMemScore;
    this.displayName = displayName;
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
              ObjectsCompat.equals(getOnline(), vfx.getOnline()) &&
              ObjectsCompat.equals(getUpdatedAt(), vfx.getUpdatedAt()) &&
              ObjectsCompat.equals(getGetMethod(), vfx.getGetMethod()) &&
              ObjectsCompat.equals(getLangCode(), vfx.getLangCode()) &&
              ObjectsCompat.equals(getGrayRelease(), vfx.getGrayRelease()) &&
              ObjectsCompat.equals(getRequireGpuScore(), vfx.getRequireGpuScore()) &&
              ObjectsCompat.equals(getRequireCpuScore(), vfx.getRequireCpuScore()) &&
              ObjectsCompat.equals(getRequireMemScore(), vfx.getRequireMemScore()) &&
              ObjectsCompat.equals(getDisplayName(), vfx.getDisplayName());
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
      .append(getOnline())
      .append(getUpdatedAt())
      .append(getGetMethod())
      .append(getLangCode())
      .append(getGrayRelease())
      .append(getRequireGpuScore())
      .append(getRequireCpuScore())
      .append(getRequireMemScore())
      .append(getDisplayName())
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
      .append("online=" + String.valueOf(getOnline()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()) + ", ")
      .append("getMethod=" + String.valueOf(getGetMethod()) + ", ")
      .append("langCode=" + String.valueOf(getLangCode()) + ", ")
      .append("grayRelease=" + String.valueOf(getGrayRelease()) + ", ")
      .append("requireGPUScore=" + String.valueOf(getRequireGpuScore()) + ", ")
      .append("requireCPUScore=" + String.valueOf(getRequireCpuScore()) + ", ")
      .append("requireMemScore=" + String.valueOf(getRequireMemScore()) + ", ")
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
      online,
      updatedAt,
      getMethod,
      langCode,
      grayRelease,
      requireGPUScore,
      requireCPUScore,
      requireMemScore,
      displayName);
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
    BuildStep online(Integer online);
    BuildStep getMethod(Integer getMethod);
    BuildStep langCode(String langCode);
    BuildStep grayRelease(Integer grayRelease);
    BuildStep requireGpuScore(Integer requireGpuScore);
    BuildStep requireCpuScore(Integer requireCpuScore);
    BuildStep requireMemScore(Integer requireMemScore);
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
    private String vfxCategoryID;
    private Integer online;
    private Integer getMethod;
    private String langCode;
    private Integer grayRelease;
    private Integer requireGPUScore;
    private Integer requireCPUScore;
    private Integer requireMemScore;
    private String displayName;
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
          online,
          updatedAt,
          getMethod,
          langCode,
          grayRelease,
          requireGPUScore,
          requireCPUScore,
          requireMemScore,
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
     public BuildStep vfxCategoryId(String vfxCategoryId) {
        this.vfxCategoryID = vfxCategoryId;
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
     public BuildStep langCode(String langCode) {
        this.langCode = langCode;
        return this;
    }
    
    @Override
     public BuildStep grayRelease(Integer grayRelease) {
        this.grayRelease = grayRelease;
        return this;
    }
    
    @Override
     public BuildStep requireGpuScore(Integer requireGpuScore) {
        this.requireGPUScore = requireGpuScore;
        return this;
    }
    
    @Override
     public BuildStep requireCpuScore(Integer requireCpuScore) {
        this.requireCPUScore = requireCpuScore;
        return this;
    }
    
    @Override
     public BuildStep requireMemScore(Integer requireMemScore) {
        this.requireMemScore = requireMemScore;
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
    private CopyOfBuilder(String id, String name, String coverUrl, String downloadUrl, Integer sort, Integer vfxEngineMinVersionCode, String vfxCategoryId, Integer online, Temporal.DateTime updatedAt, Integer getMethod, String langCode, Integer grayRelease, Integer requireGpuScore, Integer requireCpuScore, Integer requireMemScore, String displayName) {
      super.id(id);
      super.updatedAt(updatedAt)
        .name(name)
        .coverUrl(coverUrl)
        .downloadUrl(downloadUrl)
        .sort(sort)
        .vfxEngineMinVersionCode(vfxEngineMinVersionCode)
        .vfxCategoryId(vfxCategoryId)
        .online(online)
        .getMethod(getMethod)
        .langCode(langCode)
        .grayRelease(grayRelease)
        .requireGpuScore(requireGpuScore)
        .requireCpuScore(requireCpuScore)
        .requireMemScore(requireMemScore)
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
     public CopyOfBuilder vfxCategoryId(String vfxCategoryId) {
      return (CopyOfBuilder) super.vfxCategoryId(vfxCategoryId);
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
     public CopyOfBuilder langCode(String langCode) {
      return (CopyOfBuilder) super.langCode(langCode);
    }
    
    @Override
     public CopyOfBuilder grayRelease(Integer grayRelease) {
      return (CopyOfBuilder) super.grayRelease(grayRelease);
    }
    
    @Override
     public CopyOfBuilder requireGpuScore(Integer requireGpuScore) {
      return (CopyOfBuilder) super.requireGpuScore(requireGpuScore);
    }
    
    @Override
     public CopyOfBuilder requireCpuScore(Integer requireCpuScore) {
      return (CopyOfBuilder) super.requireCpuScore(requireCpuScore);
    }
    
    @Override
     public CopyOfBuilder requireMemScore(Integer requireMemScore) {
      return (CopyOfBuilder) super.requireMemScore(requireMemScore);
    }
    
    @Override
     public CopyOfBuilder displayName(String displayName) {
      return (CopyOfBuilder) super.displayName(displayName);
    }
  }
  
}
