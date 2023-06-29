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
import com.amplifyframework.core.model.annotations.Index;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.core.model.annotations.ModelField;
import com.amplifyframework.core.model.query.predicate.QueryField;
import com.amplifyframework.core.model.temporal.Temporal;

import java.util.Objects;

import static com.amplifyframework.core.model.query.predicate.QueryField.field;

/** This is an auto generated class representing the StringModelWithIdentifier type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "StringModelWithIdentifiers", type = Model.Type.USER, version = 1)
@Index(name = "undefined", fields = {"customKey"})
public final class StringModelWithIdentifier implements Model {
  public static final QueryField CUSTOM_KEY = field("StringModelWithIdentifier", "customKey");
  private final @ModelField(targetType="String", isRequired = true) String customKey;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime createdAt;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime updatedAt;
  /** @deprecated This API is internal to Amplify and should not be used. */
  @Deprecated
   public String resolveIdentifier() {
    return customKey;
  }
  
  public String getCustomKey() {
      return customKey;
  }
  
  public Temporal.DateTime getCreatedAt() {
      return createdAt;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  private StringModelWithIdentifier(String customKey) {
    this.customKey = customKey;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      StringModelWithIdentifier stringModelWithIdentifier = (StringModelWithIdentifier) obj;
      return ObjectsCompat.equals(getCustomKey(), stringModelWithIdentifier.getCustomKey()) &&
              ObjectsCompat.equals(getCreatedAt(), stringModelWithIdentifier.getCreatedAt()) &&
              ObjectsCompat.equals(getUpdatedAt(), stringModelWithIdentifier.getUpdatedAt());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getCustomKey())
      .append(getCreatedAt())
      .append(getUpdatedAt())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("StringModelWithIdentifier {")
      .append("customKey=" + String.valueOf(getCustomKey()) + ", ")
      .append("createdAt=" + String.valueOf(getCreatedAt()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()))
      .append("}")
      .toString();
  }
  
  public static CustomKeyStep builder() {
      return new Builder();
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(customKey);
  }
  public interface CustomKeyStep {
    BuildStep customKey(String customKey);
  }
  

  public interface BuildStep {
    StringModelWithIdentifier build();
  }
  

  public static class Builder implements CustomKeyStep, BuildStep {
    private String customKey;
    @Override
     public StringModelWithIdentifier build() {
        
        return new StringModelWithIdentifier(
          customKey);
    }
    
    @Override
     public BuildStep customKey(String customKey) {
        Objects.requireNonNull(customKey);
        this.customKey = customKey;
        return this;
    }
  }
  

  public final class CopyOfBuilder extends Builder {
    private CopyOfBuilder(String customKey) {
      super.customKey(customKey);
    }
    
    @Override
     public CopyOfBuilder customKey(String customKey) {
      return (CopyOfBuilder) super.customKey(customKey);
    }
  }
  

  public static class StringModelWithIdentifierIdentifier extends ModelIdentifier<StringModelWithIdentifier> {
    private static final long serialVersionUID = 1L;
    public StringModelWithIdentifierIdentifier(String customKey) {
      super(customKey);
    }
  }
  
}
