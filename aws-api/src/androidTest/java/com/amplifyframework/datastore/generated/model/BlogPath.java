package com.amplifyframework.datastore.generated.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.core.model.ModelPath;
import com.amplifyframework.core.model.PropertyPath;

/** This is an auto generated class representing the ModelPath for the Blog type in your schema. */
public final class BlogPath extends ModelPath<Blog> {
  private PostPath posts;
  BlogPath(@NonNull String name, @NonNull Boolean isCollection, @Nullable PropertyPath parent) {
    super(name, isCollection, parent, Blog.class);
  }
  
  public synchronized PostPath getPosts() {
    if (posts == null) {
      posts = new PostPath("posts", true, this);
    }
    return posts;
  }
}
