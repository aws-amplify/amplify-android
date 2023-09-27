package com.amplifyframework.datastore.generated.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.core.model.ModelPath;
import com.amplifyframework.core.model.PropertyPath;

/** This is an auto generated class representing the ModelPath for the Project type in your schema. */
public final class ProjectPath extends ModelPath<Project> {
  private TeamPath team;
  ProjectPath(@NonNull String name, @NonNull Boolean isCollection, @Nullable PropertyPath parent) {
    super(name, isCollection, parent, Project.class);
  }
  
  public synchronized TeamPath getTeam() {
    if (team == null) {
      team = new TeamPath("team", false, this);
    }
    return team;
  }
}
