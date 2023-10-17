package com.amplifyframework.datastore.generated.model;

import static com.amplifyframework.core.model.query.predicate.QueryField.field;

import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelIdentifier;
import com.amplifyframework.core.model.ModelReference;
import com.amplifyframework.core.model.annotations.HasOne;
import com.amplifyframework.core.model.annotations.Index;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.core.model.annotations.ModelField;
import com.amplifyframework.core.model.query.predicate.QueryField;
import com.amplifyframework.core.model.temporal.Temporal;

import java.util.Objects;

/** This is an auto generated class representing the Project type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "Projects", type = Model.Type.USER, version = 1, hasLazySupport = true)
@Index(name = "undefined", fields = {"projectId","name"})
public final class Project implements Model {
  public static final ProjectPath rootPath = new ProjectPath("root", false, null);
  public static final QueryField PROJECT_ID = field("Project", "projectId");
  public static final QueryField NAME = field("Project", "name");
  public static final QueryField PROJECT_TEAM_TEAM_ID = field("Project", "projectTeamTeamId");
  public static final QueryField PROJECT_TEAM_NAME = field("Project", "projectTeamName");
  private final @ModelField(targetType="ID", isRequired = true) String projectId;
  private final @ModelField(targetType="String", isRequired = true) String name;
  private final @ModelField(targetType="Team") @HasOne(associatedWith = "project", targetNames = {"projectTeamTeamId", "projectTeamName"}, type = Team.class) ModelReference<Team> team = null;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime createdAt;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime updatedAt;
  private final @ModelField(targetType="ID") String projectTeamTeamId;
  private final @ModelField(targetType="String") String projectTeamName;
  private ProjectIdentifier projectIdentifier;
  /** @deprecated This API is internal to Amplify and should not be used. */
  @Deprecated
   public ProjectIdentifier resolveIdentifier() {
    if (projectIdentifier == null) {
      this.projectIdentifier = new ProjectIdentifier(projectId, name);
    }
    return projectIdentifier;
  }
  
  public String getProjectId() {
      return projectId;
  }
  
  public String getName() {
      return name;
  }
  
  public ModelReference<Team> getTeam() {
      return team;
  }
  
  public Temporal.DateTime getCreatedAt() {
      return createdAt;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  public String getProjectTeamTeamId() {
      return projectTeamTeamId;
  }
  
  public String getProjectTeamName() {
      return projectTeamName;
  }
  
  private Project(String projectId, String name, String projectTeamTeamId, String projectTeamName) {
    this.projectId = projectId;
    this.name = name;
    this.projectTeamTeamId = projectTeamTeamId;
    this.projectTeamName = projectTeamName;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      Project project = (Project) obj;
      return ObjectsCompat.equals(getProjectId(), project.getProjectId()) &&
              ObjectsCompat.equals(getName(), project.getName()) &&
              ObjectsCompat.equals(getCreatedAt(), project.getCreatedAt()) &&
              ObjectsCompat.equals(getUpdatedAt(), project.getUpdatedAt()) &&
              ObjectsCompat.equals(getProjectTeamTeamId(), project.getProjectTeamTeamId()) &&
              ObjectsCompat.equals(getProjectTeamName(), project.getProjectTeamName());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getProjectId())
      .append(getName())
      .append(getCreatedAt())
      .append(getUpdatedAt())
      .append(getProjectTeamTeamId())
      .append(getProjectTeamName())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("Project {")
      .append("projectId=" + String.valueOf(getProjectId()) + ", ")
      .append("name=" + String.valueOf(getName()) + ", ")
      .append("createdAt=" + String.valueOf(getCreatedAt()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()) + ", ")
      .append("projectTeamTeamId=" + String.valueOf(getProjectTeamTeamId()) + ", ")
      .append("projectTeamName=" + String.valueOf(getProjectTeamName()))
      .append("}")
      .toString();
  }
  
  public static ProjectIdStep builder() {
      return new Builder();
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(projectId,
      name,
      projectTeamTeamId,
      projectTeamName);
  }
  public interface ProjectIdStep {
    NameStep projectId(String projectId);
  }
  

  public interface NameStep {
    BuildStep name(String name);
  }
  

  public interface BuildStep {
    Project build();
    BuildStep projectTeamTeamId(String projectTeamTeamId);
    BuildStep projectTeamName(String projectTeamName);
  }
  

  public static class Builder implements ProjectIdStep, NameStep, BuildStep {
    private String projectId;
    private String name;
    private String projectTeamTeamId;
    private String projectTeamName;
    public Builder() {
      
    }
    
    private Builder(String projectId, String name, String projectTeamTeamId, String projectTeamName) {
      this.projectId = projectId;
      this.name = name;
      this.projectTeamTeamId = projectTeamTeamId;
      this.projectTeamName = projectTeamName;
    }
    
    @Override
     public Project build() {
        
        return new Project(
          projectId,
          name,
          projectTeamTeamId,
          projectTeamName);
    }
    
    @Override
     public NameStep projectId(String projectId) {
        Objects.requireNonNull(projectId);
        this.projectId = projectId;
        return this;
    }
    
    @Override
     public BuildStep name(String name) {
        Objects.requireNonNull(name);
        this.name = name;
        return this;
    }
    
    @Override
     public BuildStep projectTeamTeamId(String projectTeamTeamId) {
        this.projectTeamTeamId = projectTeamTeamId;
        return this;
    }
    
    @Override
     public BuildStep projectTeamName(String projectTeamName) {
        this.projectTeamName = projectTeamName;
        return this;
    }
  }
  

  public final class CopyOfBuilder extends Builder {
    private CopyOfBuilder(String projectId, String name, String projectTeamTeamId, String projectTeamName) {
      super(projectId, name, projectTeamTeamId, projectTeamName);
      Objects.requireNonNull(projectId);
      Objects.requireNonNull(name);
    }
    
    @Override
     public CopyOfBuilder projectId(String projectId) {
      return (CopyOfBuilder) super.projectId(projectId);
    }
    
    @Override
     public CopyOfBuilder name(String name) {
      return (CopyOfBuilder) super.name(name);
    }
    
    @Override
     public CopyOfBuilder projectTeamTeamId(String projectTeamTeamId) {
      return (CopyOfBuilder) super.projectTeamTeamId(projectTeamTeamId);
    }
    
    @Override
     public CopyOfBuilder projectTeamName(String projectTeamName) {
      return (CopyOfBuilder) super.projectTeamName(projectTeamName);
    }
  }
  

  public static class ProjectIdentifier extends ModelIdentifier<Project> {
    private static final long serialVersionUID = 1L;
    public ProjectIdentifier(String projectId, String name) {
      super(projectId, name);
    }
  }
  
}
