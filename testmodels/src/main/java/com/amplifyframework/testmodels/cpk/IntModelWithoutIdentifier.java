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

import static com.amplifyframework.core.model.query.predicate.QueryField.field;

import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.annotations.Index;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.core.model.annotations.ModelField;
import com.amplifyframework.core.model.query.predicate.QueryField;
import com.amplifyframework.core.model.temporal.Temporal;

import java.util.Objects;

/** This is an auto generated class representing the CpkIntModel type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "IntModelWithoutIdentifiers", type = Model.Type.USER, version = 1)
@Index(name = "undefined", fields = {"customId"})
public final class IntModelWithoutIdentifier implements Model {
  public static final QueryField CUSTOM_ID = field("IntModelWithoutIdentifier", "customId");
  private final @ModelField(targetType="Int", isRequired = true) Integer customId;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime createdAt;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime updatedAt;
  public Integer resolveIdentifier() {
    return customId;
  }

  public Integer getCustomId() {
      return customId;
  }

  public Temporal.DateTime getCreatedAt() {
      return createdAt;
  }

  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }

  private IntModelWithoutIdentifier(Integer customId) {
    this.customId = customId;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
          IntModelWithoutIdentifier intModelWithoutIdentifier = (IntModelWithoutIdentifier) obj;
      return ObjectsCompat.equals(getCustomId(), intModelWithoutIdentifier.getCustomId()) &&
              ObjectsCompat.equals(getCreatedAt(), intModelWithoutIdentifier.getCreatedAt()) &&
              ObjectsCompat.equals(getUpdatedAt(), intModelWithoutIdentifier.getUpdatedAt());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getCustomId())
      .append(getCreatedAt())
      .append(getUpdatedAt())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("CpkIntModel {")
      .append("customId=" + String.valueOf(getCustomId()) + ", ")
      .append("createdAt=" + String.valueOf(getCreatedAt()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()))
      .append("}")
      .toString();
  }
  
  public static CustomIdStep builder() {
      return new Builder();
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(customId);
  }
  public interface CustomIdStep {
    BuildStep customId(Integer customId);
  }
  

  public interface BuildStep {
    IntModelWithoutIdentifier build();
  }
  

  public static class Builder implements CustomIdStep, BuildStep {
    private Integer customId;
    @Override
     public IntModelWithoutIdentifier build() {
        
        return new IntModelWithoutIdentifier(
          customId);
    }
    
    @Override
     public BuildStep customId(Integer customId) {
        Objects.requireNonNull(customId);
        this.customId = customId;
        return this;
    }
  }
  

  public final class CopyOfBuilder extends Builder {
    private CopyOfBuilder(Integer customId) {
      super.customId(customId);
    }
    
    @Override
     public CopyOfBuilder customId(Integer customId) {
      return (CopyOfBuilder) super.customId(customId);
    }
  }
  
}
