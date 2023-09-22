package com.amplifyframework.testmodels.lazy;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.core.model.ModelPath;
import com.amplifyframework.core.model.PropertyPath;

/** This is an auto generated class representing the ModelPath for the Post type in your schema. */
public final class PostPath extends ModelPath<Post> {
  private BlogPath blog;
  private CommentPath comments;
  PostPath(@NonNull String name, @NonNull Boolean isCollection, @Nullable PropertyPath parent) {
    super(name, isCollection, parent, Post.class);
  }
  
  public synchronized BlogPath getBlog() {
    if (blog == null) {
      blog = new BlogPath("blog", false, this);
    }
    return blog;
  }
  
  public synchronized CommentPath getComments() {
    if (comments == null) {
      comments = new CommentPath("comments", true, this);
    }
    return comments;
  }
}
