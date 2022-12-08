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

/** This is an auto generated class representing the OverlayMedia type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "OverlayMedias", authRules = {
  @AuthRule(allow = AuthStrategy.PUBLIC, provider = "iam", operations = { ModelOperation.READ }),
  @AuthRule(allow = AuthStrategy.PRIVATE, provider = "iam", operations = { ModelOperation.CREATE, ModelOperation.UPDATE, ModelOperation.DELETE, ModelOperation.READ })
})
@Index(name = "byOverlayMediaCategory", fields = {"overlaymediacategoryID"})
public final class OverlayMedia implements Model {
  public static final QueryField ID = field("OverlayMedia", "id");
  public static final QueryField NAME = field("OverlayMedia", "name");
  public static final QueryField COVER_URL = field("OverlayMedia", "coverUrl");
  public static final QueryField SORT = field("OverlayMedia", "sort");
  public static final QueryField DOWNLOAD_URL = field("OverlayMedia", "downloadUrl");
  public static final QueryField DURATION = field("OverlayMedia", "duration");
  public static final QueryField OVERLAYMEDIACATEGORY_ID = field("OverlayMedia", "overlaymediacategoryID");
  public static final QueryField UPDATED_AT = field("OverlayMedia", "updatedAt");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String") String name;
  private final @ModelField(targetType="String") String coverUrl;
  private final @ModelField(targetType="Int") Integer sort;
  private final @ModelField(targetType="String") String downloadUrl;
  private final @ModelField(targetType="Int") Integer duration;
  private final @ModelField(targetType="ID") String overlaymediacategoryID;
  private final @ModelField(targetType="AWSDateTime") Temporal.DateTime updatedAt;
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
  
  public Integer getDuration() {
      return duration;
  }
  
  public String getOverlaymediacategoryId() {
      return overlaymediacategoryID;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  private OverlayMedia(String id, String name, String coverUrl, Integer sort, String downloadUrl, Integer duration, String overlaymediacategoryID, Temporal.DateTime updatedAt) {
    this.id = id;
    this.name = name;
    this.coverUrl = coverUrl;
    this.sort = sort;
    this.downloadUrl = downloadUrl;
    this.duration = duration;
    this.overlaymediacategoryID = overlaymediacategoryID;
    this.updatedAt = updatedAt;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      OverlayMedia overlayMedia = (OverlayMedia) obj;
      return ObjectsCompat.equals(getId(), overlayMedia.getId()) &&
              ObjectsCompat.equals(getName(), overlayMedia.getName()) &&
              ObjectsCompat.equals(getCoverUrl(), overlayMedia.getCoverUrl()) &&
              ObjectsCompat.equals(getSort(), overlayMedia.getSort()) &&
              ObjectsCompat.equals(getDownloadUrl(), overlayMedia.getDownloadUrl()) &&
              ObjectsCompat.equals(getDuration(), overlayMedia.getDuration()) &&
              ObjectsCompat.equals(getOverlaymediacategoryId(), overlayMedia.getOverlaymediacategoryId()) &&
              ObjectsCompat.equals(getUpdatedAt(), overlayMedia.getUpdatedAt());
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
      .append(getDuration())
      .append(getOverlaymediacategoryId())
      .append(getUpdatedAt())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("OverlayMedia {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("name=" + String.valueOf(getName()) + ", ")
      .append("coverUrl=" + String.valueOf(getCoverUrl()) + ", ")
      .append("sort=" + String.valueOf(getSort()) + ", ")
      .append("downloadUrl=" + String.valueOf(getDownloadUrl()) + ", ")
      .append("duration=" + String.valueOf(getDuration()) + ", ")
      .append("overlaymediacategoryID=" + String.valueOf(getOverlaymediacategoryId()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()))
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
  public static OverlayMedia justId(String id) {
    return new OverlayMedia(
      id,
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
      duration,
      overlaymediacategoryID,
      updatedAt);
  }
  public interface BuildStep {
    OverlayMedia build();
    BuildStep id(String id);
    BuildStep name(String name);
    BuildStep coverUrl(String coverUrl);
    BuildStep sort(Integer sort);
    BuildStep downloadUrl(String downloadUrl);
    BuildStep duration(Integer duration);
    BuildStep overlaymediacategoryId(String overlaymediacategoryId);
    BuildStep updatedAt(Temporal.DateTime updatedAt);
  }
  

  public static class Builder implements BuildStep {
    private String id;
    private String name;
    private String coverUrl;
    private Integer sort;
    private String downloadUrl;
    private Integer duration;
    private String overlaymediacategoryID;
    private Temporal.DateTime updatedAt;
    @Override
     public OverlayMedia build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new OverlayMedia(
          id,
          name,
          coverUrl,
          sort,
          downloadUrl,
          duration,
          overlaymediacategoryID,
          updatedAt);
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
     public BuildStep duration(Integer duration) {
        this.duration = duration;
        return this;
    }
    
    @Override
     public BuildStep overlaymediacategoryId(String overlaymediacategoryId) {
        this.overlaymediacategoryID = overlaymediacategoryId;
        return this;
    }
    
    @Override
     public BuildStep updatedAt(Temporal.DateTime updatedAt) {
        this.updatedAt = updatedAt;
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
    private CopyOfBuilder(String id, String name, String coverUrl, Integer sort, String downloadUrl, Integer duration, String overlaymediacategoryId, Temporal.DateTime updatedAt) {
      super.id(id);
      super.name(name)
        .coverUrl(coverUrl)
        .sort(sort)
        .downloadUrl(downloadUrl)
        .duration(duration)
        .overlaymediacategoryId(overlaymediacategoryId)
        .updatedAt(updatedAt);
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
     public CopyOfBuilder duration(Integer duration) {
      return (CopyOfBuilder) super.duration(duration);
    }
    
    @Override
     public CopyOfBuilder overlaymediacategoryId(String overlaymediacategoryId) {
      return (CopyOfBuilder) super.overlaymediacategoryId(overlaymediacategoryId);
    }
    
    @Override
     public CopyOfBuilder updatedAt(Temporal.DateTime updatedAt) {
      return (CopyOfBuilder) super.updatedAt(updatedAt);
    }
  }
  
}
