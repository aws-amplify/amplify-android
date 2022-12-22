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
  public static final QueryField ONLINE = field("FilterVFX", "online");
  public static final QueryField UPDATED_AT = field("FilterVFX", "updatedAt");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String") String name;
  private final @ModelField(targetType="String") String coverUrl;
  private final @ModelField(targetType="String") String downloadUrl;
  private final @ModelField(targetType="Int") Integer sort;
  private final @ModelField(targetType="Int") Integer vfxEngineMinVersionCode;
  private final @ModelField(targetType="ID") String filterVfxCategoryID;
  private final @ModelField(targetType="Int") Integer online;
  private final @ModelField(targetType="AWSDateTime", isRequired = true) Temporal.DateTime updatedAt;
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
  
  public Integer getOnline() {
      return online;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  private FilterVFX(String id, String name, String coverUrl, String downloadUrl, Integer sort, Integer vfxEngineMinVersionCode, String filterVfxCategoryID, Integer online, Temporal.DateTime updatedAt) {
    this.id = id;
    this.name = name;
    this.coverUrl = coverUrl;
    this.downloadUrl = downloadUrl;
    this.sort = sort;
    this.vfxEngineMinVersionCode = vfxEngineMinVersionCode;
    this.filterVfxCategoryID = filterVfxCategoryID;
    this.online = online;
    this.updatedAt = updatedAt;
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
              ObjectsCompat.equals(getOnline(), filterVfx.getOnline()) &&
              ObjectsCompat.equals(getUpdatedAt(), filterVfx.getUpdatedAt());
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
      .append(getOnline())
      .append(getUpdatedAt())
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
      .append("online=" + String.valueOf(getOnline()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()))
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
      online,
      updatedAt);
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
    BuildStep online(Integer online);
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
    private Integer online;
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
          online,
          updatedAt);
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
     public BuildStep online(Integer online) {
        this.online = online;
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
    private CopyOfBuilder(String id, String name, String coverUrl, String downloadUrl, Integer sort, Integer vfxEngineMinVersionCode, String filterVfxCategoryId, Integer online, Temporal.DateTime updatedAt) {
      super.id(id);
      super.updatedAt(updatedAt)
        .name(name)
        .coverUrl(coverUrl)
        .downloadUrl(downloadUrl)
        .sort(sort)
        .vfxEngineMinVersionCode(vfxEngineMinVersionCode)
        .filterVfxCategoryId(filterVfxCategoryId)
        .online(online);
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
     public CopyOfBuilder online(Integer online) {
      return (CopyOfBuilder) super.online(online);
    }
  }
  
}
