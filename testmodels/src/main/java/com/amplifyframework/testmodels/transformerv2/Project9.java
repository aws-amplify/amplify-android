package com.amplifyframework.testmodels.transformerv2;

import com.amplifyframework.core.model.annotations.HasOne;
import com.amplifyframework.core.model.temporal.Temporal;

import java.util.List;
import java.util.UUID;
import java.util.Objects;

import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.annotations.Index;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.core.model.annotations.ModelField;
import com.amplifyframework.core.model.query.predicate.QueryField;

import static com.amplifyframework.core.model.query.predicate.QueryField.field;

/** This is an auto generated class representing the Project9 type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "Project9s")
public final class Project9 implements Model {
  public static final QueryField ID = field("Project9", "id");
  public static final QueryField NAME = field("Project9", "name");
  public static final QueryField PROJECT9_TEAM_ID = field("Project9", "project9TeamId");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String") String name;
  private final @ModelField(targetType="Team9") @HasOne(associatedWith = "project", type = Team9.class) Team9 team = null;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime createdAt;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime updatedAt;
  private final @ModelField(targetType="ID") String project9TeamId;
  public String getId() {
      return id;
  }
  
  public String getName() {
      return name;
  }
  
  public Team9 getTeam() {
      return team;
  }
  
  public Temporal.DateTime getCreatedAt() {
      return createdAt;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  public String getProject9TeamId() {
      return project9TeamId;
  }
  
  private Project9(String id, String name, String project9TeamId) {
    this.id = id;
    this.name = name;
    this.project9TeamId = project9TeamId;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      Project9 project9 = (Project9) obj;
      return ObjectsCompat.equals(getId(), project9.getId()) &&
              ObjectsCompat.equals(getName(), project9.getName()) &&
              ObjectsCompat.equals(getCreatedAt(), project9.getCreatedAt()) &&
              ObjectsCompat.equals(getUpdatedAt(), project9.getUpdatedAt()) &&
              ObjectsCompat.equals(getProject9TeamId(), project9.getProject9TeamId());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getName())
      .append(getCreatedAt())
      .append(getUpdatedAt())
      .append(getProject9TeamId())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("Project9 {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("name=" + String.valueOf(getName()) + ", ")
      .append("createdAt=" + String.valueOf(getCreatedAt()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()) + ", ")
      .append("project9TeamId=" + String.valueOf(getProject9TeamId()))
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
  public static Project9 justId(String id) {
    return new Project9(
      id,
      null,
      null
    );
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(id,
      name,
      project9TeamId);
  }
  public interface BuildStep {
    Project9 build();
    BuildStep id(String id);
    BuildStep name(String name);
    BuildStep project9TeamId(String project9TeamId);
  }
  

  public static class Builder implements BuildStep {
    private String id;
    private String name;
    private String project9TeamId;
    @Override
     public Project9 build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new Project9(
          id,
          name,
          project9TeamId);
    }
    
    @Override
     public BuildStep name(String name) {
        this.name = name;
        return this;
    }
    
    @Override
     public BuildStep project9TeamId(String project9TeamId) {
        this.project9TeamId = project9TeamId;
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
    private CopyOfBuilder(String id, String name, String project9TeamId) {
      super.id(id);
      super.name(name)
        .project9TeamId(project9TeamId);
    }
    
    @Override
     public CopyOfBuilder name(String name) {
      return (CopyOfBuilder) super.name(name);
    }
    
    @Override
     public CopyOfBuilder project9TeamId(String project9TeamId) {
      return (CopyOfBuilder) super.project9TeamId(project9TeamId);
    }
  }
  
}
