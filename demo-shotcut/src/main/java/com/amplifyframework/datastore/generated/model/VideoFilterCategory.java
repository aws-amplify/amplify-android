package com.amplifyframework.datastore.generated.model;

import com.amplifyframework.core.model.annotations.HasMany;
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

/** This is an auto generated class representing the VideoFilterCategory type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "VideoFilterCategories", authRules = {
  @AuthRule(allow = AuthStrategy.PUBLIC, provider = "iam", operations = { ModelOperation.READ }),
  @AuthRule(allow = AuthStrategy.GROUPS, groupClaim = "cognito:groups", groups = { "Editor" }, provider = "userPools", operations = { ModelOperation.READ, ModelOperation.CREATE, ModelOperation.UPDATE, ModelOperation.DELETE }),
  @AuthRule(allow = AuthStrategy.PRIVATE, provider = "iam", operations = { ModelOperation.CREATE, ModelOperation.UPDATE, ModelOperation.DELETE, ModelOperation.READ })
})
public final class VideoFilterCategory implements Model {
  public static final QueryField ID = field("VideoFilterCategory", "id");
  public static final QueryField NAME = field("VideoFilterCategory", "name");
  public static final QueryField COVER_URL = field("VideoFilterCategory", "coverUrl");
  public static final QueryField SORT = field("VideoFilterCategory", "sort");
  public static final QueryField UPDATED_AT = field("VideoFilterCategory", "updatedAt");
  public static final QueryField ONLINE = field("VideoFilterCategory", "online");
  public static final QueryField TEST_TAG = field("VideoFilterCategory", "testTag");
  public static final QueryField MASK_COLOR = field("VideoFilterCategory", "maskColor");
  public static final QueryField GET_METHOD = field("VideoFilterCategory", "getMethod");
  public static final QueryField LABEL = field("VideoFilterCategory", "label");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String") String name;
  private final @ModelField(targetType="String") String coverUrl;
  private final @ModelField(targetType="Int") Integer sort;
  private final @ModelField(targetType="VideoFilter") @HasMany(associatedWith = "categoryID", type = VideoFilter.class) List<VideoFilter> videoFilterSet = null;
  private final @ModelField(targetType="AWSDateTime", isRequired = true) Temporal.DateTime updatedAt;
  private final @ModelField(targetType="Int") Integer online;
  private final @ModelField(targetType="String") String testTag;
  private final @ModelField(targetType="String") String maskColor;
  private final @ModelField(targetType="Int") Integer getMethod;
  private final @ModelField(targetType="String") String label;
  private final @ModelField(targetType="VideoFilterCategoryLocale") @HasMany(associatedWith = "materialID", type = VideoFilterCategoryLocale.class) List<VideoFilterCategoryLocale> VideoFilterCategoryLocales = null;
  public String getId() {
      return id;
  }
  
  public String getName() {
      return name;
  }
  
  public String getCoverUrl() {
      return coverUrl;
  }
  
  public Integer getSort() {
      return sort;
  }
  
  public List<VideoFilter> getVideoFilterSet() {
      return videoFilterSet;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  public Integer getOnline() {
      return online;
  }
  
  public String getTestTag() {
      return testTag;
  }
  
  public String getMaskColor() {
      return maskColor;
  }
  
  public Integer getGetMethod() {
      return getMethod;
  }
  
  public String getLabel() {
      return label;
  }
  
  public List<VideoFilterCategoryLocale> getVideoFilterCategoryLocales() {
      return VideoFilterCategoryLocales;
  }
  
  private VideoFilterCategory(String id, String name, String coverUrl, Integer sort, Temporal.DateTime updatedAt, Integer online, String testTag, String maskColor, Integer getMethod, String label) {
    this.id = id;
    this.name = name;
    this.coverUrl = coverUrl;
    this.sort = sort;
    this.updatedAt = updatedAt;
    this.online = online;
    this.testTag = testTag;
    this.maskColor = maskColor;
    this.getMethod = getMethod;
    this.label = label;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      VideoFilterCategory videoFilterCategory = (VideoFilterCategory) obj;
      return ObjectsCompat.equals(getId(), videoFilterCategory.getId()) &&
              ObjectsCompat.equals(getName(), videoFilterCategory.getName()) &&
              ObjectsCompat.equals(getCoverUrl(), videoFilterCategory.getCoverUrl()) &&
              ObjectsCompat.equals(getSort(), videoFilterCategory.getSort()) &&
              ObjectsCompat.equals(getUpdatedAt(), videoFilterCategory.getUpdatedAt()) &&
              ObjectsCompat.equals(getOnline(), videoFilterCategory.getOnline()) &&
              ObjectsCompat.equals(getTestTag(), videoFilterCategory.getTestTag()) &&
              ObjectsCompat.equals(getMaskColor(), videoFilterCategory.getMaskColor()) &&
              ObjectsCompat.equals(getGetMethod(), videoFilterCategory.getGetMethod()) &&
              ObjectsCompat.equals(getLabel(), videoFilterCategory.getLabel());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getName())
      .append(getCoverUrl())
      .append(getSort())
      .append(getUpdatedAt())
      .append(getOnline())
      .append(getTestTag())
      .append(getMaskColor())
      .append(getGetMethod())
      .append(getLabel())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("VideoFilterCategory {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("name=" + String.valueOf(getName()) + ", ")
      .append("coverUrl=" + String.valueOf(getCoverUrl()) + ", ")
      .append("sort=" + String.valueOf(getSort()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()) + ", ")
      .append("online=" + String.valueOf(getOnline()) + ", ")
      .append("testTag=" + String.valueOf(getTestTag()) + ", ")
      .append("maskColor=" + String.valueOf(getMaskColor()) + ", ")
      .append("getMethod=" + String.valueOf(getGetMethod()) + ", ")
      .append("label=" + String.valueOf(getLabel()))
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
  public static VideoFilterCategory justId(String id) {
    return new VideoFilterCategory(
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
      sort,
      updatedAt,
      online,
      testTag,
      maskColor,
      getMethod,
      label);
  }
  public interface UpdatedAtStep {
    BuildStep updatedAt(Temporal.DateTime updatedAt);
  }
  

  public interface BuildStep {
    VideoFilterCategory build();
    BuildStep id(String id);
    BuildStep name(String name);
    BuildStep coverUrl(String coverUrl);
    BuildStep sort(Integer sort);
    BuildStep online(Integer online);
    BuildStep testTag(String testTag);
    BuildStep maskColor(String maskColor);
    BuildStep getMethod(Integer getMethod);
    BuildStep label(String label);
  }
  

  public static class Builder implements UpdatedAtStep, BuildStep {
    private String id;
    private Temporal.DateTime updatedAt;
    private String name;
    private String coverUrl;
    private Integer sort;
    private Integer online;
    private String testTag;
    private String maskColor;
    private Integer getMethod;
    private String label;
    @Override
     public VideoFilterCategory build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new VideoFilterCategory(
          id,
          name,
          coverUrl,
          sort,
          updatedAt,
          online,
          testTag,
          maskColor,
          getMethod,
          label);
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
     public BuildStep sort(Integer sort) {
        this.sort = sort;
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
     public BuildStep maskColor(String maskColor) {
        this.maskColor = maskColor;
        return this;
    }
    
    @Override
     public BuildStep getMethod(Integer getMethod) {
        this.getMethod = getMethod;
        return this;
    }
    
    @Override
     public BuildStep label(String label) {
        this.label = label;
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
    private CopyOfBuilder(String id, String name, String coverUrl, Integer sort, Temporal.DateTime updatedAt, Integer online, String testTag, String maskColor, Integer getMethod, String label) {
      super.id(id);
      super.updatedAt(updatedAt)
        .name(name)
        .coverUrl(coverUrl)
        .sort(sort)
        .online(online)
        .testTag(testTag)
        .maskColor(maskColor)
        .getMethod(getMethod)
        .label(label);
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
     public CopyOfBuilder sort(Integer sort) {
      return (CopyOfBuilder) super.sort(sort);
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
     public CopyOfBuilder maskColor(String maskColor) {
      return (CopyOfBuilder) super.maskColor(maskColor);
    }
    
    @Override
     public CopyOfBuilder getMethod(Integer getMethod) {
      return (CopyOfBuilder) super.getMethod(getMethod);
    }
    
    @Override
     public CopyOfBuilder label(String label) {
      return (CopyOfBuilder) super.label(label);
    }
  }
  
}
