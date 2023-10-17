package com.amplifyframework.testmodels.lazy;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.core.model.ModelPath;
import com.amplifyframework.core.model.PropertyPath;

/** This is an auto generated class representing the ModelPath for the Comment type in your schema. */
public final class CommentPath extends ModelPath<Comment> {
  private PostPath post;
  CommentPath(@NonNull String name, @NonNull Boolean isCollection, @Nullable PropertyPath parent) {
    super(name, isCollection, parent, Comment.class);
  }
  
  public synchronized PostPath getPost() {
    if (post == null) {
      post = new PostPath("post", false, this);
    }
    return post;
  }
}
