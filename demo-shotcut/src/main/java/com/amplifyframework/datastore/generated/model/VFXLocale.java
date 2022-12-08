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

/** This is an auto generated class representing the VFXLocale type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "VFXLocales", authRules = {
  @AuthRule(allow = AuthStrategy.PUBLIC, provider = "iam", operations = { ModelOperation.READ }),
  @AuthRule(allow = AuthStrategy.GROUPS, groupClaim = "cognito:groups", groups = { "Editor" }, provider = "userPools", operations = { ModelOperation.READ, ModelOperation.CREATE, ModelOperation.UPDATE, ModelOperation.DELETE }),
  @AuthRule(allow = AuthStrategy.PRIVATE, provider = "iam", operations = { ModelOperation.CREATE, ModelOperation.UPDATE, ModelOperation.DELETE, ModelOperation.READ })
})
@Index(name = "byVFX", fields = {"vfxID"})
public final class VFXLocale implements Model {
  public static final QueryField ID = field("VFXLocale", "id");
  public static final QueryField LOCALE = field("VFXLocale", "locale");
  public static final QueryField NAME = field("VFXLocale", "name");
  public static final QueryField SORT = field("VFXLocale", "sort");
  public static final QueryField VFX_ID = field("VFXLocale", "vfxID");
  public static final QueryField UPDATED_AT = field("VFXLocale", "updatedAt");
  public static final QueryField ONLINE = field("VFXLocale", "online");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String") String locale;
  private final @ModelField(targetType="String") String name;
  private final @ModelField(targetType="Int") Integer sort;
  private final @ModelField(targetType="ID", isRequired = true) String vfxID;
  private final @ModelField(targetType="AWSDateTime", isRequired = true) Temporal.DateTime updatedAt;
  private final @ModelField(targetType="Int") Integer online;
  public String getId() {
      return id;
  }
  
  public String getLocale() {
      return locale;
  }
  
  public String getName() {
      return name;
  }
  
  public Integer getSort() {
      return sort;
  }
  
  public String getVfxId() {
      return vfxID;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  public Integer getOnline() {
      return online;
  }
  
  private VFXLocale(String id, String locale, String name, Integer sort, String vfxID, Temporal.DateTime updatedAt, Integer online) {
    this.id = id;
    this.locale = locale;
    this.name = name;
    this.sort = sort;
    this.vfxID = vfxID;
    this.updatedAt = updatedAt;
    this.online = online;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      VFXLocale vfxLocale = (VFXLocale) obj;
      return ObjectsCompat.equals(getId(), vfxLocale.getId()) &&
              ObjectsCompat.equals(getLocale(), vfxLocale.getLocale()) &&
              ObjectsCompat.equals(getName(), vfxLocale.getName()) &&
              ObjectsCompat.equals(getSort(), vfxLocale.getSort()) &&
              ObjectsCompat.equals(getVfxId(), vfxLocale.getVfxId()) &&
              ObjectsCompat.equals(getUpdatedAt(), vfxLocale.getUpdatedAt()) &&
              ObjectsCompat.equals(getOnline(), vfxLocale.getOnline());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getLocale())
      .append(getName())
      .append(getSort())
      .append(getVfxId())
      .append(getUpdatedAt())
      .append(getOnline())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("VFXLocale {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("locale=" + String.valueOf(getLocale()) + ", ")
      .append("name=" + String.valueOf(getName()) + ", ")
      .append("sort=" + String.valueOf(getSort()) + ", ")
      .append("vfxID=" + String.valueOf(getVfxId()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()) + ", ")
      .append("online=" + String.valueOf(getOnline()))
      .append("}")
      .toString();
  }
  
  public static VfxIdStep builder() {
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
  public static VFXLocale justId(String id) {
    return new VFXLocale(
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
      name,
      sort,
      vfxID,
      updatedAt,
      online);
  }
  public interface VfxIdStep {
    UpdatedAtStep vfxId(String vfxId);
  }
  

  public interface UpdatedAtStep {
    BuildStep updatedAt(Temporal.DateTime updatedAt);
  }
  

  public interface BuildStep {
    VFXLocale build();
    BuildStep id(String id);
    BuildStep locale(String locale);
    BuildStep name(String name);
    BuildStep sort(Integer sort);
    BuildStep online(Integer online);
  }
  

  public static class Builder implements VfxIdStep, UpdatedAtStep, BuildStep {
    private String id;
    private String vfxID;
    private Temporal.DateTime updatedAt;
    private String locale;
    private String name;
    private Integer sort;
    private Integer online;
    @Override
     public VFXLocale build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new VFXLocale(
          id,
          locale,
          name,
          sort,
          vfxID,
          updatedAt,
          online);
    }
    
    @Override
     public UpdatedAtStep vfxId(String vfxId) {
        Objects.requireNonNull(vfxId);
        this.vfxID = vfxId;
        return this;
    }
    
    @Override
     public BuildStep updatedAt(Temporal.DateTime updatedAt) {
        Objects.requireNonNull(updatedAt);
        this.updatedAt = updatedAt;
        return this;
    }
    
    @Override
     public BuildStep locale(String locale) {
        this.locale = locale;
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
    private CopyOfBuilder(String id, String locale, String name, Integer sort, String vfxId, Temporal.DateTime updatedAt, Integer online) {
      super.id(id);
      super.vfxId(vfxId)
        .updatedAt(updatedAt)
        .locale(locale)
        .name(name)
        .sort(sort)
        .online(online);
    }
    
    @Override
     public CopyOfBuilder vfxId(String vfxId) {
      return (CopyOfBuilder) super.vfxId(vfxId);
    }
    
    @Override
     public CopyOfBuilder updatedAt(Temporal.DateTime updatedAt) {
      return (CopyOfBuilder) super.updatedAt(updatedAt);
    }
    
    @Override
     public CopyOfBuilder locale(String locale) {
      return (CopyOfBuilder) super.locale(locale);
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
  }
  
}
