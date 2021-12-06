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

/** This is an auto generated class representing the Project5 type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "Project5s")
public final class Project5 implements Model {
  public static final QueryField ID = field("Project5", "id");
  public static final QueryField NAME = field("Project5", "name");
  public static final QueryField TEAM_ID = field("Project5", "teamID");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String") String name;
  private final @ModelField(targetType="ID") String teamID;
  private final @ModelField(targetType="Team5") @HasOne(associatedWith = "id", type = Team5.class) Team5 team = null;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime createdAt;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime updatedAt;
  public String getId() {
      return id;
  }
  
  public String getName() {
      return name;
  }
  
  public String getTeamId() {
      return teamID;
  }
  
  public Team5 getTeam() {
      return team;
  }
  
  public Temporal.DateTime getCreatedAt() {
      return createdAt;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  private Project5(String id, String name, String teamID) {
    this.id = id;
    this.name = name;
    this.teamID = teamID;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      Project5 project5 = (Project5) obj;
      return ObjectsCompat.equals(getId(), project5.getId()) &&
              ObjectsCompat.equals(getName(), project5.getName()) &&
              ObjectsCompat.equals(getTeamId(), project5.getTeamId()) &&
              ObjectsCompat.equals(getCreatedAt(), project5.getCreatedAt()) &&
              ObjectsCompat.equals(getUpdatedAt(), project5.getUpdatedAt());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getName())
      .append(getTeamId())
      .append(getCreatedAt())
      .append(getUpdatedAt())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("Project5 {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("name=" + String.valueOf(getName()) + ", ")
      .append("teamID=" + String.valueOf(getTeamId()) + ", ")
      .append("createdAt=" + String.valueOf(getCreatedAt()) + ", ")
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
  public static Project5 justId(String id) {
    return new Project5(
      id,
      null,
      null
    );
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(id,
      name,
      teamID);
  }
  public interface BuildStep {
    Project5 build();
    BuildStep id(String id);
    BuildStep name(String name);
    BuildStep teamId(String teamId);
  }
  

  public static class Builder implements BuildStep {
    private String id;
    private String name;
    private String teamID;
    @Override
     public Project5 build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new Project5(
          id,
          name,
          teamID);
    }
    
    @Override
     public BuildStep name(String name) {
        this.name = name;
        return this;
    }
    
    @Override
     public BuildStep teamId(String teamId) {
        this.teamID = teamId;
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
    private CopyOfBuilder(String id, String name, String teamId) {
      super.id(id);
      super.name(name)
        .teamId(teamId);
    }
    
    @Override
     public CopyOfBuilder name(String name) {
      return (CopyOfBuilder) super.name(name);
    }
    
    @Override
     public CopyOfBuilder teamId(String teamId) {
      return (CopyOfBuilder) super.teamId(teamId);
    }
  }
  
}
