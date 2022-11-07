/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.testmodels.commentsblog;

import com.amplifyframework.core.model.annotations.HasMany;
import com.amplifyframework.core.model.temporal.Temporal;
import com.amplifyframework.core.model.ModelIdentifier;

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

/** This is an auto generated class representing the BlogOwnerWithCustomPK type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "BlogOwnerWithCustomPKS", type = Model.Type.USER, version = 1)
@Index(name = "undefined", fields = {"name","wea"})
public final class BlogOwnerWithCustomPK implements Model {
  public static final QueryField ID = field("BlogOwnerWithCustomPK", "id");
  public static final QueryField NAME = field("BlogOwnerWithCustomPK", "name");
  public static final QueryField WEA = field("BlogOwnerWithCustomPK", "wea");
  public static final QueryField CREATED_AT = field("BlogOwnerWithCustomPK", "createdAt");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String", isRequired = true) String name;
  private final @ModelField(targetType="OtherBlog") @HasMany(associatedWith = "owner", type = OtherBlog.class) List<OtherBlog> blogs = null;
  private final @ModelField(targetType="String", isRequired = true) String wea;
  private final @ModelField(targetType="AWSDateTime") Temporal.DateTime createdAt;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime updatedAt;
  private BlogOwnerWithCustomPKIdentifier blogOwnerWithCustomPKIdentifier;
  public BlogOwnerWithCustomPKIdentifier resolveIdentifier() {
    if (blogOwnerWithCustomPKIdentifier == null) {
      this.blogOwnerWithCustomPKIdentifier = new BlogOwnerWithCustomPKIdentifier(name, wea);
    }
    return blogOwnerWithCustomPKIdentifier;
  }
  
  public String getId() {
      return id;
  }
  
  public String getName() {
      return name;
  }
  
  public List<OtherBlog> getBlogs() {
      return blogs;
  }
  
  public String getWea() {
      return wea;
  }
  
  public Temporal.DateTime getCreatedAt() {
      return createdAt;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  private BlogOwnerWithCustomPK(String id, String name, String wea, Temporal.DateTime createdAt) {
    this.id = id;
    this.name = name;
    this.wea = wea;
    this.createdAt = createdAt;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      BlogOwnerWithCustomPK blogOwnerWithCustomPk = (BlogOwnerWithCustomPK) obj;
      return ObjectsCompat.equals(getId(), blogOwnerWithCustomPk.getId()) &&
              ObjectsCompat.equals(getName(), blogOwnerWithCustomPk.getName()) &&
              ObjectsCompat.equals(getWea(), blogOwnerWithCustomPk.getWea()) &&
              ObjectsCompat.equals(getCreatedAt(), blogOwnerWithCustomPk.getCreatedAt()) &&
              ObjectsCompat.equals(getUpdatedAt(), blogOwnerWithCustomPk.getUpdatedAt());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getName())
      .append(getWea())
      .append(getCreatedAt())
      .append(getUpdatedAt())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("BlogOwnerWithCustomPK {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("name=" + String.valueOf(getName()) + ", ")
      .append("wea=" + String.valueOf(getWea()) + ", ")
      .append("createdAt=" + String.valueOf(getCreatedAt()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()))
      .append("}")
      .toString();
  }
  
  public static IdStep builder() {
      return new Builder();
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(id,
      name,
      wea,
      createdAt);
  }
  public interface IdStep {
    NameStep id(String id);
  }
  

  public interface NameStep {
    WeaStep name(String name);
  }
  

  public interface WeaStep {
    BuildStep wea(String wea);
  }
  

  public interface BuildStep {
    BlogOwnerWithCustomPK build();
    BuildStep createdAt(Temporal.DateTime createdAt);
  }
  

  public static class Builder implements IdStep, NameStep, WeaStep, BuildStep {
    private String id;
    private String name;
    private String wea;
    private Temporal.DateTime createdAt;
    @Override
     public BlogOwnerWithCustomPK build() {
        
        return new BlogOwnerWithCustomPK(
          id,
          name,
          wea,
          createdAt);
    }
    
    @Override
     public NameStep id(String id) {
        Objects.requireNonNull(id);
        this.id = id;
        return this;
    }
    
    @Override
     public WeaStep name(String name) {
        Objects.requireNonNull(name);
        this.name = name;
        return this;
    }
    
    @Override
     public BuildStep wea(String wea) {
        Objects.requireNonNull(wea);
        this.wea = wea;
        return this;
    }
    
    @Override
     public BuildStep createdAt(Temporal.DateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }
  }
  

  public final class CopyOfBuilder extends Builder {
    private CopyOfBuilder(String id, String name, String wea, Temporal.DateTime createdAt) {
      super.id(id)
        .name(name)
        .wea(wea)
        .createdAt(createdAt);
    }
    
    @Override
     public CopyOfBuilder id(String id) {
      return (CopyOfBuilder) super.id(id);
    }
    
    @Override
     public CopyOfBuilder name(String name) {
      return (CopyOfBuilder) super.name(name);
    }
    
    @Override
     public CopyOfBuilder wea(String wea) {
      return (CopyOfBuilder) super.wea(wea);
    }
    
    @Override
     public CopyOfBuilder createdAt(Temporal.DateTime createdAt) {
      return (CopyOfBuilder) super.createdAt(createdAt);
    }
  }
  

  public static class BlogOwnerWithCustomPKIdentifier extends ModelIdentifier<BlogOwnerWithCustomPK> {
    private static final long serialVersionUID = 1L;
    public BlogOwnerWithCustomPKIdentifier(String name, String wea) {
      super(name, wea);
    }
  }
  
}
