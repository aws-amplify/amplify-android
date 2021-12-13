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

/** This is an auto generated class representing the Project10 type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "Project10s")
public final class Project10 implements Model {
  public static final QueryField ID = field("Project10", "id");
  public static final QueryField NAME = field("Project10", "name");
  public static final QueryField PROJECT10_TEAM_ID = field("Project10", "project10TeamId");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String") String name;
  private final @ModelField(targetType="Team10") @HasOne(associatedWith = "project", type = Team10.class) Team10 team = null;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime createdAt;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime updatedAt;
  private final @ModelField(targetType="ID") String project10TeamId;
  public String getId() {
      return id;
  }
  
  public String getName() {
      return name;
  }
  
  public Team10 getTeam() {
      return team;
  }
  
  public Temporal.DateTime getCreatedAt() {
      return createdAt;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  public String getProject10TeamId() {
      return project10TeamId;
  }
  
  private Project10(String id, String name, String project10TeamId) {
    this.id = id;
    this.name = name;
    this.project10TeamId = project10TeamId;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      Project10 project10 = (Project10) obj;
      return ObjectsCompat.equals(getId(), project10.getId()) &&
              ObjectsCompat.equals(getName(), project10.getName()) &&
              ObjectsCompat.equals(getCreatedAt(), project10.getCreatedAt()) &&
              ObjectsCompat.equals(getUpdatedAt(), project10.getUpdatedAt()) &&
              ObjectsCompat.equals(getProject10TeamId(), project10.getProject10TeamId());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getName())
      .append(getCreatedAt())
      .append(getUpdatedAt())
      .append(getProject10TeamId())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("Project10 {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("name=" + String.valueOf(getName()) + ", ")
      .append("createdAt=" + String.valueOf(getCreatedAt()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()) + ", ")
      .append("project10TeamId=" + String.valueOf(getProject10TeamId()))
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
  public static Project10 justId(String id) {
    return new Project10(
      id,
      null,
      null
    );
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(id,
      name,
      project10TeamId);
  }
  public interface BuildStep {
    Project10 build();
    BuildStep id(String id);
    BuildStep name(String name);
    BuildStep project10TeamId(String project10TeamId);
  }
  

  public static class Builder implements BuildStep {
    private String id;
    private String name;
    private String project10TeamId;
    @Override
     public Project10 build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new Project10(
          id,
          name,
          project10TeamId);
    }
    
    @Override
     public BuildStep name(String name) {
        this.name = name;
        return this;
    }
    
    @Override
     public BuildStep project10TeamId(String project10TeamId) {
        this.project10TeamId = project10TeamId;
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
    private CopyOfBuilder(String id, String name, String project10TeamId) {
      super.id(id);
      super.name(name)
        .project10TeamId(project10TeamId);
    }
    
    @Override
     public CopyOfBuilder name(String name) {
      return (CopyOfBuilder) super.name(name);
    }
    
    @Override
     public CopyOfBuilder project10TeamId(String project10TeamId) {
      return (CopyOfBuilder) super.project10TeamId(project10TeamId);
    }
  }
  
}
