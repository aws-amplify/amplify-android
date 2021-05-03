package com.amplifyframework.testmodels.ecommerce;

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

/** This is an auto generated class representing the Item type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "Items")
@Index(name = "undefined", fields = {"orderId","status","createdAt"})
@Index(name = "ByStatus", fields = {"status","createdAt"})
public final class Item implements Model {
  public static final QueryField ID = field("id");
  public static final QueryField ORDER_ID = field("orderId");
  public static final QueryField STATUS = field("status");
  public static final QueryField CREATED_AT = field("createdAt");
  public static final QueryField NAME = field("name");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="ID", isRequired = true) String orderId;
  private final @ModelField(targetType="Status", isRequired = true) Status status;
  private final @ModelField(targetType="AWSDateTime", isRequired = true) Temporal.DateTime createdAt;
  private final @ModelField(targetType="String", isRequired = true) String name;
  public String getId() {
      return id;
  }
  
  public String getOrderId() {
      return orderId;
  }
  
  public Status getStatus() {
      return status;
  }
  
  public Temporal.DateTime getCreatedAt() {
      return createdAt;
  }
  
  public String getName() {
      return name;
  }
  
  private Item(String id, String orderId, Status status, Temporal.DateTime createdAt, String name) {
    this.id = id;
    this.orderId = orderId;
    this.status = status;
    this.createdAt = createdAt;
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
      return ObjectsCompat.equals(getId(), item.getId()) &&
              ObjectsCompat.equals(getOrderId(), item.getOrderId()) &&
              ObjectsCompat.equals(getStatus(), item.getStatus()) &&
              ObjectsCompat.equals(getCreatedAt(), item.getCreatedAt()) &&
              ObjectsCompat.equals(getName(), item.getName());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getOrderId())
      .append(getStatus())
      .append(getCreatedAt())
      .append(getName())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("Item {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("orderId=" + String.valueOf(getOrderId()) + ", ")
      .append("status=" + String.valueOf(getStatus()) + ", ")
      .append("createdAt=" + String.valueOf(getCreatedAt()) + ", ")
      .append("name=" + String.valueOf(getName()))
      .append("}")
      .toString();
  }
  
  public static OrderIdStep builder() {
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
  public static Item justId(String id) {
    try {
      UUID.fromString(id); // Check that ID is in the UUID format - if not an exception is thrown
    } catch (Exception exception) {
      throw new IllegalArgumentException(
              "Model IDs must be unique in the format of UUID. This method is for creating instances " +
              "of an existing object with only its ID field for sending as a mutation parameter. When " +
              "creating a new object, use the standard builder method and leave the ID field blank."
      );
    }
    return new Item(
      id,
      null,
      null,
      null,
      null
    );
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(id,
      orderId,
      status,
      createdAt,
      name);
  }
  public interface OrderIdStep {
    StatusStep orderId(String orderId);
  }
  

  public interface StatusStep {
    CreatedAtStep status(Status status);
  }
  

  public interface CreatedAtStep {
    NameStep createdAt(Temporal.DateTime createdAt);
  }
  

  public interface NameStep {
    BuildStep name(String name);
  }
  

  public interface BuildStep {
    Item build();
    BuildStep id(String id) throws IllegalArgumentException;
  }
  

  public static class Builder implements OrderIdStep, StatusStep, CreatedAtStep, NameStep, BuildStep {
    private String id;
    private String orderId;
    private Status status;
    private Temporal.DateTime createdAt;
    private String name;
    @Override
     public Item build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new Item(
          id,
          orderId,
          status,
          createdAt,
          name);
    }
    
    @Override
     public StatusStep orderId(String orderId) {
        Objects.requireNonNull(orderId);
        this.orderId = orderId;
        return this;
    }
    
    @Override
     public CreatedAtStep status(Status status) {
        Objects.requireNonNull(status);
        this.status = status;
        return this;
    }
    
    @Override
     public NameStep createdAt(Temporal.DateTime createdAt) {
        Objects.requireNonNull(createdAt);
        this.createdAt = createdAt;
        return this;
    }
    
    @Override
     public BuildStep name(String name) {
        Objects.requireNonNull(name);
        this.name = name;
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
    private CopyOfBuilder(String id, String orderId, Status status, Temporal.DateTime createdAt, String name) {
      super.id(id);
      super.orderId(orderId)
        .status(status)
        .createdAt(createdAt)
        .name(name);
    }
    
    @Override
     public CopyOfBuilder orderId(String orderId) {
      return (CopyOfBuilder) super.orderId(orderId);
    }
    
    @Override
     public CopyOfBuilder status(Status status) {
      return (CopyOfBuilder) super.status(status);
    }
    
    @Override
     public CopyOfBuilder createdAt(Temporal.DateTime createdAt) {
      return (CopyOfBuilder) super.createdAt(createdAt);
    }
    
    @Override
     public CopyOfBuilder name(String name) {
      return (CopyOfBuilder) super.name(name);
    }
  }
  
}
