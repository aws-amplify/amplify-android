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

/** This is an auto generated class representing the TextAnim type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "TextAnims", authRules = {
  @AuthRule(allow = AuthStrategy.PUBLIC, provider = "iam", operations = { ModelOperation.READ }),
  @AuthRule(allow = AuthStrategy.GROUPS, groupClaim = "cognito:groups", groups = { "Editor" }, provider = "userPools", operations = { ModelOperation.READ, ModelOperation.CREATE, ModelOperation.UPDATE, ModelOperation.DELETE }),
  @AuthRule(allow = AuthStrategy.PRIVATE, provider = "iam", operations = { ModelOperation.CREATE, ModelOperation.UPDATE, ModelOperation.DELETE, ModelOperation.READ })
})
@Index(name = "byTextAnimCategory", fields = {"categoryID"})
public final class TextAnim implements Model {
  public static final QueryField ID = field("TextAnim", "id");
  public static final QueryField NAME = field("TextAnim", "name");
  public static final QueryField COVER_URL = field("TextAnim", "coverUrl");
  public static final QueryField DOWNLOAD_URL = field("TextAnim", "downloadUrl");
  public static final QueryField SORT = field("TextAnim", "sort");
  public static final QueryField TARGET_VERSION_CODE = field("TextAnim", "targetVersionCode");
  public static final QueryField ONLINE = field("TextAnim", "online");
  public static final QueryField CATEGORY_ID = field("TextAnim", "categoryID");
  public static final QueryField UPDATED_AT = field("TextAnim", "updatedAt");
  public static final QueryField GET_METHOD = field("TextAnim", "getMethod");
  public static final QueryField DISPLAY_NAME = field("TextAnim", "displayName");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String") String name;
  private final @ModelField(targetType="String") String coverUrl;
  private final @ModelField(targetType="String") String downloadUrl;
  private final @ModelField(targetType="Int") Integer sort;
  private final @ModelField(targetType="Int") Integer targetVersionCode;
  private final @ModelField(targetType="Int") Integer online;
  private final @ModelField(targetType="ID") String categoryID;
  private final @ModelField(targetType="AWSDateTime", isRequired = true) Temporal.DateTime updatedAt;
  private final @ModelField(targetType="Int") Integer getMethod;
  private final @ModelField(targetType="TextAnimLocale") @HasMany(associatedWith = "materialID", type = TextAnimLocale.class) List<TextAnimLocale> TextAnimLocales = null;
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
  
  public Integer getTargetVersionCode() {
      return targetVersionCode;
  }
  
  public Integer getOnline() {
      return online;
  }
  
  public String getCategoryId() {
      return categoryID;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  public Integer getGetMethod() {
      return getMethod;
  }
  
  public List<TextAnimLocale> getTextAnimLocales() {
      return TextAnimLocales;
  }
  
  public String getDisplayName() {
      return displayName;
  }
  
  private TextAnim(String id, String name, String coverUrl, String downloadUrl, Integer sort, Integer targetVersionCode, Integer online, String categoryID, Temporal.DateTime updatedAt, Integer getMethod, String displayName) {
    this.id = id;
    this.name = name;
    this.coverUrl = coverUrl;
    this.downloadUrl = downloadUrl;
    this.sort = sort;
    this.targetVersionCode = targetVersionCode;
    this.online = online;
    this.categoryID = categoryID;
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
      TextAnim textAnim = (TextAnim) obj;
      return ObjectsCompat.equals(getId(), textAnim.getId()) &&
              ObjectsCompat.equals(getName(), textAnim.getName()) &&
              ObjectsCompat.equals(getCoverUrl(), textAnim.getCoverUrl()) &&
              ObjectsCompat.equals(getDownloadUrl(), textAnim.getDownloadUrl()) &&
              ObjectsCompat.equals(getSort(), textAnim.getSort()) &&
              ObjectsCompat.equals(getTargetVersionCode(), textAnim.getTargetVersionCode()) &&
              ObjectsCompat.equals(getOnline(), textAnim.getOnline()) &&
              ObjectsCompat.equals(getCategoryId(), textAnim.getCategoryId()) &&
              ObjectsCompat.equals(getUpdatedAt(), textAnim.getUpdatedAt()) &&
              ObjectsCompat.equals(getGetMethod(), textAnim.getGetMethod()) &&
              ObjectsCompat.equals(getDisplayName(), textAnim.getDisplayName());
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
      .append(getTargetVersionCode())
      .append(getOnline())
      .append(getCategoryId())
      .append(getUpdatedAt())
      .append(getGetMethod())
      .append(getDisplayName())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("TextAnim {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("name=" + String.valueOf(getName()) + ", ")
      .append("coverUrl=" + String.valueOf(getCoverUrl()) + ", ")
      .append("downloadUrl=" + String.valueOf(getDownloadUrl()) + ", ")
      .append("sort=" + String.valueOf(getSort()) + ", ")
      .append("targetVersionCode=" + String.valueOf(getTargetVersionCode()) + ", ")
      .append("online=" + String.valueOf(getOnline()) + ", ")
      .append("categoryID=" + String.valueOf(getCategoryId()) + ", ")
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
  public static TextAnim justId(String id) {
    return new TextAnim(
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
      targetVersionCode,
      online,
      categoryID,
      updatedAt,
      getMethod,
      displayName);
  }
  public interface UpdatedAtStep {
    BuildStep updatedAt(Temporal.DateTime updatedAt);
  }
  

  public interface BuildStep {
    TextAnim build();
    BuildStep id(String id);
    BuildStep name(String name);
    BuildStep coverUrl(String coverUrl);
    BuildStep downloadUrl(String downloadUrl);
    BuildStep sort(Integer sort);
    BuildStep targetVersionCode(Integer targetVersionCode);
    BuildStep online(Integer online);
    BuildStep categoryId(String categoryId);
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
    private Integer targetVersionCode;
    private Integer online;
    private String categoryID;
    private Integer getMethod;
    private String displayName;
    @Override
     public TextAnim build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new TextAnim(
          id,
          name,
          coverUrl,
          downloadUrl,
          sort,
          targetVersionCode,
          online,
          categoryID,
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
     public BuildStep targetVersionCode(Integer targetVersionCode) {
        this.targetVersionCode = targetVersionCode;
        return this;
    }
    
    @Override
     public BuildStep online(Integer online) {
        this.online = online;
        return this;
    }
    
    @Override
     public BuildStep categoryId(String categoryId) {
        this.categoryID = categoryId;
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
    private CopyOfBuilder(String id, String name, String coverUrl, String downloadUrl, Integer sort, Integer targetVersionCode, Integer online, String categoryId, Temporal.DateTime updatedAt, Integer getMethod, String displayName) {
      super.id(id);
      super.updatedAt(updatedAt)
        .name(name)
        .coverUrl(coverUrl)
        .downloadUrl(downloadUrl)
        .sort(sort)
        .targetVersionCode(targetVersionCode)
        .online(online)
        .categoryId(categoryId)
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
     public CopyOfBuilder targetVersionCode(Integer targetVersionCode) {
      return (CopyOfBuilder) super.targetVersionCode(targetVersionCode);
    }
    
    @Override
     public CopyOfBuilder online(Integer online) {
      return (CopyOfBuilder) super.online(online);
    }
    
    @Override
     public CopyOfBuilder categoryId(String categoryId) {
      return (CopyOfBuilder) super.categoryId(categoryId);
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
