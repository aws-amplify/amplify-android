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

/** This is an auto generated class representing the VideoFilter type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "VideoFilters", authRules = {
  @AuthRule(allow = AuthStrategy.PUBLIC, provider = "iam", operations = { ModelOperation.READ }),
  @AuthRule(allow = AuthStrategy.GROUPS, groupClaim = "cognito:groups", groups = { "Editor" }, provider = "userPools", operations = { ModelOperation.READ, ModelOperation.CREATE, ModelOperation.UPDATE, ModelOperation.DELETE }),
  @AuthRule(allow = AuthStrategy.PRIVATE, provider = "iam", operations = { ModelOperation.CREATE, ModelOperation.UPDATE, ModelOperation.DELETE, ModelOperation.READ })
})
@Index(name = "byVideoFilterCategory", fields = {"categoryID"})
public final class VideoFilter implements Model {
  public static final QueryField ID = field("VideoFilter", "id");
  public static final QueryField NAME = field("VideoFilter", "name");
  public static final QueryField COVER_URL = field("VideoFilter", "coverUrl");
  public static final QueryField DOWNLOAD_URL = field("VideoFilter", "downloadUrl");
  public static final QueryField SORT = field("VideoFilter", "sort");
  public static final QueryField VFX_ENGINE_MIN_VERSION_CODE = field("VideoFilter", "vfxEngineMinVersionCode");
  public static final QueryField CATEGORY_ID = field("VideoFilter", "categoryID");
  public static final QueryField STAGED_ROLLOUT = field("VideoFilter", "stagedRollout");
  public static final QueryField ONLINE = field("VideoFilter", "online");
  public static final QueryField GET_METHOD = field("VideoFilter", "getMethod");
  public static final QueryField UPDATED_AT = field("VideoFilter", "updatedAt");
  public static final QueryField TEST_TAG = field("VideoFilter", "testTag");
  public static final QueryField DEPLOY_FLAG = field("VideoFilter", "deployFlag");
  public static final QueryField MASK_COLOR = field("VideoFilter", "maskColor");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String") String name;
  private final @ModelField(targetType="String") String coverUrl;
  private final @ModelField(targetType="String") String downloadUrl;
  private final @ModelField(targetType="Int") Integer sort;
  private final @ModelField(targetType="Int") Integer vfxEngineMinVersionCode;
  private final @ModelField(targetType="ID") String categoryID;
  private final @ModelField(targetType="String") String stagedRollout;
  private final @ModelField(targetType="Int") Integer online;
  private final @ModelField(targetType="Int") Integer getMethod;
  private final @ModelField(targetType="AWSDateTime", isRequired = true) Temporal.DateTime updatedAt;
  private final @ModelField(targetType="String") String testTag;
  private final @ModelField(targetType="String") String deployFlag;
  private final @ModelField(targetType="String") String maskColor;
  private final @ModelField(targetType="VideoFilterLocale") @HasMany(associatedWith = "materialID", type = VideoFilterLocale.class) List<VideoFilterLocale> VideoFilterLocales = null;
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
  
  public String getCategoryId() {
      return categoryID;
  }
  
  public String getStagedRollout() {
      return stagedRollout;
  }
  
  public Integer getOnline() {
      return online;
  }
  
  public Integer getGetMethod() {
      return getMethod;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  public String getTestTag() {
      return testTag;
  }
  
  public String getDeployFlag() {
      return deployFlag;
  }
  
  public String getMaskColor() {
      return maskColor;
  }
  
  public List<VideoFilterLocale> getVideoFilterLocales() {
      return VideoFilterLocales;
  }
  
  public Temporal.DateTime getCreatedAt() {
      return createdAt;
  }
  
  private VideoFilter(String id, String name, String coverUrl, String downloadUrl, Integer sort, Integer vfxEngineMinVersionCode, String categoryID, String stagedRollout, Integer online, Integer getMethod, Temporal.DateTime updatedAt, String testTag, String deployFlag, String maskColor) {
    this.id = id;
    this.name = name;
    this.coverUrl = coverUrl;
    this.downloadUrl = downloadUrl;
    this.sort = sort;
    this.vfxEngineMinVersionCode = vfxEngineMinVersionCode;
    this.categoryID = categoryID;
    this.stagedRollout = stagedRollout;
    this.online = online;
    this.getMethod = getMethod;
    this.updatedAt = updatedAt;
    this.testTag = testTag;
    this.deployFlag = deployFlag;
    this.maskColor = maskColor;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      VideoFilter videoFilter = (VideoFilter) obj;
      return ObjectsCompat.equals(getId(), videoFilter.getId()) &&
              ObjectsCompat.equals(getName(), videoFilter.getName()) &&
              ObjectsCompat.equals(getCoverUrl(), videoFilter.getCoverUrl()) &&
              ObjectsCompat.equals(getDownloadUrl(), videoFilter.getDownloadUrl()) &&
              ObjectsCompat.equals(getSort(), videoFilter.getSort()) &&
              ObjectsCompat.equals(getVfxEngineMinVersionCode(), videoFilter.getVfxEngineMinVersionCode()) &&
              ObjectsCompat.equals(getCategoryId(), videoFilter.getCategoryId()) &&
              ObjectsCompat.equals(getStagedRollout(), videoFilter.getStagedRollout()) &&
              ObjectsCompat.equals(getOnline(), videoFilter.getOnline()) &&
              ObjectsCompat.equals(getGetMethod(), videoFilter.getGetMethod()) &&
              ObjectsCompat.equals(getUpdatedAt(), videoFilter.getUpdatedAt()) &&
              ObjectsCompat.equals(getTestTag(), videoFilter.getTestTag()) &&
              ObjectsCompat.equals(getDeployFlag(), videoFilter.getDeployFlag()) &&
              ObjectsCompat.equals(getMaskColor(), videoFilter.getMaskColor()) &&
              ObjectsCompat.equals(getCreatedAt(), videoFilter.getCreatedAt());
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
      .append(getCategoryId())
      .append(getStagedRollout())
      .append(getOnline())
      .append(getGetMethod())
      .append(getUpdatedAt())
      .append(getTestTag())
      .append(getDeployFlag())
      .append(getMaskColor())
      .append(getCreatedAt())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("VideoFilter {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("name=" + String.valueOf(getName()) + ", ")
      .append("coverUrl=" + String.valueOf(getCoverUrl()) + ", ")
      .append("downloadUrl=" + String.valueOf(getDownloadUrl()) + ", ")
      .append("sort=" + String.valueOf(getSort()) + ", ")
      .append("vfxEngineMinVersionCode=" + String.valueOf(getVfxEngineMinVersionCode()) + ", ")
      .append("categoryID=" + String.valueOf(getCategoryId()) + ", ")
      .append("stagedRollout=" + String.valueOf(getStagedRollout()) + ", ")
      .append("online=" + String.valueOf(getOnline()) + ", ")
      .append("getMethod=" + String.valueOf(getGetMethod()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()) + ", ")
      .append("testTag=" + String.valueOf(getTestTag()) + ", ")
      .append("deployFlag=" + String.valueOf(getDeployFlag()) + ", ")
      .append("maskColor=" + String.valueOf(getMaskColor()) + ", ")
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
  public static VideoFilter justId(String id) {
    return new VideoFilter(
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
      categoryID,
      stagedRollout,
      online,
      getMethod,
      updatedAt,
      testTag,
      deployFlag,
      maskColor);
  }
  public interface UpdatedAtStep {
    BuildStep updatedAt(Temporal.DateTime updatedAt);
  }
  

  public interface BuildStep {
    VideoFilter build();
    BuildStep id(String id);
    BuildStep name(String name);
    BuildStep coverUrl(String coverUrl);
    BuildStep downloadUrl(String downloadUrl);
    BuildStep sort(Integer sort);
    BuildStep vfxEngineMinVersionCode(Integer vfxEngineMinVersionCode);
    BuildStep categoryId(String categoryId);
    BuildStep stagedRollout(String stagedRollout);
    BuildStep online(Integer online);
    BuildStep getMethod(Integer getMethod);
    BuildStep testTag(String testTag);
    BuildStep deployFlag(String deployFlag);
    BuildStep maskColor(String maskColor);
  }
  

  public static class Builder implements UpdatedAtStep, BuildStep {
    private String id;
    private Temporal.DateTime updatedAt;
    private String name;
    private String coverUrl;
    private String downloadUrl;
    private Integer sort;
    private Integer vfxEngineMinVersionCode;
    private String categoryID;
    private String stagedRollout;
    private Integer online;
    private Integer getMethod;
    private String testTag;
    private String deployFlag;
    private String maskColor;
    @Override
     public VideoFilter build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new VideoFilter(
          id,
          name,
          coverUrl,
          downloadUrl,
          sort,
          vfxEngineMinVersionCode,
          categoryID,
          stagedRollout,
          online,
          getMethod,
          updatedAt,
          testTag,
          deployFlag,
          maskColor);
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
     public BuildStep categoryId(String categoryId) {
        this.categoryID = categoryId;
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
     public BuildStep getMethod(Integer getMethod) {
        this.getMethod = getMethod;
        return this;
    }
    
    @Override
     public BuildStep testTag(String testTag) {
        this.testTag = testTag;
        return this;
    }
    
    @Override
     public BuildStep deployFlag(String deployFlag) {
        this.deployFlag = deployFlag;
        return this;
    }
    
    @Override
     public BuildStep maskColor(String maskColor) {
        this.maskColor = maskColor;
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
    private CopyOfBuilder(String id, String name, String coverUrl, String downloadUrl, Integer sort, Integer vfxEngineMinVersionCode, String categoryId, String stagedRollout, Integer online, Integer getMethod, Temporal.DateTime updatedAt, String testTag, String deployFlag, String maskColor) {
      super.id(id);
      super.updatedAt(updatedAt)
        .name(name)
        .coverUrl(coverUrl)
        .downloadUrl(downloadUrl)
        .sort(sort)
        .vfxEngineMinVersionCode(vfxEngineMinVersionCode)
        .categoryId(categoryId)
        .stagedRollout(stagedRollout)
        .online(online)
        .getMethod(getMethod)
        .testTag(testTag)
        .deployFlag(deployFlag)
        .maskColor(maskColor);
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
     public CopyOfBuilder categoryId(String categoryId) {
      return (CopyOfBuilder) super.categoryId(categoryId);
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
     public CopyOfBuilder getMethod(Integer getMethod) {
      return (CopyOfBuilder) super.getMethod(getMethod);
    }
    
    @Override
     public CopyOfBuilder testTag(String testTag) {
      return (CopyOfBuilder) super.testTag(testTag);
    }
    
    @Override
     public CopyOfBuilder deployFlag(String deployFlag) {
      return (CopyOfBuilder) super.deployFlag(deployFlag);
    }
    
    @Override
     public CopyOfBuilder maskColor(String maskColor) {
      return (CopyOfBuilder) super.maskColor(maskColor);
    }
  }
  
}
