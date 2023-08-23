/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.testmodels.cpk;

import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelIdentifier;
import com.amplifyframework.core.model.annotations.HasMany;
import com.amplifyframework.core.model.annotations.HasOne;
import com.amplifyframework.core.model.annotations.Index;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.core.model.annotations.ModelField;
import com.amplifyframework.core.model.query.predicate.QueryField;
import com.amplifyframework.core.model.temporal.Temporal;

import java.util.List;
import java.util.Objects;

import static com.amplifyframework.core.model.query.predicate.QueryField.field;

/** This is an auto generated class representing the Blog type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "Blogs", type = Model.Type.USER, version = 1)
@Index(name = "undefined", fields = {"blogId","siteId"})
public final class Blog implements Model {
  public static final QueryField BLOG_ID = field("Blog", "blogId");
  public static final QueryField SITE_ID = field("Blog", "siteId");
  public static final QueryField NAME = field("Blog", "name");
  public static final QueryField BLOG_AUTHOR_ID = field("Blog", "blogAuthorId");
  private final @ModelField(targetType="String", isRequired = true) String blogId;
  private final @ModelField(targetType="ID", isRequired = true) String siteId;
  private final @ModelField(targetType="String", isRequired = true) String name;
  private final @ModelField(targetType="User", isRequired = true) @HasOne(associatedWith = "id", type = User.class) User author = null;
  private final @ModelField(targetType="Post", isRequired = true) @HasMany(associatedWith = "blog", type = Post.class) List<Post> posts = null;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime createdAt;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime updatedAt;
  private final @ModelField(targetType="ID", isRequired = true) String blogAuthorId;
  private BlogIdentifier blogIdentifier;
  /** @deprecated This API is internal to Amplify and should not be used. */
  @Deprecated
   public BlogIdentifier resolveIdentifier() {
    if (blogIdentifier == null) {
      this.blogIdentifier = new BlogIdentifier(blogId, siteId);
    }
    return blogIdentifier;
  }
  
  public String getBlogId() {
      return blogId;
  }
  
  public String getSiteId() {
      return siteId;
  }
  
  public String getName() {
      return name;
  }
  
  public User getAuthor() {
      return author;
  }
  
  public List<Post> getPosts() {
      return posts;
  }
  
  public Temporal.DateTime getCreatedAt() {
      return createdAt;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  public String getBlogAuthorId() {
      return blogAuthorId;
  }
  
  private Blog(String blogId, String siteId, String name, String blogAuthorId) {
    this.blogId = blogId;
    this.siteId = siteId;
    this.name = name;
    this.blogAuthorId = blogAuthorId;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      Blog blog = (Blog) obj;
      return ObjectsCompat.equals(getBlogId(), blog.getBlogId()) &&
              ObjectsCompat.equals(getSiteId(), blog.getSiteId()) &&
              ObjectsCompat.equals(getName(), blog.getName()) &&
              ObjectsCompat.equals(getCreatedAt(), blog.getCreatedAt()) &&
              ObjectsCompat.equals(getUpdatedAt(), blog.getUpdatedAt()) &&
              ObjectsCompat.equals(getBlogAuthorId(), blog.getBlogAuthorId());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getBlogId())
      .append(getSiteId())
      .append(getName())
      .append(getCreatedAt())
      .append(getUpdatedAt())
      .append(getBlogAuthorId())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("Blog {")
      .append("blogId=" + String.valueOf(getBlogId()) + ", ")
      .append("siteId=" + String.valueOf(getSiteId()) + ", ")
      .append("name=" + String.valueOf(getName()) + ", ")
      .append("createdAt=" + String.valueOf(getCreatedAt()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()) + ", ")
      .append("blogAuthorId=" + String.valueOf(getBlogAuthorId()))
      .append("}")
      .toString();
  }
  
  public static BlogIdStep builder() {
      return new Builder();
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(blogId,
      siteId,
      name,
      blogAuthorId);
  }
  public interface BlogIdStep {
    SiteIdStep blogId(String blogId);
  }
  

  public interface SiteIdStep {
    NameStep siteId(String siteId);
  }
  

  public interface NameStep {
    BlogAuthorIdStep name(String name);
  }
  

  public interface BlogAuthorIdStep {
    BuildStep blogAuthorId(String blogAuthorId);
  }
  

  public interface BuildStep {
    Blog build();
  }
  

  public static class Builder implements BlogIdStep, SiteIdStep, NameStep, BlogAuthorIdStep, BuildStep {
    private String blogId;
    private String siteId;
    private String name;
    private String blogAuthorId;
    @Override
     public Blog build() {
        
        return new Blog(
          blogId,
          siteId,
          name,
          blogAuthorId);
    }
    
    @Override
     public SiteIdStep blogId(String blogId) {
        Objects.requireNonNull(blogId);
        this.blogId = blogId;
        return this;
    }
    
    @Override
     public NameStep siteId(String siteId) {
        Objects.requireNonNull(siteId);
        this.siteId = siteId;
        return this;
    }
    
    @Override
     public BlogAuthorIdStep name(String name) {
        Objects.requireNonNull(name);
        this.name = name;
        return this;
    }
    
    @Override
     public BuildStep blogAuthorId(String blogAuthorId) {
        Objects.requireNonNull(blogAuthorId);
        this.blogAuthorId = blogAuthorId;
        return this;
    }
  }
  

  public final class CopyOfBuilder extends Builder {
    private CopyOfBuilder(String blogId, String siteId, String name, String blogAuthorId) {
      super.blogId(blogId)
        .siteId(siteId)
        .name(name)
        .blogAuthorId(blogAuthorId);
    }
    
    @Override
     public CopyOfBuilder blogId(String blogId) {
      return (CopyOfBuilder) super.blogId(blogId);
    }
    
    @Override
     public CopyOfBuilder siteId(String siteId) {
      return (CopyOfBuilder) super.siteId(siteId);
    }
    
    @Override
     public CopyOfBuilder name(String name) {
      return (CopyOfBuilder) super.name(name);
    }
    
    @Override
     public CopyOfBuilder blogAuthorId(String blogAuthorId) {
      return (CopyOfBuilder) super.blogAuthorId(blogAuthorId);
    }
  }
  

  public static class BlogIdentifier extends ModelIdentifier<Blog> {
    private static final long serialVersionUID = 1L;
    public BlogIdentifier(String blogId, String siteId) {
      super(blogId, siteId);
    }
  }
  
}
