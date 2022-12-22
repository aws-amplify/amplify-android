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

/** This is an auto generated class representing the FontVFX type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "FontVFXES", authRules = {
  @AuthRule(allow = AuthStrategy.PUBLIC, provider = "iam", operations = { ModelOperation.READ }),
  @AuthRule(allow = AuthStrategy.GROUPS, groupClaim = "cognito:groups", groups = { "Editor" }, provider = "userPools", operations = { ModelOperation.READ, ModelOperation.CREATE, ModelOperation.UPDATE, ModelOperation.DELETE }),
  @AuthRule(allow = AuthStrategy.PRIVATE, provider = "iam", operations = { ModelOperation.CREATE, ModelOperation.UPDATE, ModelOperation.DELETE, ModelOperation.READ })
})
@Index(name = "byFontVFXCategory", fields = {"fontvfxcategoryID"})
public final class FontVFX implements Model {
  public static final QueryField ID = field("FontVFX", "id");
  public static final QueryField NAME = field("FontVFX", "name");
  public static final QueryField COVER_URL = field("FontVFX", "coverUrl");
  public static final QueryField DOWNLOAD_URL = field("FontVFX", "downloadUrl");
  public static final QueryField SORT = field("FontVFX", "sort");
  public static final QueryField VFX_ENGINE_MIN_VERSION_CODE = field("FontVFX", "vfxEngineMinVersionCode");
  public static final QueryField FONTVFXCATEGORY_ID = field("FontVFX", "fontvfxcategoryID");
  public static final QueryField ONLINE = field("FontVFX", "online");
  public static final QueryField UPDATED_AT = field("FontVFX", "updatedAt");
  public static final QueryField GET_METHOD = field("FontVFX", "getMethod");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String") String name;
  private final @ModelField(targetType="String") String coverUrl;
  private final @ModelField(targetType="String") String downloadUrl;
  private final @ModelField(targetType="Int") Integer sort;
  private final @ModelField(targetType="Int") Integer vfxEngineMinVersionCode;
  private final @ModelField(targetType="ID") String fontvfxcategoryID;
  private final @ModelField(targetType="Int") Integer online;
  private final @ModelField(targetType="AWSDateTime", isRequired = true) Temporal.DateTime updatedAt;
  private final @ModelField(targetType="Int") Integer getMethod;
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
  
  public String getFontvfxcategoryId() {
      return fontvfxcategoryID;
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
  
  private FontVFX(String id, String name, String coverUrl, String downloadUrl, Integer sort, Integer vfxEngineMinVersionCode, String fontvfxcategoryID, Integer online, Temporal.DateTime updatedAt, Integer getMethod) {
    this.id = id;
    this.name = name;
    this.coverUrl = coverUrl;
    this.downloadUrl = downloadUrl;
    this.sort = sort;
    this.vfxEngineMinVersionCode = vfxEngineMinVersionCode;
    this.fontvfxcategoryID = fontvfxcategoryID;
    this.online = online;
    this.updatedAt = updatedAt;
    this.getMethod = getMethod;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      FontVFX fontVfx = (FontVFX) obj;
      return ObjectsCompat.equals(getId(), fontVfx.getId()) &&
              ObjectsCompat.equals(getName(), fontVfx.getName()) &&
              ObjectsCompat.equals(getCoverUrl(), fontVfx.getCoverUrl()) &&
              ObjectsCompat.equals(getDownloadUrl(), fontVfx.getDownloadUrl()) &&
              ObjectsCompat.equals(getSort(), fontVfx.getSort()) &&
              ObjectsCompat.equals(getVfxEngineMinVersionCode(), fontVfx.getVfxEngineMinVersionCode()) &&
              ObjectsCompat.equals(getFontvfxcategoryId(), fontVfx.getFontvfxcategoryId()) &&
              ObjectsCompat.equals(getOnline(), fontVfx.getOnline()) &&
              ObjectsCompat.equals(getUpdatedAt(), fontVfx.getUpdatedAt()) &&
              ObjectsCompat.equals(getGetMethod(), fontVfx.getGetMethod());
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
      .append(getFontvfxcategoryId())
      .append(getOnline())
      .append(getUpdatedAt())
      .append(getGetMethod())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("FontVFX {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("name=" + String.valueOf(getName()) + ", ")
      .append("coverUrl=" + String.valueOf(getCoverUrl()) + ", ")
      .append("downloadUrl=" + String.valueOf(getDownloadUrl()) + ", ")
      .append("sort=" + String.valueOf(getSort()) + ", ")
      .append("vfxEngineMinVersionCode=" + String.valueOf(getVfxEngineMinVersionCode()) + ", ")
      .append("fontvfxcategoryID=" + String.valueOf(getFontvfxcategoryId()) + ", ")
      .append("online=" + String.valueOf(getOnline()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()) + ", ")
      .append("getMethod=" + String.valueOf(getGetMethod()))
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
  public static FontVFX justId(String id) {
    return new FontVFX(
      id,
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
      fontvfxcategoryID,
      online,
      updatedAt,
      getMethod);
  }
  public interface UpdatedAtStep {
    BuildStep updatedAt(Temporal.DateTime updatedAt);
  }
  

  public interface BuildStep {
    FontVFX build();
    BuildStep id(String id);
    BuildStep name(String name);
    BuildStep coverUrl(String coverUrl);
    BuildStep downloadUrl(String downloadUrl);
    BuildStep sort(Integer sort);
    BuildStep vfxEngineMinVersionCode(Integer vfxEngineMinVersionCode);
    BuildStep fontvfxcategoryId(String fontvfxcategoryId);
    BuildStep online(Integer online);
    BuildStep getMethod(Integer getMethod);
  }
  

  public static class Builder implements UpdatedAtStep, BuildStep {
    private String id;
    private Temporal.DateTime updatedAt;
    private String name;
    private String coverUrl;
    private String downloadUrl;
    private Integer sort;
    private Integer vfxEngineMinVersionCode;
    private String fontvfxcategoryID;
    private Integer online;
    private Integer getMethod;
    @Override
     public FontVFX build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new FontVFX(
          id,
          name,
          coverUrl,
          downloadUrl,
          sort,
          vfxEngineMinVersionCode,
          fontvfxcategoryID,
          online,
          updatedAt,
          getMethod);
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
     public BuildStep fontvfxcategoryId(String fontvfxcategoryId) {
        this.fontvfxcategoryID = fontvfxcategoryId;
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
    private CopyOfBuilder(String id, String name, String coverUrl, String downloadUrl, Integer sort, Integer vfxEngineMinVersionCode, String fontvfxcategoryId, Integer online, Temporal.DateTime updatedAt, Integer getMethod) {
      super.id(id);
      super.updatedAt(updatedAt)
        .name(name)
        .coverUrl(coverUrl)
        .downloadUrl(downloadUrl)
        .sort(sort)
        .vfxEngineMinVersionCode(vfxEngineMinVersionCode)
        .fontvfxcategoryId(fontvfxcategoryId)
        .online(online)
        .getMethod(getMethod);
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
     public CopyOfBuilder fontvfxcategoryId(String fontvfxcategoryId) {
      return (CopyOfBuilder) super.fontvfxcategoryId(fontvfxcategoryId);
    }
    
    @Override
     public CopyOfBuilder online(Integer online) {
      return (CopyOfBuilder) super.online(online);
    }
    
    @Override
     public CopyOfBuilder getMethod(Integer getMethod) {
      return (CopyOfBuilder) super.getMethod(getMethod);
    }
  }
  
}
