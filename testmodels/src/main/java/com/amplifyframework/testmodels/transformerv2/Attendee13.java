package com.amplifyframework.testmodels.transformerv2;

import com.amplifyframework.core.model.annotations.HasMany;
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

/** This is an auto generated class representing the Attendee13 type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "Attendee13s")
@Index(name = "undefined", fields = {"id"})
public final class Attendee13 implements Model {
  public static final QueryField ID = field("Attendee13", "id");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="Registration13") @HasMany(associatedWith = "attendee", type = Registration13.class) List<Registration13> meetings = null;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime createdAt;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime updatedAt;
  public String getId() {
      return id;
  }
  
  public List<Registration13> getMeetings() {
      return meetings;
  }
  
  public Temporal.DateTime getCreatedAt() {
      return createdAt;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  private Attendee13(String id) {
    this.id = id;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      Attendee13 attendee13 = (Attendee13) obj;
      return ObjectsCompat.equals(getId(), attendee13.getId()) &&
              ObjectsCompat.equals(getCreatedAt(), attendee13.getCreatedAt()) &&
              ObjectsCompat.equals(getUpdatedAt(), attendee13.getUpdatedAt());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getCreatedAt())
      .append(getUpdatedAt())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("Attendee13 {")
      .append("id=" + String.valueOf(getId()) + ", ")
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
  public static Attendee13 justId(String id) {
    return new Attendee13(
      id
    );
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(id);
  }
  public interface BuildStep {
    Attendee13 build();
    BuildStep id(String id);
  }
  

  public static class Builder implements BuildStep {
    private String id;
    @Override
     public Attendee13 build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new Attendee13(
          id);
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
    private CopyOfBuilder(String id) {
      super.id(id);
      
    }
  }
  
}
