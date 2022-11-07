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

package com.amplifyframework.testmodels.phonecall;

import static com.amplifyframework.core.model.query.predicate.QueryField.field;

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.annotations.BelongsTo;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.core.model.annotations.ModelField;
import com.amplifyframework.core.model.query.predicate.QueryField;
import com.amplifyframework.core.model.temporal.Temporal;

import java.util.Objects;
import java.util.UUID;

/** This is an auto generated class representing the Call type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "Calls")
public final class Call implements Model {
  public static final QueryField ID = field("Call", "id");
  public static final QueryField STARTTIME = field("Call", "startTime");
  public static final QueryField CALLER = field("Call", "callerId");
  public static final QueryField CALLEE = field("Call", "calleeId");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="AWSTime", isRequired = true) Temporal.Time startTime;
  private final @ModelField(targetType="Phone", isRequired = true) @BelongsTo(targetName = "callerId", type = Phone.class) Phone caller;
  private final @ModelField(targetType="Phone", isRequired = true) @BelongsTo(targetName = "calleeId", type = Phone.class) Phone callee;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime createdAt;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime updatedAt;
  @NonNull
  public String resolveIdentifier() {
      return id;
  }

  public String getId() {
      return id;
  }
  
  public Temporal.Time getStartTime() {
      return startTime;
  }
  
  public Phone getCaller() {
      return caller;
  }
  
  public Phone getCallee() {
      return callee;
  }
  
  public Temporal.DateTime getCreatedAt() {
      return createdAt;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  private Call(String id, Temporal.Time startTime, Phone caller, Phone callee) {
    this.id = id;
    this.startTime = startTime;
    this.caller = caller;
    this.callee = callee;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      Call call = (Call) obj;
      return ObjectsCompat.equals(getId(), call.getId()) &&
              ObjectsCompat.equals(getStartTime(), call.getStartTime()) &&
              ObjectsCompat.equals(getCaller(), call.getCaller()) &&
              ObjectsCompat.equals(getCallee(), call.getCallee()) &&
              ObjectsCompat.equals(getCreatedAt(), call.getCreatedAt()) &&
              ObjectsCompat.equals(getUpdatedAt(), call.getUpdatedAt());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getStartTime())
      .append(getCaller())
      .append(getCallee())
      .append(getCreatedAt())
      .append(getUpdatedAt())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("Call {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("startTime=" + String.valueOf(getStartTime()) + ", ")
      .append("caller=" + String.valueOf(getCaller()) + ", ")
      .append("callee=" + String.valueOf(getCallee()) + ", ")
      .append("createdAt=" + String.valueOf(getCreatedAt()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()))
      .append("}")
      .toString();
  }
  
  public static StartTimeStep builder() {
      return new Builder();
  }
  
  /** 
   * WARNING: This method should not be used to build an instance of this object for a CREATE mutation.
   * This is a convenience method to return an instance of the object with only its ID populated
   * to be used in the context of a parameter in a delete mutation or referencing a foreign key
   * in a relationship.
   * @param id the id of the existing item this instance will represent
   * @return an instance of this model with only ID populated
   * @throws IllegalArgumentException Checks that ID is in the proper format
   */
  public static Call justId(String id) {
    try {
      UUID.fromString(id); // Check that ID is in the UUID format - if not an exception is thrown
    } catch (Exception exception) {
      throw new IllegalArgumentException(
              "Model IDs must be unique in the format of UUID. This method is for creating instances " +
              "of an existing object with only its ID field for sending as a mutation parameter. When " +
              "creating a new object, use the standard builder method and leave the ID field blank."
      );
    }
    return new Call(
      id,
      null,
      null,
      null
    );
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(id,
      startTime,
      caller,
      callee);
  }
  public interface StartTimeStep {
    CallerStep startTime(Temporal.Time startTime);
  }
  

  public interface CallerStep {
    CalleeStep caller(Phone caller);
  }
  

  public interface CalleeStep {
    BuildStep callee(Phone callee);
  }
  

  public interface BuildStep {
    Call build();
    BuildStep id(String id) throws IllegalArgumentException;
  }
  

  public static class Builder implements StartTimeStep, CallerStep, CalleeStep, BuildStep {
    private String id;
    private Temporal.Time startTime;
    private Phone caller;
    private Phone callee;
    @Override
     public Call build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new Call(
          id,
          startTime,
          caller,
          callee);
    }
    
    @Override
     public CallerStep startTime(Temporal.Time startTime) {
        Objects.requireNonNull(startTime);
        this.startTime = startTime;
        return this;
    }
    
    @Override
     public CalleeStep caller(Phone caller) {
        Objects.requireNonNull(caller);
        this.caller = caller;
        return this;
    }
    
    @Override
     public BuildStep callee(Phone callee) {
        Objects.requireNonNull(callee);
        this.callee = callee;
        return this;
    }
    
    /** 
     * WARNING: Do not set ID when creating a new object. Leave this blank and one will be auto generated for you.
     * This should only be set when referring to an already existing object.
     * @param id id
     * @return Current Builder instance, for fluent method chaining
     * @throws IllegalArgumentException Checks that ID is in the proper format
     */
    public BuildStep id(String id) throws IllegalArgumentException {
        this.id = id;
        
        try {
            UUID.fromString(id); // Check that ID is in the UUID format - if not an exception is thrown
        } catch (Exception exception) {
          throw new IllegalArgumentException("Model IDs must be unique in the format of UUID.",
                    exception);
        }
        
        return this;
    }
  }
  

  public final class CopyOfBuilder extends Builder {
    private CopyOfBuilder(String id, Temporal.Time startTime, Phone caller, Phone callee) {
      super.id(id);
      super.startTime(startTime)
        .caller(caller)
        .callee(callee);
    }
    
    @Override
     public CopyOfBuilder startTime(Temporal.Time startTime) {
      return (CopyOfBuilder) super.startTime(startTime);
    }
    
    @Override
     public CopyOfBuilder caller(Phone caller) {
      return (CopyOfBuilder) super.caller(caller);
    }
    
    @Override
     public CopyOfBuilder callee(Phone callee) {
      return (CopyOfBuilder) super.callee(callee);
    }
  }
  
}
