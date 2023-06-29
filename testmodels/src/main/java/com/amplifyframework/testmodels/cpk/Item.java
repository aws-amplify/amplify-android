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

/** This is an auto generated class representing the Item type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "Items", type = Model.Type.USER, version = 1)
@Index(name = "undefined", fields = {"customKey"})
public final class Item implements Model {
  public static final QueryField CUSTOM_KEY = field("Item", "customKey");
  public static final QueryField NAME = field("Item", "name");
  private final @ModelField(targetType="String", isRequired = true) String customKey;
  private final @ModelField(targetType="String", isRequired = true) String name;
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
  
  public String getName() {
      return name;
  }
  
  public Temporal.DateTime getCreatedAt() {
      return createdAt;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  private Item(String customKey, String name) {
    this.customKey = customKey;
    this.name = name;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      Item item = (Item) obj;
      return ObjectsCompat.equals(getCustomKey(), item.getCustomKey()) &&
              ObjectsCompat.equals(getName(), item.getName()) &&
              ObjectsCompat.equals(getCreatedAt(), item.getCreatedAt()) &&
              ObjectsCompat.equals(getUpdatedAt(), item.getUpdatedAt());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getCustomKey())
      .append(getName())
      .append(getCreatedAt())
      .append(getUpdatedAt())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("Item {")
      .append("customKey=" + String.valueOf(getCustomKey()) + ", ")
      .append("name=" + String.valueOf(getName()) + ", ")
      .append("createdAt=" + String.valueOf(getCreatedAt()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()))
      .append("}")
      .toString();
  }
  
  public static CustomKeyStep builder() {
      return new Builder();
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(customKey,
      name);
  }
  public interface CustomKeyStep {
    NameStep customKey(String customKey);
  }
  

  public interface NameStep {
    BuildStep name(String name);
  }
  

  public interface BuildStep {
    Item build();
  }
  

  public static class Builder implements CustomKeyStep, NameStep, BuildStep {
    private String customKey;
    private String name;
    @Override
     public Item build() {
        
        return new Item(
          customKey,
          name);
    }
    
    @Override
     public NameStep customKey(String customKey) {
        Objects.requireNonNull(customKey);
        this.customKey = customKey;
        return this;
    }
    
    @Override
     public BuildStep name(String name) {
        Objects.requireNonNull(name);
        this.name = name;
        return this;
    }
  }
  

  public final class CopyOfBuilder extends Builder {
    private CopyOfBuilder(String customKey, String name) {
      super.customKey(customKey)
        .name(name);
    }
    
    @Override
     public CopyOfBuilder customKey(String customKey) {
      return (CopyOfBuilder) super.customKey(customKey);
    }
    
    @Override
     public CopyOfBuilder name(String name) {
      return (CopyOfBuilder) super.name(name);
    }
  }
  

  public static class ItemIdentifier extends ModelIdentifier<Item> {
    private static final long serialVersionUID = 1L;
    public ItemIdentifier(String customKey) {
      super(customKey);
    }
  }
  
}
