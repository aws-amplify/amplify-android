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

/** This is an auto generated class representing the Blog2 type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "Blog2s", type = Model.Type.USER, version = 1)
public final class Blog2 implements Model {
  public static final QueryField ID = field("Blog2", "id");
  public static final QueryField NAME = field("Blog2", "name");
  public static final QueryField OWNER = field("Blog2", "blogOwner2BlogsId");
  public static final QueryField CREATED_AT = field("Blog2", "createdAt");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String", isRequired = true) String name;
  private final @ModelField(targetType="BlogOwner2", isRequired = true) @BelongsTo(targetName = "otherBlogOwner2Name", targetNames = {"otherBlogOwner2Name"}, type = BlogOwner2.class) BlogOwner2 owner;
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
  
  public BlogOwner2 getOwner() {
      return owner;
  }
  
  public Temporal.DateTime getCreatedAt() {
      return createdAt;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  private Blog2(String id, String name, BlogOwner2 owner, Temporal.DateTime createdAt) {
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
      Blog2 blog2 = (Blog2) obj;
      return ObjectsCompat.equals(getId(), blog2.getId()) &&
              ObjectsCompat.equals(getName(), blog2.getName()) &&
              ObjectsCompat.equals(getOwner(), blog2.getOwner()) &&
              ObjectsCompat.equals(getCreatedAt(), blog2.getCreatedAt()) &&
              ObjectsCompat.equals(getUpdatedAt(), blog2.getUpdatedAt());
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
      .append("Blog2 {")
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
  public static Blog2 justId(String id) {
    return new Blog2(
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
    BuildStep owner(BlogOwner2 owner);
  }
  

  public interface BuildStep {
    Blog2 build();
    BuildStep id(String id);
    BuildStep createdAt(Temporal.DateTime createdAt);
  }
  

  public static class Builder implements NameStep, OwnerStep, BuildStep {
    private String id;
    private String name;
    private BlogOwner2 owner;
    private Temporal.DateTime createdAt;
    @Override
     public Blog2 build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new Blog2(
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
     public BuildStep owner(BlogOwner2 owner) {
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
    private CopyOfBuilder(String id, String name, BlogOwner2 owner, Temporal.DateTime createdAt) {
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
     public CopyOfBuilder owner(BlogOwner2 owner) {
      return (CopyOfBuilder) super.owner(owner);
    }
    
    @Override
     public CopyOfBuilder createdAt(Temporal.DateTime createdAt) {
      return (CopyOfBuilder) super.createdAt(createdAt);
    }
  }
  
}
