package com.amplifyframework.datastore.generated.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.core.model.ModelPath;
import com.amplifyframework.core.model.PropertyPath;

/** This is an auto generated class representing the ModelPath for the Team type in your schema. */
public final class TeamPath extends ModelPath<Team> {
  private ProjectPath project;
  TeamPath(@NonNull String name, @NonNull Boolean isCollection, @Nullable PropertyPath parent) {
    super(name, isCollection, parent, Team.class);
  }
  
  public synchronized ProjectPath getProject() {
    if (project == null) {
      project = new ProjectPath("project", false, this);
    }
    return project;
  }
}
