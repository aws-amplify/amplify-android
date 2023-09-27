package com.amplifyframework.datastore.generated.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.core.model.ModelPath;
import com.amplifyframework.core.model.PropertyPath;

/** This is an auto generated class representing the ModelPath for the HasManyChild type in your schema. */
public final class HasManyChildPath extends ModelPath<HasManyChild> {
  private ParentPath parent;
  HasManyChildPath(@NonNull String name, @NonNull Boolean isCollection, @Nullable PropertyPath parent) {
    super(name, isCollection, parent, HasManyChild.class);
  }
  
  public synchronized ParentPath getParent() {
    if (parent == null) {
      parent = new ParentPath("parent", false, this);
    }
    return parent;
  }
}
