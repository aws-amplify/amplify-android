package com.amplifyframework.testmodels.phonecall;

import static com.amplifyframework.core.model.query.predicate.QueryField.field;

import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.annotations.BelongsTo;
import com.amplifyframework.core.model.annotations.HasMany;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.core.model.annotations.ModelField;
import com.amplifyframework.core.model.query.predicate.QueryField;
import com.amplifyframework.core.model.temporal.Temporal;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** This is an auto generated class representing the Phone type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "Phones")
public final class Phone implements Model {
  public static final QueryField ID = field("Phone", "id");
  public static final QueryField NUMBER = field("Phone", "number");
  public static final QueryField OWNED_BY = field("Phone", "ownedById");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String", isRequired = true) String number;
  private final @ModelField(targetType="Person", isRequired = true) @BelongsTo(targetName = "ownedById", type = Person.class) Person ownedBy;
  private final @ModelField(targetType="Call") @HasMany(associatedWith = "id", type = Call.class) List<Call> calls = null;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime createdAt;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime updatedAt;
  public String getId() {
      return id;
  }
  
  public String getNumber() {
      return number;
  }
  
  public Person getOwnedBy() {
      return ownedBy;
  }
  
  public List<Call> getCalls() {
      return calls;
  }
  
  public Temporal.DateTime getCreatedAt() {
      return createdAt;
  }
  
  public Temporal.DateTime getUpdatedAt() {
      return updatedAt;
  }
  
  private Phone(String id, String number, Person ownedBy) {
    this.id = id;
    this.number = number;
    this.ownedBy = ownedBy;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      Phone phone = (Phone) obj;
      return ObjectsCompat.equals(getId(), phone.getId()) &&
              ObjectsCompat.equals(getNumber(), phone.getNumber()) &&
              ObjectsCompat.equals(getOwnedBy(), phone.getOwnedBy()) &&
              ObjectsCompat.equals(getCreatedAt(), phone.getCreatedAt()) &&
              ObjectsCompat.equals(getUpdatedAt(), phone.getUpdatedAt());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getNumber())
      .append(getOwnedBy())
      .append(getCreatedAt())
      .append(getUpdatedAt())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("Phone {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("number=" + String.valueOf(getNumber()) + ", ")
      .append("ownedBy=" + String.valueOf(getOwnedBy()) + ", ")
      .append("createdAt=" + String.valueOf(getCreatedAt()) + ", ")
      .append("updatedAt=" + String.valueOf(getUpdatedAt()))
      .append("}")
      .toString();
  }
  
  public static NumberStep builder() {
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
  public static Phone justId(String id) {
    try {
      UUID.fromString(id); // Check that ID is in the UUID format - if not an exception is thrown
    } catch (Exception exception) {
      throw new IllegalArgumentException(
              "Model IDs must be unique in the format of UUID. This method is for creating instances " +
              "of an existing object with only its ID field for sending as a mutation parameter. When " +
              "creating a new object, use the standard builder method and leave the ID field blank."
      );
    }
    return new Phone(
      id,
      null,
      null
    );
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(id,
      number,
      ownedBy);
  }
  public interface NumberStep {
    OwnedByStep number(String number);
  }

  public interface OwnedByStep {
    BuildStep ownedBy(Person ownedBy);
  }
  

  public interface BuildStep {
    Phone build();
    BuildStep id(String id) throws IllegalArgumentException;
  }
  

  public static class Builder implements NumberStep, OwnedByStep, BuildStep {
    private String id;
    private String number;
    private Person ownedBy;
    @Override
     public Phone build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new Phone(
          id,
          number,
          ownedBy);
    }
    
    @Override
     public OwnedByStep number(String number) {
        Objects.requireNonNull(number);
        this.number = number;
        return this;
    }
    
    @Override
     public BuildStep ownedBy(Person ownedBy) {
        this.ownedBy = ownedBy;
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
    private CopyOfBuilder(String id, String number, Person ownedBy) {
      super.id(id);
      super.number(number)
        .ownedBy(ownedBy);
    }
    
    @Override
     public CopyOfBuilder number(String number) {
      return (CopyOfBuilder) super.number(number);
    }
    
    @Override
     public CopyOfBuilder ownedBy(Person ownedBy) {
      return (CopyOfBuilder) super.ownedBy(ownedBy);
    }
  }
  
}
