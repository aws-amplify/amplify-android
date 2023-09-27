package com.amplifyframework.datastore.generated.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.core.model.ModelPath;
import com.amplifyframework.core.model.PropertyPath;

/** This is an auto generated class representing the ModelPath for the Parent type in your schema. */
public final class ParentPath extends ModelPath<Parent> {
  private HasOneChildPath child;
  private HasManyChildPath children;
  ParentPath(@NonNull String name, @NonNull Boolean isCollection, @Nullable PropertyPath parent) {
    super(name, isCollection, parent, Parent.class);
  }
  
  public synchronized HasOneChildPath getChild() {
    if (child == null) {
      child = new HasOneChildPath("child", false, this);
    }
    return child;
  }
  
  public synchronized HasManyChildPath getChildren() {
    if (children == null) {
      children = new HasManyChildPath("children", true, this);
    }
    return children;
  }
}
