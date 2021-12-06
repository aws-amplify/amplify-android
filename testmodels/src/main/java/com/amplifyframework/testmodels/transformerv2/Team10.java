package com.amplifyframework.testmodels.transformerv2;

import com.amplifyframework.core.model.annotations.BelongsTo;
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

/** This is an auto generated class representing the Team10 type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "Team10s")
public final class Team10 implements Model {
  public static final QueryField ID = field("Team10", "id");
  public static final QueryField NAME = field("Team10", "name");
  public static final QueryField PROJECT = field("Team10", "projectID");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String", isRequired = true) String name;
  private final @ModelField(targetType="Project10") @BelongsTo(targetName = "projectID", type = Project10.class) Project10 project;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime createdAt;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime updatedAt;
  public String getId() {
      return id;
  }
  
  public String getName() {
      return name;
  }
  
  public Project10 getProject() {
      return project;
  }
  
  public Temporal.DateTime getCreatedAt() {
      return createdAt;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  private Team10(String id, String name, Project10 project) {
    this.id = id;
    this.name = name;
    this.project = project;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      Team10 team10 = (Team10) obj;
      return ObjectsCompat.equals(getId(), team10.getId()) &&
              ObjectsCompat.equals(getName(), team10.getName()) &&
              ObjectsCompat.equals(getProject(), team10.getProject()) &&
              ObjectsCompat.equals(getCreatedAt(), team10.getCreatedAt()) &&
              ObjectsCompat.equals(getUpdatedAt(), team10.getUpdatedAt());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getName())
      .append(getProject())
      .append(getCreatedAt())
      .append(getUpdatedAt())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("Team10 {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("name=" + String.valueOf(getName()) + ", ")
      .append("project=" + String.valueOf(getProject()) + ", ")
      .append("createdAt=" + String.valueOf(getCreatedAt()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()))
      .append("}")
      .toString();
  }
  
  public static NameStep builder() {
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
  public static Team10 justId(String id) {
    return new Team10(
      id,
      null,
      null
    );
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(id,
      name,
      project);
  }
  public interface NameStep {
    BuildStep name(String name);
  }
  

  public interface BuildStep {
    Team10 build();
    BuildStep id(String id);
    BuildStep project(Project10 project);
  }
  

  public static class Builder implements NameStep, BuildStep {
    private String id;
    private String name;
    private Project10 project;
    @Override
     public Team10 build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new Team10(
          id,
          name,
          project);
    }
    
    @Override
     public BuildStep name(String name) {
        Objects.requireNonNull(name);
        this.name = name;
        return this;
    }
    
    @Override
     public BuildStep project(Project10 project) {
        this.project = project;
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
    private CopyOfBuilder(String id, String name, Project10 project) {
      super.id(id);
      super.name(name)
        .project(project);
    }
    
    @Override
     public CopyOfBuilder name(String name) {
      return (CopyOfBuilder) super.name(name);
    }
    
    @Override
     public CopyOfBuilder project(Project10 project) {
      return (CopyOfBuilder) super.project(project);
    }
  }
  
}
