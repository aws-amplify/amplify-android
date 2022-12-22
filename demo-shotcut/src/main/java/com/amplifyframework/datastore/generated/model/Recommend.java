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

/** This is an auto generated class representing the Recommend type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "Recommends", authRules = {
  @AuthRule(allow = AuthStrategy.PUBLIC, provider = "iam", operations = { ModelOperation.READ }),
  @AuthRule(allow = AuthStrategy.PRIVATE, provider = "iam", operations = { ModelOperation.CREATE, ModelOperation.UPDATE, ModelOperation.DELETE, ModelOperation.READ })
})
@Index(name = "byRecommendCategory", fields = {"recommendCategoryID"})
public final class Recommend implements Model {
  public static final QueryField ID = field("Recommend", "id");
  public static final QueryField NAME = field("Recommend", "name");
  public static final QueryField COVER_URL = field("Recommend", "coverUrl");
  public static final QueryField SORT = field("Recommend", "sort");
  public static final QueryField DOWNLOAD_URL = field("Recommend", "downloadUrl");
  public static final QueryField PREVIEW_VIDEO_URL = field("Recommend", "previewVideoUrl");
  public static final QueryField UPDATED_AT = field("Recommend", "updatedAt");
  public static final QueryField COUNTRY = field("Recommend", "country");
  public static final QueryField RECOMMEND_CATEGORY_ID = field("Recommend", "recommendCategoryID");
  public static final QueryField RESOURCE_NAME = field("Recommend", "resourceName");
  public static final QueryField ONLINE = field("Recommend", "online");
  public static final QueryField LABEL = field("Recommend", "label");
  public static final QueryField GET_METHOD = field("Recommend", "getMethod");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String") String name;
  private final @ModelField(targetType="String") String coverUrl;
  private final @ModelField(targetType="Int") Integer sort;
  private final @ModelField(targetType="String") String downloadUrl;
  private final @ModelField(targetType="String") String previewVideoUrl;
  private final @ModelField(targetType="AWSDateTime") Temporal.DateTime updatedAt;
  private final @ModelField(targetType="String") String country;
  private final @ModelField(targetType="ID", isRequired = true) String recommendCategoryID;
  private final @ModelField(targetType="String") String resourceName;
  private final @ModelField(targetType="Int") Integer online;
  private final @ModelField(targetType="String") String label;
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
  
  public Integer getSort() {
      return sort;
  }
  
  public String getDownloadUrl() {
      return downloadUrl;
  }
  
  public String getPreviewVideoUrl() {
      return previewVideoUrl;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  public String getCountry() {
      return country;
  }
  
  public String getRecommendCategoryId() {
      return recommendCategoryID;
  }
  
  public String getResourceName() {
      return resourceName;
  }
  
  public Integer getOnline() {
      return online;
  }
  
  public String getLabel() {
      return label;
  }
  
  public Integer getGetMethod() {
      return getMethod;
  }
  
  private Recommend(String id, String name, String coverUrl, Integer sort, String downloadUrl, String previewVideoUrl, Temporal.DateTime updatedAt, String country, String recommendCategoryID, String resourceName, Integer online, String label, Integer getMethod) {
    this.id = id;
    this.name = name;
    this.coverUrl = coverUrl;
    this.sort = sort;
    this.downloadUrl = downloadUrl;
    this.previewVideoUrl = previewVideoUrl;
    this.updatedAt = updatedAt;
    this.country = country;
    this.recommendCategoryID = recommendCategoryID;
    this.resourceName = resourceName;
    this.online = online;
    this.label = label;
    this.getMethod = getMethod;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      Recommend recommend = (Recommend) obj;
      return ObjectsCompat.equals(getId(), recommend.getId()) &&
              ObjectsCompat.equals(getName(), recommend.getName()) &&
              ObjectsCompat.equals(getCoverUrl(), recommend.getCoverUrl()) &&
              ObjectsCompat.equals(getSort(), recommend.getSort()) &&
              ObjectsCompat.equals(getDownloadUrl(), recommend.getDownloadUrl()) &&
              ObjectsCompat.equals(getPreviewVideoUrl(), recommend.getPreviewVideoUrl()) &&
              ObjectsCompat.equals(getUpdatedAt(), recommend.getUpdatedAt()) &&
              ObjectsCompat.equals(getCountry(), recommend.getCountry()) &&
              ObjectsCompat.equals(getRecommendCategoryId(), recommend.getRecommendCategoryId()) &&
              ObjectsCompat.equals(getResourceName(), recommend.getResourceName()) &&
              ObjectsCompat.equals(getOnline(), recommend.getOnline()) &&
              ObjectsCompat.equals(getLabel(), recommend.getLabel()) &&
              ObjectsCompat.equals(getGetMethod(), recommend.getGetMethod());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getName())
      .append(getCoverUrl())
      .append(getSort())
      .append(getDownloadUrl())
      .append(getPreviewVideoUrl())
      .append(getUpdatedAt())
      .append(getCountry())
      .append(getRecommendCategoryId())
      .append(getResourceName())
      .append(getOnline())
      .append(getLabel())
      .append(getGetMethod())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("Recommend {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("name=" + String.valueOf(getName()) + ", ")
      .append("coverUrl=" + String.valueOf(getCoverUrl()) + ", ")
      .append("sort=" + String.valueOf(getSort()) + ", ")
      .append("downloadUrl=" + String.valueOf(getDownloadUrl()) + ", ")
      .append("previewVideoUrl=" + String.valueOf(getPreviewVideoUrl()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()) + ", ")
      .append("country=" + String.valueOf(getCountry()) + ", ")
      .append("recommendCategoryID=" + String.valueOf(getRecommendCategoryId()) + ", ")
      .append("resourceName=" + String.valueOf(getResourceName()) + ", ")
      .append("online=" + String.valueOf(getOnline()) + ", ")
      .append("label=" + String.valueOf(getLabel()) + ", ")
      .append("getMethod=" + String.valueOf(getGetMethod()))
      .append("}")
      .toString();
  }
  
  public static RecommendCategoryIdStep builder() {
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
  public static Recommend justId(String id) {
    return new Recommend(
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
      sort,
      downloadUrl,
      previewVideoUrl,
      updatedAt,
      country,
      recommendCategoryID,
      resourceName,
      online,
      label,
      getMethod);
  }
  public interface RecommendCategoryIdStep {
    BuildStep recommendCategoryId(String recommendCategoryId);
  }
  

  public interface BuildStep {
    Recommend build();
    BuildStep id(String id);
    BuildStep name(String name);
    BuildStep coverUrl(String coverUrl);
    BuildStep sort(Integer sort);
    BuildStep downloadUrl(String downloadUrl);
    BuildStep previewVideoUrl(String previewVideoUrl);
    BuildStep updatedAt(Temporal.DateTime updatedAt);
    BuildStep country(String country);
    BuildStep resourceName(String resourceName);
    BuildStep online(Integer online);
    BuildStep label(String label);
    BuildStep getMethod(Integer getMethod);
  }
  

  public static class Builder implements RecommendCategoryIdStep, BuildStep {
    private String id;
    private String recommendCategoryID;
    private String name;
    private String coverUrl;
    private Integer sort;
    private String downloadUrl;
    private String previewVideoUrl;
    private Temporal.DateTime updatedAt;
    private String country;
    private String resourceName;
    private Integer online;
    private String label;
    private Integer getMethod;
    @Override
     public Recommend build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new Recommend(
          id,
          name,
          coverUrl,
          sort,
          downloadUrl,
          previewVideoUrl,
          updatedAt,
          country,
          recommendCategoryID,
          resourceName,
          online,
          label,
          getMethod);
    }
    
    @Override
     public BuildStep recommendCategoryId(String recommendCategoryId) {
        Objects.requireNonNull(recommendCategoryId);
        this.recommendCategoryID = recommendCategoryId;
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
     public BuildStep downloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
        return this;
    }
    
    @Override
     public BuildStep previewVideoUrl(String previewVideoUrl) {
        this.previewVideoUrl = previewVideoUrl;
        return this;
    }
    
    @Override
     public BuildStep updatedAt(Temporal.DateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }
    
    @Override
     public BuildStep country(String country) {
        this.country = country;
        return this;
    }
    
    @Override
     public BuildStep resourceName(String resourceName) {
        this.resourceName = resourceName;
        return this;
    }
    
    @Override
     public BuildStep online(Integer online) {
        this.online = online;
        return this;
    }
    
    @Override
     public BuildStep label(String label) {
        this.label = label;
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
    private CopyOfBuilder(String id, String name, String coverUrl, Integer sort, String downloadUrl, String previewVideoUrl, Temporal.DateTime updatedAt, String country, String recommendCategoryId, String resourceName, Integer online, String label, Integer getMethod) {
      super.id(id);
      super.recommendCategoryId(recommendCategoryId)
        .name(name)
        .coverUrl(coverUrl)
        .sort(sort)
        .downloadUrl(downloadUrl)
        .previewVideoUrl(previewVideoUrl)
        .updatedAt(updatedAt)
        .country(country)
        .resourceName(resourceName)
        .online(online)
        .label(label)
        .getMethod(getMethod);
    }
    
    @Override
     public CopyOfBuilder recommendCategoryId(String recommendCategoryId) {
      return (CopyOfBuilder) super.recommendCategoryId(recommendCategoryId);
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
     public CopyOfBuilder downloadUrl(String downloadUrl) {
      return (CopyOfBuilder) super.downloadUrl(downloadUrl);
    }
    
    @Override
     public CopyOfBuilder previewVideoUrl(String previewVideoUrl) {
      return (CopyOfBuilder) super.previewVideoUrl(previewVideoUrl);
    }
    
    @Override
     public CopyOfBuilder updatedAt(Temporal.DateTime updatedAt) {
      return (CopyOfBuilder) super.updatedAt(updatedAt);
    }
    
    @Override
     public CopyOfBuilder country(String country) {
      return (CopyOfBuilder) super.country(country);
    }
    
    @Override
     public CopyOfBuilder resourceName(String resourceName) {
      return (CopyOfBuilder) super.resourceName(resourceName);
    }
    
    @Override
     public CopyOfBuilder online(Integer online) {
      return (CopyOfBuilder) super.online(online);
    }
    
    @Override
     public CopyOfBuilder label(String label) {
      return (CopyOfBuilder) super.label(label);
    }
    
    @Override
     public CopyOfBuilder getMethod(Integer getMethod) {
      return (CopyOfBuilder) super.getMethod(getMethod);
    }
  }
  
}
