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

/** This is an auto generated class representing the TextAnimCategory type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "TextAnimCategories", authRules = {
  @AuthRule(allow = AuthStrategy.PUBLIC, provider = "iam", operations = { ModelOperation.READ }),
  @AuthRule(allow = AuthStrategy.GROUPS, groupClaim = "cognito:groups", groups = { "Editor" }, provider = "userPools", operations = { ModelOperation.READ, ModelOperation.CREATE, ModelOperation.UPDATE, ModelOperation.DELETE }),
  @AuthRule(allow = AuthStrategy.PRIVATE, provider = "iam", operations = { ModelOperation.CREATE, ModelOperation.UPDATE, ModelOperation.DELETE, ModelOperation.READ })
})
public final class TextAnimCategory implements Model {
  public static final QueryField ID = field("TextAnimCategory", "id");
  public static final QueryField NAME = field("TextAnimCategory", "name");
  public static final QueryField COVER_URL = field("TextAnimCategory", "coverUrl");
  public static final QueryField SORT = field("TextAnimCategory", "sort");
  public static final QueryField ONLINE = field("TextAnimCategory", "online");
  public static final QueryField UPDATED_AT = field("TextAnimCategory", "updatedAt");
  public static final QueryField ANIM_TYPE = field("TextAnimCategory", "animType");
  public static final QueryField DISPLAY_NAME = field("TextAnimCategory", "displayName");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String") String name;
  private final @ModelField(targetType="String") String coverUrl;
  private final @ModelField(targetType="Int") Integer sort;
  private final @ModelField(targetType="Int") Integer online;
  private final @ModelField(targetType="TextAnim") @HasMany(associatedWith = "categoryID", type = TextAnim.class) List<TextAnim> TextAnimSet = null;
  private final @ModelField(targetType="AWSDateTime", isRequired = true) Temporal.DateTime updatedAt;
  private final @ModelField(targetType="Int") Integer animType;
  private final @ModelField(targetType="TextAnimCategoryLocale") @HasMany(associatedWith = "materialID", type = TextAnimCategoryLocale.class) List<TextAnimCategoryLocale> TextAnimCategoryLocales = null;
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
  
  public Integer getSort() {
      return sort;
  }
  
  public Integer getOnline() {
      return online;
  }
  
  public List<TextAnim> getTextAnimSet() {
      return TextAnimSet;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  public Integer getAnimType() {
      return animType;
  }
  
  public List<TextAnimCategoryLocale> getTextAnimCategoryLocales() {
      return TextAnimCategoryLocales;
  }
  
  public String getDisplayName() {
      return displayName;
  }
  
  private TextAnimCategory(String id, String name, String coverUrl, Integer sort, Integer online, Temporal.DateTime updatedAt, Integer animType, String displayName) {
    this.id = id;
    this.name = name;
    this.coverUrl = coverUrl;
    this.sort = sort;
    this.online = online;
    this.updatedAt = updatedAt;
    this.animType = animType;
    this.displayName = displayName;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      TextAnimCategory textAnimCategory = (TextAnimCategory) obj;
      return ObjectsCompat.equals(getId(), textAnimCategory.getId()) &&
              ObjectsCompat.equals(getName(), textAnimCategory.getName()) &&
              ObjectsCompat.equals(getCoverUrl(), textAnimCategory.getCoverUrl()) &&
              ObjectsCompat.equals(getSort(), textAnimCategory.getSort()) &&
              ObjectsCompat.equals(getOnline(), textAnimCategory.getOnline()) &&
              ObjectsCompat.equals(getUpdatedAt(), textAnimCategory.getUpdatedAt()) &&
              ObjectsCompat.equals(getAnimType(), textAnimCategory.getAnimType()) &&
              ObjectsCompat.equals(getDisplayName(), textAnimCategory.getDisplayName());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getName())
      .append(getCoverUrl())
      .append(getSort())
      .append(getOnline())
      .append(getUpdatedAt())
      .append(getAnimType())
      .append(getDisplayName())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("TextAnimCategory {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("name=" + String.valueOf(getName()) + ", ")
      .append("coverUrl=" + String.valueOf(getCoverUrl()) + ", ")
      .append("sort=" + String.valueOf(getSort()) + ", ")
      .append("online=" + String.valueOf(getOnline()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()) + ", ")
      .append("animType=" + String.valueOf(getAnimType()) + ", ")
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
  public static TextAnimCategory justId(String id) {
    return new TextAnimCategory(
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
      online,
      updatedAt,
      animType,
      displayName);
  }
  public interface UpdatedAtStep {
    BuildStep updatedAt(Temporal.DateTime updatedAt);
  }
  

  public interface BuildStep {
    TextAnimCategory build();
    BuildStep id(String id);
    BuildStep name(String name);
    BuildStep coverUrl(String coverUrl);
    BuildStep sort(Integer sort);
    BuildStep online(Integer online);
    BuildStep animType(Integer animType);
    BuildStep displayName(String displayName);
  }
  

  public static class Builder implements UpdatedAtStep, BuildStep {
    private String id;
    private Temporal.DateTime updatedAt;
    private String name;
    private String coverUrl;
    private Integer sort;
    private Integer online;
    private Integer animType;
    private String displayName;
    @Override
     public TextAnimCategory build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new TextAnimCategory(
          id,
          name,
          coverUrl,
          sort,
          online,
          updatedAt,
          animType,
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
     public BuildStep animType(Integer animType) {
        this.animType = animType;
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
    private CopyOfBuilder(String id, String name, String coverUrl, Integer sort, Integer online, Temporal.DateTime updatedAt, Integer animType, String displayName) {
      super.id(id);
      super.updatedAt(updatedAt)
        .name(name)
        .coverUrl(coverUrl)
        .sort(sort)
        .online(online)
        .animType(animType)
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
     public CopyOfBuilder sort(Integer sort) {
      return (CopyOfBuilder) super.sort(sort);
    }
    
    @Override
     public CopyOfBuilder online(Integer online) {
      return (CopyOfBuilder) super.online(online);
    }
    
    @Override
     public CopyOfBuilder animType(Integer animType) {
      return (CopyOfBuilder) super.animType(animType);
    }
    
    @Override
     public CopyOfBuilder displayName(String displayName) {
      return (CopyOfBuilder) super.displayName(displayName);
    }
  }
  
}
