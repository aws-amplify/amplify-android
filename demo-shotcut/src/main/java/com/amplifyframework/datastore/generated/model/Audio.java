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

/** This is an auto generated class representing the Audio type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "Audio", authRules = {
  @AuthRule(allow = AuthStrategy.PUBLIC, provider = "iam", operations = { ModelOperation.READ }),
  @AuthRule(allow = AuthStrategy.GROUPS, groupClaim = "cognito:groups", groups = { "Editor" }, provider = "userPools", operations = { ModelOperation.READ, ModelOperation.CREATE, ModelOperation.UPDATE, ModelOperation.DELETE }),
  @AuthRule(allow = AuthStrategy.PRIVATE, provider = "iam", operations = { ModelOperation.CREATE, ModelOperation.UPDATE, ModelOperation.DELETE, ModelOperation.READ })
})
@Index(name = "byAudioCategory", fields = {"audioCategoryID"})
public final class Audio implements Model {
  public static final QueryField ID = field("Audio", "id");
  public static final QueryField NAME = field("Audio", "name");
  public static final QueryField COVER_URL = field("Audio", "coverUrl");
  public static final QueryField DOWNLOAD_URL = field("Audio", "downloadUrl");
  public static final QueryField AUTHOR = field("Audio", "author");
  public static final QueryField GENRE = field("Audio", "genre");
  public static final QueryField MOOD = field("Audio", "mood");
  public static final QueryField SORT = field("Audio", "sort");
  public static final QueryField TAG = field("Audio", "tag");
  public static final QueryField AUDIO_CATEGORY_ID = field("Audio", "audioCategoryID");
  public static final QueryField DURATION = field("Audio", "duration");
  public static final QueryField WAVE = field("Audio", "wave");
  public static final QueryField ONLINE = field("Audio", "online");
  public static final QueryField UPDATED_AT = field("Audio", "updatedAt");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String") String name;
  private final @ModelField(targetType="String") String coverUrl;
  private final @ModelField(targetType="String") String downloadUrl;
  private final @ModelField(targetType="String") String author;
  private final @ModelField(targetType="String") String genre;
  private final @ModelField(targetType="String") String mood;
  private final @ModelField(targetType="Int") Integer sort;
  private final @ModelField(targetType="String") String tag;
  private final @ModelField(targetType="ID") String audioCategoryID;
  private final @ModelField(targetType="Int") Integer duration;
  private final @ModelField(targetType="String") String wave;
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
  
  public String getAuthor() {
      return author;
  }
  
  public String getGenre() {
      return genre;
  }
  
  public String getMood() {
      return mood;
  }
  
  public Integer getSort() {
      return sort;
  }
  
  public String getTag() {
      return tag;
  }
  
  public String getAudioCategoryId() {
      return audioCategoryID;
  }
  
  public Integer getDuration() {
      return duration;
  }
  
  public String getWave() {
      return wave;
  }
  
  public Integer getOnline() {
      return online;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  private Audio(String id, String name, String coverUrl, String downloadUrl, String author, String genre, String mood, Integer sort, String tag, String audioCategoryID, Integer duration, String wave, Integer online, Temporal.DateTime updatedAt) {
    this.id = id;
    this.name = name;
    this.coverUrl = coverUrl;
    this.downloadUrl = downloadUrl;
    this.author = author;
    this.genre = genre;
    this.mood = mood;
    this.sort = sort;
    this.tag = tag;
    this.audioCategoryID = audioCategoryID;
    this.duration = duration;
    this.wave = wave;
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
      Audio audio = (Audio) obj;
      return ObjectsCompat.equals(getId(), audio.getId()) &&
              ObjectsCompat.equals(getName(), audio.getName()) &&
              ObjectsCompat.equals(getCoverUrl(), audio.getCoverUrl()) &&
              ObjectsCompat.equals(getDownloadUrl(), audio.getDownloadUrl()) &&
              ObjectsCompat.equals(getAuthor(), audio.getAuthor()) &&
              ObjectsCompat.equals(getGenre(), audio.getGenre()) &&
              ObjectsCompat.equals(getMood(), audio.getMood()) &&
              ObjectsCompat.equals(getSort(), audio.getSort()) &&
              ObjectsCompat.equals(getTag(), audio.getTag()) &&
              ObjectsCompat.equals(getAudioCategoryId(), audio.getAudioCategoryId()) &&
              ObjectsCompat.equals(getDuration(), audio.getDuration()) &&
              ObjectsCompat.equals(getWave(), audio.getWave()) &&
              ObjectsCompat.equals(getOnline(), audio.getOnline()) &&
              ObjectsCompat.equals(getUpdatedAt(), audio.getUpdatedAt());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getName())
      .append(getCoverUrl())
      .append(getDownloadUrl())
      .append(getAuthor())
      .append(getGenre())
      .append(getMood())
      .append(getSort())
      .append(getTag())
      .append(getAudioCategoryId())
      .append(getDuration())
      .append(getWave())
      .append(getOnline())
      .append(getUpdatedAt())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("Audio {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("name=" + String.valueOf(getName()) + ", ")
      .append("coverUrl=" + String.valueOf(getCoverUrl()) + ", ")
      .append("downloadUrl=" + String.valueOf(getDownloadUrl()) + ", ")
      .append("author=" + String.valueOf(getAuthor()) + ", ")
      .append("genre=" + String.valueOf(getGenre()) + ", ")
      .append("mood=" + String.valueOf(getMood()) + ", ")
      .append("sort=" + String.valueOf(getSort()) + ", ")
      .append("tag=" + String.valueOf(getTag()) + ", ")
      .append("audioCategoryID=" + String.valueOf(getAudioCategoryId()) + ", ")
      .append("duration=" + String.valueOf(getDuration()) + ", ")
      .append("wave=" + String.valueOf(getWave()) + ", ")
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
  public static Audio justId(String id) {
    return new Audio(
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
      author,
      genre,
      mood,
      sort,
      tag,
      audioCategoryID,
      duration,
      wave,
      online,
      updatedAt);
  }
  public interface UpdatedAtStep {
    BuildStep updatedAt(Temporal.DateTime updatedAt);
  }
  

  public interface BuildStep {
    Audio build();
    BuildStep id(String id);
    BuildStep name(String name);
    BuildStep coverUrl(String coverUrl);
    BuildStep downloadUrl(String downloadUrl);
    BuildStep author(String author);
    BuildStep genre(String genre);
    BuildStep mood(String mood);
    BuildStep sort(Integer sort);
    BuildStep tag(String tag);
    BuildStep audioCategoryId(String audioCategoryId);
    BuildStep duration(Integer duration);
    BuildStep wave(String wave);
    BuildStep online(Integer online);
  }
  

  public static class Builder implements UpdatedAtStep, BuildStep {
    private String id;
    private Temporal.DateTime updatedAt;
    private String name;
    private String coverUrl;
    private String downloadUrl;
    private String author;
    private String genre;
    private String mood;
    private Integer sort;
    private String tag;
    private String audioCategoryID;
    private Integer duration;
    private String wave;
    private Integer online;
    @Override
     public Audio build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new Audio(
          id,
          name,
          coverUrl,
          downloadUrl,
          author,
          genre,
          mood,
          sort,
          tag,
          audioCategoryID,
          duration,
          wave,
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
     public BuildStep author(String author) {
        this.author = author;
        return this;
    }
    
    @Override
     public BuildStep genre(String genre) {
        this.genre = genre;
        return this;
    }
    
    @Override
     public BuildStep mood(String mood) {
        this.mood = mood;
        return this;
    }
    
    @Override
     public BuildStep sort(Integer sort) {
        this.sort = sort;
        return this;
    }
    
    @Override
     public BuildStep tag(String tag) {
        this.tag = tag;
        return this;
    }
    
    @Override
     public BuildStep audioCategoryId(String audioCategoryId) {
        this.audioCategoryID = audioCategoryId;
        return this;
    }
    
    @Override
     public BuildStep duration(Integer duration) {
        this.duration = duration;
        return this;
    }
    
    @Override
     public BuildStep wave(String wave) {
        this.wave = wave;
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
    private CopyOfBuilder(String id, String name, String coverUrl, String downloadUrl, String author, String genre, String mood, Integer sort, String tag, String audioCategoryId, Integer duration, String wave, Integer online, Temporal.DateTime updatedAt) {
      super.id(id);
      super.updatedAt(updatedAt)
        .name(name)
        .coverUrl(coverUrl)
        .downloadUrl(downloadUrl)
        .author(author)
        .genre(genre)
        .mood(mood)
        .sort(sort)
        .tag(tag)
        .audioCategoryId(audioCategoryId)
        .duration(duration)
        .wave(wave)
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
     public CopyOfBuilder author(String author) {
      return (CopyOfBuilder) super.author(author);
    }
    
    @Override
     public CopyOfBuilder genre(String genre) {
      return (CopyOfBuilder) super.genre(genre);
    }
    
    @Override
     public CopyOfBuilder mood(String mood) {
      return (CopyOfBuilder) super.mood(mood);
    }
    
    @Override
     public CopyOfBuilder sort(Integer sort) {
      return (CopyOfBuilder) super.sort(sort);
    }
    
    @Override
     public CopyOfBuilder tag(String tag) {
      return (CopyOfBuilder) super.tag(tag);
    }
    
    @Override
     public CopyOfBuilder audioCategoryId(String audioCategoryId) {
      return (CopyOfBuilder) super.audioCategoryId(audioCategoryId);
    }
    
    @Override
     public CopyOfBuilder duration(Integer duration) {
      return (CopyOfBuilder) super.duration(duration);
    }
    
    @Override
     public CopyOfBuilder wave(String wave) {
      return (CopyOfBuilder) super.wave(wave);
    }
    
    @Override
     public CopyOfBuilder online(Integer online) {
      return (CopyOfBuilder) super.online(online);
    }
  }
  
}
