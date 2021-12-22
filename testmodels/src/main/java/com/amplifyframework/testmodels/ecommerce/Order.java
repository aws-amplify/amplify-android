package com.amplifyframework.testmodels.ecommerce;

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

/** This is an auto generated class representing the Order type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "Orders")
@Index(name = "undefined", fields = {"customerEmail","createdAt"})
public final class Order implements Model {
  public static final QueryField ID = field("id");
  public static final QueryField CUSTOMER_EMAIL = field("customerEmail");
  public static final QueryField CREATED_AT = field("createdAt");
  public static final QueryField ORDER_ID = field("orderId");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String", isRequired = true) String customerEmail;
  private final @ModelField(targetType="String", isRequired = true) String createdAt;
  private final @ModelField(targetType="ID", isRequired = true) String orderId;
  public String getId() {
      return id;
  }
  
  public String getCustomerEmail() {
      return customerEmail;
  }
  
  public String getCreatedAt() {
      return createdAt;
  }
  
  public String getOrderId() {
      return orderId;
  }
  
  private Order(String id, String customerEmail, String createdAt, String orderId) {
    this.id = id;
    this.customerEmail = customerEmail;
    this.createdAt = createdAt;
    this.orderId = orderId;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      Order order = (Order) obj;
      return ObjectsCompat.equals(getId(), order.getId()) &&
              ObjectsCompat.equals(getCustomerEmail(), order.getCustomerEmail()) &&
              ObjectsCompat.equals(getCreatedAt(), order.getCreatedAt()) &&
              ObjectsCompat.equals(getOrderId(), order.getOrderId());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getCustomerEmail())
      .append(getCreatedAt())
      .append(getOrderId())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("Order {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("customerEmail=" + String.valueOf(getCustomerEmail()) + ", ")
      .append("createdAt=" + String.valueOf(getCreatedAt()) + ", ")
      .append("orderId=" + String.valueOf(getOrderId()))
      .append("}")
      .toString();
  }
  
  public static CustomerEmailStep builder() {
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
  public static Order justId(String id) {
    try {
      UUID.fromString(id); // Check that ID is in the UUID format - if not an exception is thrown
    } catch (Exception exception) {
      throw new IllegalArgumentException(
              "Model IDs must be unique in the format of UUID. This method is for creating instances " +
              "of an existing object with only its ID field for sending as a mutation parameter. When " +
              "creating a new object, use the standard builder method and leave the ID field blank."
      );
    }
    return new Order(
      id,
      null,
      null,
      null
    );
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(id,
      customerEmail,
      createdAt,
      orderId);
  }
  public interface CustomerEmailStep {
    CreatedAtStep customerEmail(String customerEmail);
  }
  

  public interface CreatedAtStep {
    OrderIdStep createdAt(String createdAt);
  }
  

  public interface OrderIdStep {
    BuildStep orderId(String orderId);
  }
  

  public interface BuildStep {
    Order build();
    BuildStep id(String id) throws IllegalArgumentException;
  }
  

  public static class Builder implements CustomerEmailStep, CreatedAtStep, OrderIdStep, BuildStep {
    private String id;
    private String customerEmail;
    private String createdAt;
    private String orderId;
    @Override
     public Order build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new Order(
          id,
          customerEmail,
          createdAt,
          orderId);
    }
    
    @Override
     public CreatedAtStep customerEmail(String customerEmail) {
        Objects.requireNonNull(customerEmail);
        this.customerEmail = customerEmail;
        return this;
    }
    
    @Override
     public OrderIdStep createdAt(String createdAt) {
        Objects.requireNonNull(createdAt);
        this.createdAt = createdAt;
        return this;
    }
    
    @Override
     public BuildStep orderId(String orderId) {
        Objects.requireNonNull(orderId);
        this.orderId = orderId;
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
    private CopyOfBuilder(String id, String customerEmail, String createdAt, String orderId) {
      super.id(id);
      super.customerEmail(customerEmail)
        .createdAt(createdAt)
        .orderId(orderId);
    }
    
    @Override
     public CopyOfBuilder customerEmail(String customerEmail) {
      return (CopyOfBuilder) super.customerEmail(customerEmail);
    }
    
    @Override
     public CopyOfBuilder createdAt(String createdAt) {
      return (CopyOfBuilder) super.createdAt(createdAt);
    }
    
    @Override
     public CopyOfBuilder orderId(String orderId) {
      return (CopyOfBuilder) super.orderId(orderId);
    }
  }
  
}
