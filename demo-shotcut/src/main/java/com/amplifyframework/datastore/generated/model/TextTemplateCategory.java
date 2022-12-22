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

/** This is an auto generated class representing the TextTemplateCategory type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "TextTemplateCategories", authRules = {
  @AuthRule(allow = AuthStrategy.PUBLIC, provider = "iam", operations = { ModelOperation.READ }),
  @AuthRule(allow = AuthStrategy.GROUPS, groupClaim = "cognito:groups", groups = { "Editor" }, provider = "userPools", operations = { ModelOperation.READ, ModelOperation.CREATE, ModelOperation.UPDATE, ModelOperation.DELETE }),
  @AuthRule(allow = AuthStrategy.PRIVATE, provider = "iam", operations = { ModelOperation.CREATE, ModelOperation.UPDATE, ModelOperation.DELETE, ModelOperation.READ })
})
public final class TextTemplateCategory implements Model {
  public static final QueryField ID = field("TextTemplateCategory", "id");
  public static final QueryField NAME = field("TextTemplateCategory", "name");
  public static final QueryField COVER_URL = field("TextTemplateCategory", "coverUrl");
  public static final QueryField SORT = field("TextTemplateCategory", "sort");
  public static final QueryField ONLINE = field("TextTemplateCategory", "online");
  public static final QueryField UPDATED_AT = field("TextTemplateCategory", "updatedAt");
  public static final QueryField DISPLAY_NAME = field("TextTemplateCategory", "displayName");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String") String name;
  private final @ModelField(targetType="String") String coverUrl;
  private final @ModelField(targetType="Int") Integer sort;
  private final @ModelField(targetType="Int") Integer online;
  private final @ModelField(targetType="AWSDateTime") Temporal.DateTime updatedAt;
  private final @ModelField(targetType="TextTemplate") @HasMany(associatedWith = "categoryID", type = TextTemplate.class) List<TextTemplate> textTemplates = null;
  private final @ModelField(targetType="TextTemplateCategoryLocale") @HasMany(associatedWith = "materialID", type = TextTemplateCategoryLocale.class) List<TextTemplateCategoryLocale> TextTemplateCategoryLocales = null;
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
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  public List<TextTemplate> getTextTemplates() {
      return textTemplates;
  }
  
  public List<TextTemplateCategoryLocale> getTextTemplateCategoryLocales() {
      return TextTemplateCategoryLocales;
  }
  
  public String getDisplayName() {
      return displayName;
  }
  
  private TextTemplateCategory(String id, String name, String coverUrl, Integer sort, Integer online, Temporal.DateTime updatedAt, String displayName) {
    this.id = id;
    this.name = name;
    this.coverUrl = coverUrl;
    this.sort = sort;
    this.online = online;
    this.updatedAt = updatedAt;
    this.displayName = displayName;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      TextTemplateCategory textTemplateCategory = (TextTemplateCategory) obj;
      return ObjectsCompat.equals(getId(), textTemplateCategory.getId()) &&
              ObjectsCompat.equals(getName(), textTemplateCategory.getName()) &&
              ObjectsCompat.equals(getCoverUrl(), textTemplateCategory.getCoverUrl()) &&
              ObjectsCompat.equals(getSort(), textTemplateCategory.getSort()) &&
              ObjectsCompat.equals(getOnline(), textTemplateCategory.getOnline()) &&
              ObjectsCompat.equals(getUpdatedAt(), textTemplateCategory.getUpdatedAt()) &&
              ObjectsCompat.equals(getDisplayName(), textTemplateCategory.getDisplayName());
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
      .append(getDisplayName())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("TextTemplateCategory {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("name=" + String.valueOf(getName()) + ", ")
      .append("coverUrl=" + String.valueOf(getCoverUrl()) + ", ")
      .append("sort=" + String.valueOf(getSort()) + ", ")
      .append("online=" + String.valueOf(getOnline()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()) + ", ")
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
  public static TextTemplateCategory justId(String id) {
    return new TextTemplateCategory(
      id,
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
      displayName);
  }
  public interface BuildStep {
    TextTemplateCategory build();
    BuildStep id(String id);
    BuildStep name(String name);
    BuildStep coverUrl(String coverUrl);
    BuildStep sort(Integer sort);
    BuildStep online(Integer online);
    BuildStep updatedAt(Temporal.DateTime updatedAt);
    BuildStep displayName(String displayName);
  }
  

  public static class Builder implements BuildStep {
    private String id;
    private String name;
    private String coverUrl;
    private Integer sort;
    private Integer online;
    private Temporal.DateTime updatedAt;
    private String displayName;
    @Override
     public TextTemplateCategory build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new TextTemplateCategory(
          id,
          name,
          coverUrl,
          sort,
          online,
          updatedAt,
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
     public BuildStep updatedAt(Temporal.DateTime updatedAt) {
        this.updatedAt = updatedAt;
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
    private CopyOfBuilder(String id, String name, String coverUrl, Integer sort, Integer online, Temporal.DateTime updatedAt, String displayName) {
      super.id(id);
      super.name(name)
        .coverUrl(coverUrl)
        .sort(sort)
        .online(online)
        .updatedAt(updatedAt)
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
     public CopyOfBuilder sort(Integer sort) {
      return (CopyOfBuilder) super.sort(sort);
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
     public CopyOfBuilder displayName(String displayName) {
      return (CopyOfBuilder) super.displayName(displayName);
    }
  }
  
}
