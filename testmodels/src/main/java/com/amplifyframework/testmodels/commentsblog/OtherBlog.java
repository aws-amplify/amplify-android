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

import com.amplifyframework.core.model.annotations.BelongsTo;
import com.amplifyframework.core.model.temporal.Temporal;

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

/** This is an auto generated class representing the OtherBlog type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "OtherBlogs", type = Model.Type.USER, version = 1)
public final class OtherBlog implements Model {
  public static final QueryField ID = field("OtherBlog", "id");
  public static final QueryField NAME = field("OtherBlog", "name");
  public static final QueryField OWNER = field("OtherBlog", "blogOwnerWithCustomPkBlogsId");
  public static final QueryField CREATED_AT = field("OtherBlog", "createdAt");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String", isRequired = true) String name;
  private final @ModelField(targetType="BlogOwnerWithCustomPK", isRequired = true) @BelongsTo(targetName = "otherBlogOwnerName", targetNames = {"otherBlogOwnerName", "otherBlogOwnerWea", }, type = BlogOwnerWithCustomPK.class) BlogOwnerWithCustomPK owner;
  private final @ModelField(targetType="AWSDateTime") Temporal.DateTime createdAt;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime updatedAt;
  public String resolveIdentifier() {
    return id;
  }
  
  public String getId() {
      return id;
  }
  
  public String getName() {
      return name;
  }
  
  public BlogOwnerWithCustomPK getOwner() {
      return owner;
  }
  
  public Temporal.DateTime getCreatedAt() {
      return createdAt;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  private OtherBlog(String id, String name, BlogOwnerWithCustomPK owner, Temporal.DateTime createdAt) {
    this.id = id;
    this.name = name;
    this.owner = owner;
    this.createdAt = createdAt;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      OtherBlog otherBlog = (OtherBlog) obj;
      return ObjectsCompat.equals(getId(), otherBlog.getId()) &&
              ObjectsCompat.equals(getName(), otherBlog.getName()) &&
              ObjectsCompat.equals(getOwner(), otherBlog.getOwner()) &&
              ObjectsCompat.equals(getCreatedAt(), otherBlog.getCreatedAt()) &&
              ObjectsCompat.equals(getUpdatedAt(), otherBlog.getUpdatedAt());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getName())
      .append(getOwner())
      .append(getCreatedAt())
      .append(getUpdatedAt())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("OtherBlog {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("name=" + String.valueOf(getName()) + ", ")
      .append("owner=" + String.valueOf(getOwner()) + ", ")
      .append("createdAt=" + String.valueOf(getCreatedAt()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()))
      .append("}")
      .toString();
  }
  
  public static NameStep builder() {
      return new Builder();
  }
  
  /** 
   * WARNING: This method should not be used to build an instance of this object for a CREATE mutation.
   * This is a convenience method to return an instance of the object with only its ID populated
   * to be used in the context of a parameter in a delete mutation or referencing a foreign key
   * in a relationship.
   * @param id the id of the existing item this instance will represent
   * @return an instance of this model with only ID populated
   */
  public static OtherBlog justId(String id) {
    return new OtherBlog(
      id,
      null,
      null,
      null
    );
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(id,
      name,
      owner,
      createdAt);
  }
  public interface NameStep {
    OwnerStep name(String name);
  }
  

  public interface OwnerStep {
    BuildStep owner(BlogOwnerWithCustomPK owner);
  }
  

  public interface BuildStep {
    OtherBlog build();
    BuildStep id(String id);
    BuildStep createdAt(Temporal.DateTime createdAt);
  }
  

  public static class Builder implements NameStep, OwnerStep, BuildStep {
    private String id;
    private String name;
    private BlogOwnerWithCustomPK owner;
    private Temporal.DateTime createdAt;
    @Override
     public OtherBlog build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new OtherBlog(
          id,
          name,
          owner,
          createdAt);
    }
    
    @Override
     public OwnerStep name(String name) {
        Objects.requireNonNull(name);
        this.name = name;
        return this;
    }
    
    @Override
     public BuildStep owner(BlogOwnerWithCustomPK owner) {
        Objects.requireNonNull(owner);
        this.owner = owner;
        return this;
    }
    
    @Override
     public BuildStep createdAt(Temporal.DateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }
    
    /** 
     * @param id id
     * @return Current Builder instance, for fluent method chaining
     */
    public BuildStep id(String id) {
        this.id = id;
        return this;
    }
  }
  

  public final class CopyOfBuilder extends Builder {
    private CopyOfBuilder(String id, String name, BlogOwnerWithCustomPK owner, Temporal.DateTime createdAt) {
      super.id(id);
      super.name(name)
        .owner(owner)
        .createdAt(createdAt);
    }
    
    @Override
     public CopyOfBuilder name(String name) {
      return (CopyOfBuilder) super.name(name);
    }
    
    @Override
     public CopyOfBuilder owner(BlogOwnerWithCustomPK owner) {
      return (CopyOfBuilder) super.owner(owner);
    }
    
    @Override
     public CopyOfBuilder createdAt(Temporal.DateTime createdAt) {
      return (CopyOfBuilder) super.createdAt(createdAt);
    }
  }
  
}
