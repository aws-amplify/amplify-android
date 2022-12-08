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

/** This is an auto generated class representing the Font2 type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "Font2s", authRules = {
  @AuthRule(allow = AuthStrategy.PUBLIC, provider = "iam", operations = { ModelOperation.READ }),
  @AuthRule(allow = AuthStrategy.GROUPS, groupClaim = "cognito:groups", groups = { "Editor" }, provider = "userPools", operations = { ModelOperation.READ, ModelOperation.CREATE, ModelOperation.UPDATE, ModelOperation.DELETE }),
  @AuthRule(allow = AuthStrategy.PRIVATE, provider = "iam", operations = { ModelOperation.CREATE, ModelOperation.UPDATE, ModelOperation.DELETE, ModelOperation.READ })
})
public final class Font2 implements Model {
  public static final QueryField ID = field("Font2", "id");
  public static final QueryField NAME = field("Font2", "name");
  public static final QueryField DOWNLOAD_URL = field("Font2", "downloadUrl");
  public static final QueryField SORT = field("Font2", "sort");
  public static final QueryField COVER_URL = field("Font2", "coverUrl");
  public static final QueryField UPDATED_AT = field("Font2", "updatedAt");
  public static final QueryField LANGUAGE = field("Font2", "language");
  public static final QueryField BUILD_IN = field("Font2", "buildIn");
  public static final QueryField ONLINE = field("Font2", "online");
  public static final QueryField CATEGORY = field("Font2", "category");
  public static final QueryField LANG_CODE = field("Font2", "langCode");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String", isRequired = true) String name;
  private final @ModelField(targetType="String", isRequired = true) String downloadUrl;
  private final @ModelField(targetType="Int") Integer sort;
  private final @ModelField(targetType="String") String coverUrl;
  private final @ModelField(targetType="AWSDateTime") Temporal.DateTime updatedAt;
  private final @ModelField(targetType="String") String language;
  private final @ModelField(targetType="Int") Integer buildIn;
  private final @ModelField(targetType="Int") Integer online;
  private final @ModelField(targetType="String") String category;
  private final @ModelField(targetType="String") String langCode;
  public String getId() {
      return id;
  }
  
  public String getName() {
      return name;
  }
  
  public String getDownloadUrl() {
      return downloadUrl;
  }
  
  public Integer getSort() {
      return sort;
  }
  
  public String getCoverUrl() {
      return coverUrl;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  public String getLanguage() {
      return language;
  }
  
  public Integer getBuildIn() {
      return buildIn;
  }
  
  public Integer getOnline() {
      return online;
  }
  
  public String getCategory() {
      return category;
  }
  
  public String getLangCode() {
      return langCode;
  }
  
  private Font2(String id, String name, String downloadUrl, Integer sort, String coverUrl, Temporal.DateTime updatedAt, String language, Integer buildIn, Integer online, String category, String langCode) {
    this.id = id;
    this.name = name;
    this.downloadUrl = downloadUrl;
    this.sort = sort;
    this.coverUrl = coverUrl;
    this.updatedAt = updatedAt;
    this.language = language;
    this.buildIn = buildIn;
    this.online = online;
    this.category = category;
    this.langCode = langCode;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      Font2 font2 = (Font2) obj;
      return ObjectsCompat.equals(getId(), font2.getId()) &&
              ObjectsCompat.equals(getName(), font2.getName()) &&
              ObjectsCompat.equals(getDownloadUrl(), font2.getDownloadUrl()) &&
              ObjectsCompat.equals(getSort(), font2.getSort()) &&
              ObjectsCompat.equals(getCoverUrl(), font2.getCoverUrl()) &&
              ObjectsCompat.equals(getUpdatedAt(), font2.getUpdatedAt()) &&
              ObjectsCompat.equals(getLanguage(), font2.getLanguage()) &&
              ObjectsCompat.equals(getBuildIn(), font2.getBuildIn()) &&
              ObjectsCompat.equals(getOnline(), font2.getOnline()) &&
              ObjectsCompat.equals(getCategory(), font2.getCategory()) &&
              ObjectsCompat.equals(getLangCode(), font2.getLangCode());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getName())
      .append(getDownloadUrl())
      .append(getSort())
      .append(getCoverUrl())
      .append(getUpdatedAt())
      .append(getLanguage())
      .append(getBuildIn())
      .append(getOnline())
      .append(getCategory())
      .append(getLangCode())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("Font2 {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("name=" + String.valueOf(getName()) + ", ")
      .append("downloadUrl=" + String.valueOf(getDownloadUrl()) + ", ")
      .append("sort=" + String.valueOf(getSort()) + ", ")
      .append("coverUrl=" + String.valueOf(getCoverUrl()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()) + ", ")
      .append("language=" + String.valueOf(getLanguage()) + ", ")
      .append("buildIn=" + String.valueOf(getBuildIn()) + ", ")
      .append("online=" + String.valueOf(getOnline()) + ", ")
      .append("category=" + String.valueOf(getCategory()) + ", ")
      .append("langCode=" + String.valueOf(getLangCode()))
      .append("}")
      .toString();
  }
  
  public static NameStep builder() {
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
  public static Font2 justId(String id) {
    return new Font2(
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
      downloadUrl,
      sort,
      coverUrl,
      updatedAt,
      language,
      buildIn,
      online,
      category,
      langCode);
  }
  public interface NameStep {
    DownloadUrlStep name(String name);
  }
  

  public interface DownloadUrlStep {
    BuildStep downloadUrl(String downloadUrl);
  }
  

  public interface BuildStep {
    Font2 build();
    BuildStep id(String id);
    BuildStep sort(Integer sort);
    BuildStep coverUrl(String coverUrl);
    BuildStep updatedAt(Temporal.DateTime updatedAt);
    BuildStep language(String language);
    BuildStep buildIn(Integer buildIn);
    BuildStep online(Integer online);
    BuildStep category(String category);
    BuildStep langCode(String langCode);
  }
  

  public static class Builder implements NameStep, DownloadUrlStep, BuildStep {
    private String id;
    private String name;
    private String downloadUrl;
    private Integer sort;
    private String coverUrl;
    private Temporal.DateTime updatedAt;
    private String language;
    private Integer buildIn;
    private Integer online;
    private String category;
    private String langCode;
    @Override
     public Font2 build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new Font2(
          id,
          name,
          downloadUrl,
          sort,
          coverUrl,
          updatedAt,
          language,
          buildIn,
          online,
          category,
          langCode);
    }
    
    @Override
     public DownloadUrlStep name(String name) {
        Objects.requireNonNull(name);
        this.name = name;
        return this;
    }
    
    @Override
     public BuildStep downloadUrl(String downloadUrl) {
        Objects.requireNonNull(downloadUrl);
        this.downloadUrl = downloadUrl;
        return this;
    }
    
    @Override
     public BuildStep sort(Integer sort) {
        this.sort = sort;
        return this;
    }
    
    @Override
     public BuildStep coverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
        return this;
    }
    
    @Override
     public BuildStep updatedAt(Temporal.DateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }
    
    @Override
     public BuildStep language(String language) {
        this.language = language;
        return this;
    }
    
    @Override
     public BuildStep buildIn(Integer buildIn) {
        this.buildIn = buildIn;
        return this;
    }
    
    @Override
     public BuildStep online(Integer online) {
        this.online = online;
        return this;
    }
    
    @Override
     public BuildStep category(String category) {
        this.category = category;
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
    private CopyOfBuilder(String id, String name, String downloadUrl, Integer sort, String coverUrl, Temporal.DateTime updatedAt, String language, Integer buildIn, Integer online, String category, String langCode) {
      super.id(id);
      super.name(name)
        .downloadUrl(downloadUrl)
        .sort(sort)
        .coverUrl(coverUrl)
        .updatedAt(updatedAt)
        .language(language)
        .buildIn(buildIn)
        .online(online)
        .category(category)
        .langCode(langCode);
    }
    
    @Override
     public CopyOfBuilder name(String name) {
      return (CopyOfBuilder) super.name(name);
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
     public CopyOfBuilder coverUrl(String coverUrl) {
      return (CopyOfBuilder) super.coverUrl(coverUrl);
    }
    
    @Override
     public CopyOfBuilder updatedAt(Temporal.DateTime updatedAt) {
      return (CopyOfBuilder) super.updatedAt(updatedAt);
    }
    
    @Override
     public CopyOfBuilder language(String language) {
      return (CopyOfBuilder) super.language(language);
    }
    
    @Override
     public CopyOfBuilder buildIn(Integer buildIn) {
      return (CopyOfBuilder) super.buildIn(buildIn);
    }
    
    @Override
     public CopyOfBuilder online(Integer online) {
      return (CopyOfBuilder) super.online(online);
    }
    
    @Override
     public CopyOfBuilder category(String category) {
      return (CopyOfBuilder) super.category(category);
    }
    
    @Override
     public CopyOfBuilder langCode(String langCode) {
      return (CopyOfBuilder) super.langCode(langCode);
    }
  }
  
}
