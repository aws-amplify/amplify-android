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

/** This is an auto generated class representing the RecommendCategory type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "RecommendCategories", authRules = {
  @AuthRule(allow = AuthStrategy.PUBLIC, provider = "iam", operations = { ModelOperation.READ }),
  @AuthRule(allow = AuthStrategy.PRIVATE, provider = "iam", operations = { ModelOperation.CREATE, ModelOperation.UPDATE, ModelOperation.DELETE, ModelOperation.READ })
})
public final class RecommendCategory implements Model {
  public static final QueryField ID = field("RecommendCategory", "id");
  public static final QueryField NAME = field("RecommendCategory", "name");
  public static final QueryField SORT = field("RecommendCategory", "sort");
  public static final QueryField UPDATED_AT = field("RecommendCategory", "updatedAt");
  public static final QueryField COVER_URL = field("RecommendCategory", "coverUrl");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String") String name;
  private final @ModelField(targetType="Int") Integer sort;
  private final @ModelField(targetType="AWSDateTime") Temporal.DateTime updatedAt;
  private final @ModelField(targetType="Recommend") @HasMany(associatedWith = "recommendCategoryID", type = Recommend.class) List<Recommend> Recommends = null;
  private final @ModelField(targetType="String") String coverUrl;
  public String getId() {
      return id;
  }
  
  public String getName() {
      return name;
  }
  
  public Integer getSort() {
      return sort;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  public List<Recommend> getRecommends() {
      return Recommends;
  }
  
  public String getCoverUrl() {
      return coverUrl;
  }
  
  private RecommendCategory(String id, String name, Integer sort, Temporal.DateTime updatedAt, String coverUrl) {
    this.id = id;
    this.name = name;
    this.sort = sort;
    this.updatedAt = updatedAt;
    this.coverUrl = coverUrl;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      RecommendCategory recommendCategory = (RecommendCategory) obj;
      return ObjectsCompat.equals(getId(), recommendCategory.getId()) &&
              ObjectsCompat.equals(getName(), recommendCategory.getName()) &&
              ObjectsCompat.equals(getSort(), recommendCategory.getSort()) &&
              ObjectsCompat.equals(getUpdatedAt(), recommendCategory.getUpdatedAt()) &&
              ObjectsCompat.equals(getCoverUrl(), recommendCategory.getCoverUrl());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getName())
      .append(getSort())
      .append(getUpdatedAt())
      .append(getCoverUrl())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("RecommendCategory {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("name=" + String.valueOf(getName()) + ", ")
      .append("sort=" + String.valueOf(getSort()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()) + ", ")
      .append("coverUrl=" + String.valueOf(getCoverUrl()))
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
  public static RecommendCategory justId(String id) {
    return new RecommendCategory(
      id,
      null,
      null,
      null,
      null
    );
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(id,
      name,
      sort,
      updatedAt,
      coverUrl);
  }
  public interface BuildStep {
    RecommendCategory build();
    BuildStep id(String id);
    BuildStep name(String name);
    BuildStep sort(Integer sort);
    BuildStep updatedAt(Temporal.DateTime updatedAt);
    BuildStep coverUrl(String coverUrl);
  }
  

  public static class Builder implements BuildStep {
    private String id;
    private String name;
    private Integer sort;
    private Temporal.DateTime updatedAt;
    private String coverUrl;
    @Override
     public RecommendCategory build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new RecommendCategory(
          id,
          name,
          sort,
          updatedAt,
          coverUrl);
    }
    
    @Override
     public BuildStep name(String name) {
        this.name = name;
        return this;
    }
    
    @Override
     public BuildStep sort(Integer sort) {
        this.sort = sort;
        return this;
    }
    
    @Override
     public BuildStep updatedAt(Temporal.DateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }
    
    @Override
     public BuildStep coverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
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
    private CopyOfBuilder(String id, String name, Integer sort, Temporal.DateTime updatedAt, String coverUrl) {
      super.id(id);
      super.name(name)
        .sort(sort)
        .updatedAt(updatedAt)
        .coverUrl(coverUrl);
    }
    
    @Override
     public CopyOfBuilder name(String name) {
      return (CopyOfBuilder) super.name(name);
    }
    
    @Override
     public CopyOfBuilder sort(Integer sort) {
      return (CopyOfBuilder) super.sort(sort);
    }
    
    @Override
     public CopyOfBuilder updatedAt(Temporal.DateTime updatedAt) {
      return (CopyOfBuilder) super.updatedAt(updatedAt);
    }
    
    @Override
     public CopyOfBuilder coverUrl(String coverUrl) {
      return (CopyOfBuilder) super.coverUrl(coverUrl);
    }
  }
  
}
