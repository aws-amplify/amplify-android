package com.amplifyframework.testmodels.phonecall;

import static com.amplifyframework.core.model.query.predicate.QueryField.field;

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
  public static final QueryField START_TIME = field("Call", "startTime");
  public static final QueryField END_TIME = field("Call", "endTime");
  public static final QueryField CALLER = field("Call", "callerId");
  public static final QueryField CALLEE = field("Call", "calleeId");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="AWSDateTime", isRequired = true) Temporal.DateTime startTime;
  private final @ModelField(targetType="AWSDateTime", isRequired = true) Temporal.DateTime endTime;
  private final @ModelField(targetType="Person", isRequired = true) @BelongsTo(targetName = "callerId", type = Person.class) Person caller;
  private final @ModelField(targetType="Person", isRequired = true) @BelongsTo(targetName = "calleeId", type = Person.class) Person callee;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime createdAt;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime updatedAt;
  public String getId() {
      return id;
  }
  
  public Temporal.DateTime getStartTime() {
      return startTime;
  }
  
  public Temporal.DateTime getEndTime() {
      return endTime;
  }
  
  public Person getCaller() {
      return caller;
  }
  
  public Person getCallee() {
      return callee;
  }
  
  public Temporal.DateTime getCreatedAt() {
      return createdAt;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  private Call(String id, Temporal.DateTime startTime, Temporal.DateTime endTime, Person caller, Person callee) {
    this.id = id;
    this.startTime = startTime;
    this.endTime = endTime;
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
              ObjectsCompat.equals(getEndTime(), call.getEndTime()) &&
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
      .append(getEndTime())
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
      .append("endTime=" + String.valueOf(getEndTime()) + ", ")
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
      null,
      null
    );
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(id,
      startTime,
      endTime,
      caller,
      callee);
  }
  public interface StartTimeStep {
    EndTimeStep startTime(Temporal.DateTime startTime);
  }
  

  public interface EndTimeStep {
    CallerStep endTime(Temporal.DateTime endTime);
  }
  

  public interface CallerStep {
    CalleeStep caller(Person caller);
  }
  

  public interface CalleeStep {
    BuildStep callee(Person callee);
  }
  

  public interface BuildStep {
    Call build();
    BuildStep id(String id) throws IllegalArgumentException;
  }
  

  public static class Builder implements StartTimeStep, EndTimeStep, CallerStep, CalleeStep, BuildStep {
    private String id;
    private Temporal.DateTime startTime;
    private Temporal.DateTime endTime;
    private Person caller;
    private Person callee;
    @Override
     public Call build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new Call(
          id,
          startTime,
          endTime,
          caller,
          callee);
    }
    
    @Override
     public EndTimeStep startTime(Temporal.DateTime startTime) {
        Objects.requireNonNull(startTime);
        this.startTime = startTime;
        return this;
    }
    
    @Override
     public CallerStep endTime(Temporal.DateTime endTime) {
        Objects.requireNonNull(endTime);
        this.endTime = endTime;
        return this;
    }
    
    @Override
     public CalleeStep caller(Person caller) {
        Objects.requireNonNull(caller);
        this.caller = caller;
        return this;
    }
    
    @Override
     public BuildStep callee(Person callee) {
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
    private CopyOfBuilder(String id, Temporal.DateTime startTime, Temporal.DateTime endTime, Person caller, Person callee) {
      super.id(id);
      super.startTime(startTime)
        .endTime(endTime)
        .caller(caller)
        .callee(callee);
    }
    
    @Override
     public CopyOfBuilder startTime(Temporal.DateTime startTime) {
      return (CopyOfBuilder) super.startTime(startTime);
    }
    
    @Override
     public CopyOfBuilder endTime(Temporal.DateTime endTime) {
      return (CopyOfBuilder) super.endTime(endTime);
    }
    
    @Override
     public CopyOfBuilder caller(Person caller) {
      return (CopyOfBuilder) super.caller(caller);
    }
    
    @Override
     public CopyOfBuilder callee(Person callee) {
      return (CopyOfBuilder) super.callee(callee);
    }
  }
  
}
