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
import com.amplifyframework.core.model.annotations.Index;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.core.model.annotations.ModelField;
import com.amplifyframework.core.model.query.predicate.QueryField;
import com.amplifyframework.core.model.temporal.Temporal;

import java.util.Objects;
import java.util.UUID;

import static com.amplifyframework.core.model.query.predicate.QueryField.field;

/** This is an auto generated class representing the ModelCompositeMultiplePk type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "ModelCompositeMultiplePks", type = Model.Type.USER, version = 1)
@Index(name = "undefined", fields = {"id","location","name"})
public final class ModelCompositeMultiplePk implements Model {
  public static final QueryField ID = field("ModelCompositeMultiplePk", "id");
  public static final QueryField LOCATION = field("ModelCompositeMultiplePk", "location");
  public static final QueryField NAME = field("ModelCompositeMultiplePk", "name");
  public static final QueryField LAST_NAME = field("ModelCompositeMultiplePk", "lastName");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String", isRequired = true) String location;
  private final @ModelField(targetType="String", isRequired = true) String name;
  private final @ModelField(targetType="String") String lastName;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime createdAt;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime updatedAt;
  public String resolveIdentifier() {
    return id;
  }
  
  public String getId() {
      return id;
  }
  
  public String getLocation() {
      return location;
  }
  
  public String getName() {
      return name;
  }
  
  public String getLastName() {
      return lastName;
  }
  
  public Temporal.DateTime getCreatedAt() {
      return createdAt;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  private ModelCompositeMultiplePk(String id, String location, String name, String lastName) {
    this.id = id;
    this.location = location;
    this.name = name;
    this.lastName = lastName;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      ModelCompositeMultiplePk modelCompositeMultiplePk = (ModelCompositeMultiplePk) obj;
      return ObjectsCompat.equals(getId(), modelCompositeMultiplePk.getId()) &&
              ObjectsCompat.equals(getLocation(), modelCompositeMultiplePk.getLocation()) &&
              ObjectsCompat.equals(getName(), modelCompositeMultiplePk.getName()) &&
              ObjectsCompat.equals(getLastName(), modelCompositeMultiplePk.getLastName()) &&
              ObjectsCompat.equals(getCreatedAt(), modelCompositeMultiplePk.getCreatedAt()) &&
              ObjectsCompat.equals(getUpdatedAt(), modelCompositeMultiplePk.getUpdatedAt());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getLocation())
      .append(getName())
      .append(getLastName())
      .append(getCreatedAt())
      .append(getUpdatedAt())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("ModelCompositeMultiplePk {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("location=" + String.valueOf(getLocation()) + ", ")
      .append("name=" + String.valueOf(getName()) + ", ")
      .append("lastName=" + String.valueOf(getLastName()) + ", ")
      .append("createdAt=" + String.valueOf(getCreatedAt()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()))
      .append("}")
      .toString();
  }
  
  public static LocationStep builder() {
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
  public static ModelCompositeMultiplePk justId(String id) {
    return new ModelCompositeMultiplePk(
      id,
      null,
      null,
      null
    );
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(id,
      location,
      name,
      lastName);
  }
  public interface LocationStep {
    NameStep location(String location);
  }
  

  public interface NameStep {
    BuildStep name(String name);
  }
  

  public interface BuildStep {
    ModelCompositeMultiplePk build();
    BuildStep id(String id);
    BuildStep lastName(String lastName);
  }
  

  public static class Builder implements LocationStep, NameStep, BuildStep {
    private String id;
    private String location;
    private String name;
    private String lastName;
    @Override
     public ModelCompositeMultiplePk build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new ModelCompositeMultiplePk(
          id,
          location,
          name,
          lastName);
    }
    
    @Override
     public NameStep location(String location) {
        Objects.requireNonNull(location);
        this.location = location;
        return this;
    }
    
    @Override
     public BuildStep name(String name) {
        Objects.requireNonNull(name);
        this.name = name;
        return this;
    }
    
    @Override
     public BuildStep lastName(String lastName) {
        this.lastName = lastName;
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
    private CopyOfBuilder(String id, String location, String name, String lastName) {
      super.id(id);
      super.location(location)
        .name(name)
        .lastName(lastName);
    }
    
    @Override
     public CopyOfBuilder location(String location) {
      return (CopyOfBuilder) super.location(location);
    }
    
    @Override
     public CopyOfBuilder name(String name) {
      return (CopyOfBuilder) super.name(name);
    }
    
    @Override
     public CopyOfBuilder lastName(String lastName) {
      return (CopyOfBuilder) super.lastName(lastName);
    }
  }
  
}
