package com.amplifyframework.testmodels.transformerv2;

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

/** This is an auto generated class representing the Registration13 type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "Registration13s")
@Index(name = "undefined", fields = {"id"})
@Index(name = "byMeeting", fields = {"meetingId","attendeeId"})
@Index(name = "byAttendee", fields = {"attendeeId","meetingId"})
public final class Registration13 implements Model {
  public static final QueryField ID = field("Registration13", "id");
  public static final QueryField MEETING = field("Registration13", "meetingId");
  public static final QueryField ATTENDEE = field("Registration13", "attendeeId");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="Meeting13", isRequired = true) @BelongsTo(targetName = "meetingId", type = Meeting13.class) Meeting13 meeting;
  private final @ModelField(targetType="Attendee13", isRequired = true) @BelongsTo(targetName = "attendeeId", type = Attendee13.class) Attendee13 attendee;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime createdAt;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime updatedAt;
  public String getId() {
      return id;
  }
  
  public Meeting13 getMeeting() {
      return meeting;
  }
  
  public Attendee13 getAttendee() {
      return attendee;
  }
  
  public Temporal.DateTime getCreatedAt() {
      return createdAt;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  private Registration13(String id, Meeting13 meeting, Attendee13 attendee) {
    this.id = id;
    this.meeting = meeting;
    this.attendee = attendee;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      Registration13 registration13 = (Registration13) obj;
      return ObjectsCompat.equals(getId(), registration13.getId()) &&
              ObjectsCompat.equals(getMeeting(), registration13.getMeeting()) &&
              ObjectsCompat.equals(getAttendee(), registration13.getAttendee()) &&
              ObjectsCompat.equals(getCreatedAt(), registration13.getCreatedAt()) &&
              ObjectsCompat.equals(getUpdatedAt(), registration13.getUpdatedAt());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getMeeting())
      .append(getAttendee())
      .append(getCreatedAt())
      .append(getUpdatedAt())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("Registration13 {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("meeting=" + String.valueOf(getMeeting()) + ", ")
      .append("attendee=" + String.valueOf(getAttendee()) + ", ")
      .append("createdAt=" + String.valueOf(getCreatedAt()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()))
      .append("}")
      .toString();
  }
  
  public static MeetingStep builder() {
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
  public static Registration13 justId(String id) {
    return new Registration13(
      id,
      null,
      null
    );
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(id,
      meeting,
      attendee);
  }
  public interface MeetingStep {
    AttendeeStep meeting(Meeting13 meeting);
  }
  

  public interface AttendeeStep {
    BuildStep attendee(Attendee13 attendee);
  }
  

  public interface BuildStep {
    Registration13 build();
    BuildStep id(String id);
  }
  

  public static class Builder implements MeetingStep, AttendeeStep, BuildStep {
    private String id;
    private Meeting13 meeting;
    private Attendee13 attendee;
    @Override
     public Registration13 build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new Registration13(
          id,
          meeting,
          attendee);
    }
    
    @Override
     public AttendeeStep meeting(Meeting13 meeting) {
        Objects.requireNonNull(meeting);
        this.meeting = meeting;
        return this;
    }
    
    @Override
     public BuildStep attendee(Attendee13 attendee) {
        Objects.requireNonNull(attendee);
        this.attendee = attendee;
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
    private CopyOfBuilder(String id, Meeting13 meeting, Attendee13 attendee) {
      super.id(id);
      super.meeting(meeting)
        .attendee(attendee);
    }
    
    @Override
     public CopyOfBuilder meeting(Meeting13 meeting) {
      return (CopyOfBuilder) super.meeting(meeting);
    }
    
    @Override
     public CopyOfBuilder attendee(Attendee13 attendee) {
      return (CopyOfBuilder) super.attendee(attendee);
    }
  }
  
}
