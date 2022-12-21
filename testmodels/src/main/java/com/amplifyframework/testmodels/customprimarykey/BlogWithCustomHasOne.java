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

package com.amplifyframework.testmodels.customprimarykey;

import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.annotations.HasOne;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.core.model.annotations.ModelField;
import com.amplifyframework.core.model.query.predicate.QueryField;
import com.amplifyframework.core.model.temporal.Temporal;

import java.util.UUID;

import static com.amplifyframework.core.model.query.predicate.QueryField.field;

/** This is an auto generated class representing the BlogWithCustomHasOne type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "BlogWithCustomHasOnes", type = Model.Type.USER, version = 1)
public final class BlogWithCustomHasOne implements Model {
  public static final QueryField ID = field("BlogWithCustomHasOne", "id");
  public static final QueryField TITLE = field("BlogWithCustomHasOne", "title");
  public static final QueryField OWNER_REF_ID = field("BlogWithCustomHasOne", "ownerRefId");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String") String title;
  private final @ModelField(targetType="ID") String ownerRefId;
  private final @ModelField(targetType="User") @HasOne(associatedWith = "id", type = User.class) User owner = null;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime createdAt;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime updatedAt;
  public String resolveIdentifier() {
    return id;
  }
  
  public String getId() {
      return id;
  }
  
  public String getTitle() {
      return title;
  }
  
  public String getOwnerRefId() {
      return ownerRefId;
  }
  
  public User getOwner() {
      return owner;
  }
  
  public Temporal.DateTime getCreatedAt() {
      return createdAt;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  private BlogWithCustomHasOne(String id, String title, String ownerRefId) {
    this.id = id;
    this.title = title;
    this.ownerRefId = ownerRefId;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      BlogWithCustomHasOne blogWithCustomHasOne = (BlogWithCustomHasOne) obj;
      return ObjectsCompat.equals(getId(), blogWithCustomHasOne.getId()) &&
              ObjectsCompat.equals(getTitle(), blogWithCustomHasOne.getTitle()) &&
              ObjectsCompat.equals(getOwnerRefId(), blogWithCustomHasOne.getOwnerRefId()) &&
              ObjectsCompat.equals(getCreatedAt(), blogWithCustomHasOne.getCreatedAt()) &&
              ObjectsCompat.equals(getUpdatedAt(), blogWithCustomHasOne.getUpdatedAt());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getTitle())
      .append(getOwnerRefId())
      .append(getCreatedAt())
      .append(getUpdatedAt())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("BlogWithCustomHasOne {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("title=" + String.valueOf(getTitle()) + ", ")
      .append("ownerRefId=" + String.valueOf(getOwnerRefId()) + ", ")
      .append("createdAt=" + String.valueOf(getCreatedAt()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()))
      .append("}")
      .toString();
  }
  
  public static BuildStep builder() {
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
  public static BlogWithCustomHasOne justId(String id) {
    return new BlogWithCustomHasOne(
      id,
      null,
      null
    );
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(id,
      title,
      ownerRefId);
  }
  public interface BuildStep {
    BlogWithCustomHasOne build();
    BuildStep id(String id);
    BuildStep title(String title);
    BuildStep ownerRefId(String ownerRefId);
  }
  

  public static class Builder implements BuildStep {
    private String id;
    private String title;
    private String ownerRefId;
    @Override
     public BlogWithCustomHasOne build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new BlogWithCustomHasOne(
          id,
          title,
          ownerRefId);
    }
    
    @Override
     public BuildStep title(String title) {
        this.title = title;
        return this;
    }
    
    @Override
     public BuildStep ownerRefId(String ownerRefId) {
        this.ownerRefId = ownerRefId;
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
    private CopyOfBuilder(String id, String title, String ownerRefId) {
      super.id(id);
      super.title(title)
        .ownerRefId(ownerRefId);
    }
    
    @Override
     public CopyOfBuilder title(String title) {
      return (CopyOfBuilder) super.title(title);
    }
    
    @Override
     public CopyOfBuilder ownerRefId(String ownerRefId) {
      return (CopyOfBuilder) super.ownerRefId(ownerRefId);
    }
  }
  
}
