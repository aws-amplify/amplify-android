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

/** This is an auto generated class representing the TextTemplate type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "TextTemplates", authRules = {
  @AuthRule(allow = AuthStrategy.PUBLIC, provider = "iam", operations = { ModelOperation.READ }),
  @AuthRule(allow = AuthStrategy.GROUPS, groupClaim = "cognito:groups", groups = { "Editor" }, provider = "userPools", operations = { ModelOperation.READ, ModelOperation.CREATE, ModelOperation.UPDATE, ModelOperation.DELETE }),
  @AuthRule(allow = AuthStrategy.PRIVATE, provider = "iam", operations = { ModelOperation.CREATE, ModelOperation.UPDATE, ModelOperation.DELETE, ModelOperation.READ })
})
@Index(name = "byTextTemplateCategory", fields = {"categoryID"})
public final class TextTemplate implements Model {
  public static final QueryField ID = field("TextTemplate", "id");
  public static final QueryField NAME = field("TextTemplate", "name");
  public static final QueryField COVER_URL = field("TextTemplate", "coverUrl");
  public static final QueryField DOWNLOAD_URL = field("TextTemplate", "downloadUrl");
  public static final QueryField SORT = field("TextTemplate", "sort");
  public static final QueryField TARGET_VERSION_CODE = field("TextTemplate", "targetVersionCode");
  public static final QueryField ONLINE = field("TextTemplate", "online");
  public static final QueryField UPDATED_AT = field("TextTemplate", "updatedAt");
  public static final QueryField CATEGORY_ID = field("TextTemplate", "categoryID");
  public static final QueryField FONT_NAME = field("TextTemplate", "fontName");
  public static final QueryField FONT_URL = field("TextTemplate", "fontUrl");
  public static final QueryField GET_METHOD = field("TextTemplate", "getMethod");
  public static final QueryField DISPLAY_NAME = field("TextTemplate", "displayName");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String") String name;
  private final @ModelField(targetType="String") String coverUrl;
  private final @ModelField(targetType="String") String downloadUrl;
  private final @ModelField(targetType="Int") Integer sort;
  private final @ModelField(targetType="Int") Integer targetVersionCode;
  private final @ModelField(targetType="Int") Integer online;
  private final @ModelField(targetType="AWSDateTime") Temporal.DateTime updatedAt;
  private final @ModelField(targetType="ID") String categoryID;
  private final @ModelField(targetType="String") String fontName;
  private final @ModelField(targetType="String") String fontUrl;
  private final @ModelField(targetType="Int") Integer getMethod;
  private final @ModelField(targetType="TextTemplateLocale") @HasMany(associatedWith = "materialID", type = TextTemplateLocale.class) List<TextTemplateLocale> TextTemplateLocales = null;
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
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  public String getCategoryId() {
      return categoryID;
  }
  
  public String getFontName() {
      return fontName;
  }
  
  public String getFontUrl() {
      return fontUrl;
  }
  
  public Integer getGetMethod() {
      return getMethod;
  }
  
  public List<TextTemplateLocale> getTextTemplateLocales() {
      return TextTemplateLocales;
  }
  
  public String getDisplayName() {
      return displayName;
  }
  
  private TextTemplate(String id, String name, String coverUrl, String downloadUrl, Integer sort, Integer targetVersionCode, Integer online, Temporal.DateTime updatedAt, String categoryID, String fontName, String fontUrl, Integer getMethod, String displayName) {
    this.id = id;
    this.name = name;
    this.coverUrl = coverUrl;
    this.downloadUrl = downloadUrl;
    this.sort = sort;
    this.targetVersionCode = targetVersionCode;
    this.online = online;
    this.updatedAt = updatedAt;
    this.categoryID = categoryID;
    this.fontName = fontName;
    this.fontUrl = fontUrl;
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
      TextTemplate textTemplate = (TextTemplate) obj;
      return ObjectsCompat.equals(getId(), textTemplate.getId()) &&
              ObjectsCompat.equals(getName(), textTemplate.getName()) &&
              ObjectsCompat.equals(getCoverUrl(), textTemplate.getCoverUrl()) &&
              ObjectsCompat.equals(getDownloadUrl(), textTemplate.getDownloadUrl()) &&
              ObjectsCompat.equals(getSort(), textTemplate.getSort()) &&
              ObjectsCompat.equals(getTargetVersionCode(), textTemplate.getTargetVersionCode()) &&
              ObjectsCompat.equals(getOnline(), textTemplate.getOnline()) &&
              ObjectsCompat.equals(getUpdatedAt(), textTemplate.getUpdatedAt()) &&
              ObjectsCompat.equals(getCategoryId(), textTemplate.getCategoryId()) &&
              ObjectsCompat.equals(getFontName(), textTemplate.getFontName()) &&
              ObjectsCompat.equals(getFontUrl(), textTemplate.getFontUrl()) &&
              ObjectsCompat.equals(getGetMethod(), textTemplate.getGetMethod()) &&
              ObjectsCompat.equals(getDisplayName(), textTemplate.getDisplayName());
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
      .append(getUpdatedAt())
      .append(getCategoryId())
      .append(getFontName())
      .append(getFontUrl())
      .append(getGetMethod())
      .append(getDisplayName())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("TextTemplate {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("name=" + String.valueOf(getName()) + ", ")
      .append("coverUrl=" + String.valueOf(getCoverUrl()) + ", ")
      .append("downloadUrl=" + String.valueOf(getDownloadUrl()) + ", ")
      .append("sort=" + String.valueOf(getSort()) + ", ")
      .append("targetVersionCode=" + String.valueOf(getTargetVersionCode()) + ", ")
      .append("online=" + String.valueOf(getOnline()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()) + ", ")
      .append("categoryID=" + String.valueOf(getCategoryId()) + ", ")
      .append("fontName=" + String.valueOf(getFontName()) + ", ")
      .append("fontUrl=" + String.valueOf(getFontUrl()) + ", ")
      .append("getMethod=" + String.valueOf(getGetMethod()) + ", ")
      .append("displayName=" + String.valueOf(getDisplayName()))
      .append("}")
      .toString();
  }
  
  public static BuildStep builder() {
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
  public static TextTemplate justId(String id) {
    return new TextTemplate(
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
      targetVersionCode,
      online,
      updatedAt,
      categoryID,
      fontName,
      fontUrl,
      getMethod,
      displayName);
  }
  public interface BuildStep {
    TextTemplate build();
    BuildStep id(String id);
    BuildStep name(String name);
    BuildStep coverUrl(String coverUrl);
    BuildStep downloadUrl(String downloadUrl);
    BuildStep sort(Integer sort);
    BuildStep targetVersionCode(Integer targetVersionCode);
    BuildStep online(Integer online);
    BuildStep updatedAt(Temporal.DateTime updatedAt);
    BuildStep categoryId(String categoryId);
    BuildStep fontName(String fontName);
    BuildStep fontUrl(String fontUrl);
    BuildStep getMethod(Integer getMethod);
    BuildStep displayName(String displayName);
  }
  

  public static class Builder implements BuildStep {
    private String id;
    private String name;
    private String coverUrl;
    private String downloadUrl;
    private Integer sort;
    private Integer targetVersionCode;
    private Integer online;
    private Temporal.DateTime updatedAt;
    private String categoryID;
    private String fontName;
    private String fontUrl;
    private Integer getMethod;
    private String displayName;
    @Override
     public TextTemplate build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new TextTemplate(
          id,
          name,
          coverUrl,
          downloadUrl,
          sort,
          targetVersionCode,
          online,
          updatedAt,
          categoryID,
          fontName,
          fontUrl,
          getMethod,
          displayName);
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
     public BuildStep updatedAt(Temporal.DateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }
    
    @Override
     public BuildStep categoryId(String categoryId) {
        this.categoryID = categoryId;
        return this;
    }
    
    @Override
     public BuildStep fontName(String fontName) {
        this.fontName = fontName;
        return this;
    }
    
    @Override
     public BuildStep fontUrl(String fontUrl) {
        this.fontUrl = fontUrl;
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
    private CopyOfBuilder(String id, String name, String coverUrl, String downloadUrl, Integer sort, Integer targetVersionCode, Integer online, Temporal.DateTime updatedAt, String categoryId, String fontName, String fontUrl, Integer getMethod, String displayName) {
      super.id(id);
      super.name(name)
        .coverUrl(coverUrl)
        .downloadUrl(downloadUrl)
        .sort(sort)
        .targetVersionCode(targetVersionCode)
        .online(online)
        .updatedAt(updatedAt)
        .categoryId(categoryId)
        .fontName(fontName)
        .fontUrl(fontUrl)
        .getMethod(getMethod)
        .displayName(displayName);
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
     public CopyOfBuilder updatedAt(Temporal.DateTime updatedAt) {
      return (CopyOfBuilder) super.updatedAt(updatedAt);
    }
    
    @Override
     public CopyOfBuilder categoryId(String categoryId) {
      return (CopyOfBuilder) super.categoryId(categoryId);
    }
    
    @Override
     public CopyOfBuilder fontName(String fontName) {
      return (CopyOfBuilder) super.fontName(fontName);
    }
    
    @Override
     public CopyOfBuilder fontUrl(String fontUrl) {
      return (CopyOfBuilder) super.fontUrl(fontUrl);
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
