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

/** This is an auto generated class representing the TransitionVFXLocale type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "TransitionVFXLocales", authRules = {
  @AuthRule(allow = AuthStrategy.PUBLIC, provider = "iam", operations = { ModelOperation.READ }),
  @AuthRule(allow = AuthStrategy.GROUPS, groupClaim = "cognito:groups", groups = { "Editor" }, provider = "userPools", operations = { ModelOperation.READ, ModelOperation.CREATE, ModelOperation.UPDATE, ModelOperation.DELETE }),
  @AuthRule(allow = AuthStrategy.PRIVATE, provider = "iam", operations = { ModelOperation.CREATE, ModelOperation.UPDATE, ModelOperation.DELETE, ModelOperation.READ })
})
@Index(name = "byTransitionVFX", fields = {"materialID"})
public final class TransitionVFXLocale implements Model {
  public static final QueryField ID = field("TransitionVFXLocale", "id");
  public static final QueryField LOCALE = field("TransitionVFXLocale", "locale");
  public static final QueryField MATERIAL_ID = field("TransitionVFXLocale", "materialID");
  public static final QueryField NAME = field("TransitionVFXLocale", "name");
  public static final QueryField SORT = field("TransitionVFXLocale", "sort");
  public static final QueryField ONLINE = field("TransitionVFXLocale", "online");
  public static final QueryField UPDATED_AT = field("TransitionVFXLocale", "updatedAt");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String") String locale;
  private final @ModelField(targetType="ID") String materialID;
  private final @ModelField(targetType="String") String name;
  private final @ModelField(targetType="Int") Integer sort;
  private final @ModelField(targetType="Int") Integer online;
  private final @ModelField(targetType="AWSDateTime") Temporal.DateTime updatedAt;
  public String getId() {
      return id;
  }
  
  public String getLocale() {
      return locale;
  }
  
  public String getMaterialId() {
      return materialID;
  }
  
  public String getName() {
      return name;
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
  
  private TransitionVFXLocale(String id, String locale, String materialID, String name, Integer sort, Integer online, Temporal.DateTime updatedAt) {
    this.id = id;
    this.locale = locale;
    this.materialID = materialID;
    this.name = name;
    this.sort = sort;
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
      TransitionVFXLocale transitionVfxLocale = (TransitionVFXLocale) obj;
      return ObjectsCompat.equals(getId(), transitionVfxLocale.getId()) &&
              ObjectsCompat.equals(getLocale(), transitionVfxLocale.getLocale()) &&
              ObjectsCompat.equals(getMaterialId(), transitionVfxLocale.getMaterialId()) &&
              ObjectsCompat.equals(getName(), transitionVfxLocale.getName()) &&
              ObjectsCompat.equals(getSort(), transitionVfxLocale.getSort()) &&
              ObjectsCompat.equals(getOnline(), transitionVfxLocale.getOnline()) &&
              ObjectsCompat.equals(getUpdatedAt(), transitionVfxLocale.getUpdatedAt());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getLocale())
      .append(getMaterialId())
      .append(getName())
      .append(getSort())
      .append(getOnline())
      .append(getUpdatedAt())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("TransitionVFXLocale {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("locale=" + String.valueOf(getLocale()) + ", ")
      .append("materialID=" + String.valueOf(getMaterialId()) + ", ")
      .append("name=" + String.valueOf(getName()) + ", ")
      .append("sort=" + String.valueOf(getSort()) + ", ")
      .append("online=" + String.valueOf(getOnline()) + ", ")
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
  public static TransitionVFXLocale justId(String id) {
    return new TransitionVFXLocale(
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
      locale,
      materialID,
      name,
      sort,
      online,
      updatedAt);
  }
  public interface BuildStep {
    TransitionVFXLocale build();
    BuildStep id(String id);
    BuildStep locale(String locale);
    BuildStep materialId(String materialId);
    BuildStep name(String name);
    BuildStep sort(Integer sort);
    BuildStep online(Integer online);
    BuildStep updatedAt(Temporal.DateTime updatedAt);
  }
  

  public static class Builder implements BuildStep {
    private String id;
    private String locale;
    private String materialID;
    private String name;
    private Integer sort;
    private Integer online;
    private Temporal.DateTime updatedAt;
    @Override
     public TransitionVFXLocale build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new TransitionVFXLocale(
          id,
          locale,
          materialID,
          name,
          sort,
          online,
          updatedAt);
    }
    
    @Override
     public BuildStep locale(String locale) {
        this.locale = locale;
        return this;
    }
    
    @Override
     public BuildStep materialId(String materialId) {
        this.materialID = materialId;
        return this;
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
     public BuildStep online(Integer online) {
        this.online = online;
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
    private CopyOfBuilder(String id, String locale, String materialId, String name, Integer sort, Integer online, Temporal.DateTime updatedAt) {
      super.id(id);
      super.locale(locale)
        .materialId(materialId)
        .name(name)
        .sort(sort)
        .online(online)
        .updatedAt(updatedAt);
    }
    
    @Override
     public CopyOfBuilder locale(String locale) {
      return (CopyOfBuilder) super.locale(locale);
    }
    
    @Override
     public CopyOfBuilder materialId(String materialId) {
      return (CopyOfBuilder) super.materialId(materialId);
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
     public CopyOfBuilder online(Integer online) {
      return (CopyOfBuilder) super.online(online);
    }
    
    @Override
     public CopyOfBuilder updatedAt(Temporal.DateTime updatedAt) {
      return (CopyOfBuilder) super.updatedAt(updatedAt);
    }
  }
  
}
