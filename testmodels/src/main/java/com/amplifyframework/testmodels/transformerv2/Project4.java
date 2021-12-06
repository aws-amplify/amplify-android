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

/** This is an auto generated class representing the Project4 type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "Project4s")
public final class Project4 implements Model {
  public static final QueryField ID = field("Project4", "id");
  public static final QueryField NAME = field("Project4", "name");
  public static final QueryField PROJECT4_TEAM_ID = field("Project4", "project4TeamId");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String") String name;
  private final @ModelField(targetType="Team4") @HasOne(associatedWith = "id", type = Team4.class) Team4 team = null;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime createdAt;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime updatedAt;
  private final @ModelField(targetType="ID") String project4TeamId;
  public String getId() {
      return id;
  }
  
  public String getName() {
      return name;
  }
  
  public Team4 getTeam() {
      return team;
  }
  
  public Temporal.DateTime getCreatedAt() {
      return createdAt;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  public String getProject4TeamId() {
      return project4TeamId;
  }
  
  private Project4(String id, String name, String project4TeamId) {
    this.id = id;
    this.name = name;
    this.project4TeamId = project4TeamId;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      Project4 project4 = (Project4) obj;
      return ObjectsCompat.equals(getId(), project4.getId()) &&
              ObjectsCompat.equals(getName(), project4.getName()) &&
              ObjectsCompat.equals(getCreatedAt(), project4.getCreatedAt()) &&
              ObjectsCompat.equals(getUpdatedAt(), project4.getUpdatedAt()) &&
              ObjectsCompat.equals(getProject4TeamId(), project4.getProject4TeamId());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getName())
      .append(getCreatedAt())
      .append(getUpdatedAt())
      .append(getProject4TeamId())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("Project4 {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("name=" + String.valueOf(getName()) + ", ")
      .append("createdAt=" + String.valueOf(getCreatedAt()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()) + ", ")
      .append("project4TeamId=" + String.valueOf(getProject4TeamId()))
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
  public static Project4 justId(String id) {
    return new Project4(
      id,
      null,
      null
    );
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(id,
      name,
      project4TeamId);
  }
  public interface BuildStep {
    Project4 build();
    BuildStep id(String id);
    BuildStep name(String name);
    BuildStep project4TeamId(String project4TeamId);
  }
  

  public static class Builder implements BuildStep {
    private String id;
    private String name;
    private String project4TeamId;
    @Override
     public Project4 build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new Project4(
          id,
          name,
          project4TeamId);
    }
    
    @Override
     public BuildStep name(String name) {
        this.name = name;
        return this;
    }
    
    @Override
     public BuildStep project4TeamId(String project4TeamId) {
        this.project4TeamId = project4TeamId;
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
    private CopyOfBuilder(String id, String name, String project4TeamId) {
      super.id(id);
      super.name(name)
        .project4TeamId(project4TeamId);
    }
    
    @Override
     public CopyOfBuilder name(String name) {
      return (CopyOfBuilder) super.name(name);
    }
    
    @Override
     public CopyOfBuilder project4TeamId(String project4TeamId) {
      return (CopyOfBuilder) super.project4TeamId(project4TeamId);
    }
  }
  
}
