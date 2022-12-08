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

/** This is an auto generated class representing the FilterVFX type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "FilterVFXES", authRules = {
  @AuthRule(allow = AuthStrategy.PUBLIC, provider = "iam", operations = { ModelOperation.READ }),
  @AuthRule(allow = AuthStrategy.PRIVATE, provider = "iam", operations = { ModelOperation.CREATE, ModelOperation.UPDATE, ModelOperation.DELETE, ModelOperation.READ })
})
@Index(name = "byFilterVFXCategory", fields = {"filterVfxCategoryID"})
public final class FilterVFX implements Model {
  public static final QueryField ID = field("FilterVFX", "id");
  public static final QueryField NAME = field("FilterVFX", "name");
  public static final QueryField COVER_URL = field("FilterVFX", "coverUrl");
  public static final QueryField DOWNLOAD_URL = field("FilterVFX", "downloadUrl");
  public static final QueryField SORT = field("FilterVFX", "sort");
  public static final QueryField VFX_ENGINE_MIN_VERSION_CODE = field("FilterVFX", "vfxEngineMinVersionCode");
  public static final QueryField FILTER_VFX_CATEGORY_ID = field("FilterVFX", "filterVfxCategoryID");
  public static final QueryField STAGED_ROLLOUT = field("FilterVFX", "stagedRollout");
  public static final QueryField ONLINE = field("FilterVFX", "online");
  public static final QueryField UPDATED_AT = field("FilterVFX", "updatedAt");
  public static final QueryField TEST_TAG = field("FilterVFX", "testTag");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String") String name;
  private final @ModelField(targetType="String") String coverUrl;
  private final @ModelField(targetType="String") String downloadUrl;
  private final @ModelField(targetType="Int") Integer sort;
  private final @ModelField(targetType="Int") Integer vfxEngineMinVersionCode;
  private final @ModelField(targetType="ID") String filterVfxCategoryID;
  private final @ModelField(targetType="String") String stagedRollout;
  private final @ModelField(targetType="Int") Integer online;
  private final @ModelField(targetType="AWSDateTime", isRequired = true) Temporal.DateTime updatedAt;
  private final @ModelField(targetType="String") String testTag;
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
  
  public String getFilterVfxCategoryId() {
      return filterVfxCategoryID;
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
  
  private FilterVFX(String id, String name, String coverUrl, String downloadUrl, Integer sort, Integer vfxEngineMinVersionCode, String filterVfxCategoryID, String stagedRollout, Integer online, Temporal.DateTime updatedAt, String testTag) {
    this.id = id;
    this.name = name;
    this.coverUrl = coverUrl;
    this.downloadUrl = downloadUrl;
    this.sort = sort;
    this.vfxEngineMinVersionCode = vfxEngineMinVersionCode;
    this.filterVfxCategoryID = filterVfxCategoryID;
    this.stagedRollout = stagedRollout;
    this.online = online;
    this.updatedAt = updatedAt;
    this.testTag = testTag;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      FilterVFX filterVfx = (FilterVFX) obj;
      return ObjectsCompat.equals(getId(), filterVfx.getId()) &&
              ObjectsCompat.equals(getName(), filterVfx.getName()) &&
              ObjectsCompat.equals(getCoverUrl(), filterVfx.getCoverUrl()) &&
              ObjectsCompat.equals(getDownloadUrl(), filterVfx.getDownloadUrl()) &&
              ObjectsCompat.equals(getSort(), filterVfx.getSort()) &&
              ObjectsCompat.equals(getVfxEngineMinVersionCode(), filterVfx.getVfxEngineMinVersionCode()) &&
              ObjectsCompat.equals(getFilterVfxCategoryId(), filterVfx.getFilterVfxCategoryId()) &&
              ObjectsCompat.equals(getStagedRollout(), filterVfx.getStagedRollout()) &&
              ObjectsCompat.equals(getOnline(), filterVfx.getOnline()) &&
              ObjectsCompat.equals(getUpdatedAt(), filterVfx.getUpdatedAt()) &&
              ObjectsCompat.equals(getTestTag(), filterVfx.getTestTag());
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
      .append(getFilterVfxCategoryId())
      .append(getStagedRollout())
      .append(getOnline())
      .append(getUpdatedAt())
      .append(getTestTag())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("FilterVFX {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("name=" + String.valueOf(getName()) + ", ")
      .append("coverUrl=" + String.valueOf(getCoverUrl()) + ", ")
      .append("downloadUrl=" + String.valueOf(getDownloadUrl()) + ", ")
      .append("sort=" + String.valueOf(getSort()) + ", ")
      .append("vfxEngineMinVersionCode=" + String.valueOf(getVfxEngineMinVersionCode()) + ", ")
      .append("filterVfxCategoryID=" + String.valueOf(getFilterVfxCategoryId()) + ", ")
      .append("stagedRollout=" + String.valueOf(getStagedRollout()) + ", ")
      .append("online=" + String.valueOf(getOnline()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()) + ", ")
      .append("testTag=" + String.valueOf(getTestTag()))
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
  public static FilterVFX justId(String id) {
    return new FilterVFX(
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
      filterVfxCategoryID,
      stagedRollout,
      online,
      updatedAt,
      testTag);
  }
  public interface UpdatedAtStep {
    BuildStep updatedAt(Temporal.DateTime updatedAt);
  }
  

  public interface BuildStep {
    FilterVFX build();
    BuildStep id(String id);
    BuildStep name(String name);
    BuildStep coverUrl(String coverUrl);
    BuildStep downloadUrl(String downloadUrl);
    BuildStep sort(Integer sort);
    BuildStep vfxEngineMinVersionCode(Integer vfxEngineMinVersionCode);
    BuildStep filterVfxCategoryId(String filterVfxCategoryId);
    BuildStep stagedRollout(String stagedRollout);
    BuildStep online(Integer online);
    BuildStep testTag(String testTag);
  }
  

  public static class Builder implements UpdatedAtStep, BuildStep {
    private String id;
    private Temporal.DateTime updatedAt;
    private String name;
    private String coverUrl;
    private String downloadUrl;
    private Integer sort;
    private Integer vfxEngineMinVersionCode;
    private String filterVfxCategoryID;
    private String stagedRollout;
    private Integer online;
    private String testTag;
    @Override
     public FilterVFX build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new FilterVFX(
          id,
          name,
          coverUrl,
          downloadUrl,
          sort,
          vfxEngineMinVersionCode,
          filterVfxCategoryID,
          stagedRollout,
          online,
          updatedAt,
          testTag);
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
     public BuildStep filterVfxCategoryId(String filterVfxCategoryId) {
        this.filterVfxCategoryID = filterVfxCategoryId;
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
    private CopyOfBuilder(String id, String name, String coverUrl, String downloadUrl, Integer sort, Integer vfxEngineMinVersionCode, String filterVfxCategoryId, String stagedRollout, Integer online, Temporal.DateTime updatedAt, String testTag) {
      super.id(id);
      super.updatedAt(updatedAt)
        .name(name)
        .coverUrl(coverUrl)
        .downloadUrl(downloadUrl)
        .sort(sort)
        .vfxEngineMinVersionCode(vfxEngineMinVersionCode)
        .filterVfxCategoryId(filterVfxCategoryId)
        .stagedRollout(stagedRollout)
        .online(online)
        .testTag(testTag);
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
     public CopyOfBuilder filterVfxCategoryId(String filterVfxCategoryId) {
      return (CopyOfBuilder) super.filterVfxCategoryId(filterVfxCategoryId);
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
  }
  
}
