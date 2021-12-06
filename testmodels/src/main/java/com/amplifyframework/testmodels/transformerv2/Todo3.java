package com.amplifyframework.testmodels.transformerv2;

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

/** This is an auto generated class representing the Todo3 type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "Todo3s")
public final class Todo3 implements Model {
  public static final QueryField ID = field("Todo3", "id");
  public static final QueryField CONTENT = field("Todo3", "content");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String") String content;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime createdOn;
  private @ModelField(targetType="AWSDateTime", isReadOnly = true) Temporal.DateTime updatedOn;
  public String getId() {
      return id;
  }
  
  public String getContent() {
      return content;
  }
  
  public Temporal.DateTime getCreatedOn() {
      return createdOn;
  }
  
  public Temporal.DateTime getUpdatedOn() {
      return updatedOn;
  }
  
  private Todo3(String id, String content) {
    this.id = id;
    this.content = content;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      Todo3 todo3 = (Todo3) obj;
      return ObjectsCompat.equals(getId(), todo3.getId()) &&
              ObjectsCompat.equals(getContent(), todo3.getContent()) &&
              ObjectsCompat.equals(getCreatedOn(), todo3.getCreatedOn()) &&
              ObjectsCompat.equals(getUpdatedOn(), todo3.getUpdatedOn());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getContent())
      .append(getCreatedOn())
      .append(getUpdatedOn())
      .toString()
      .hashCode();
  }
  
  @Override
   public String toString() {
    return new StringBuilder()
      .append("Todo3 {")
      .append("id=" + String.valueOf(getId()) + ", ")
      .append("content=" + String.valueOf(getContent()) + ", ")
      .append("createdOn=" + String.valueOf(getCreatedOn()) + ", ")
      .append("updatedOn=" + String.valueOf(getUpdatedOn()))
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
  public static Todo3 justId(String id) {
    return new Todo3(
      id,
      null
    );
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(id,
      content);
  }
  public interface BuildStep {
    Todo3 build();
    BuildStep id(String id);
    BuildStep content(String content);
  }
  

  public static class Builder implements BuildStep {
    private String id;
    private String content;
    @Override
     public Todo3 build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new Todo3(
          id,
          content);
    }
    
    @Override
     public BuildStep content(String content) {
        this.content = content;
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
    private CopyOfBuilder(String id, String content) {
      super.id(id);
      super.content(content);
    }
    
    @Override
     public CopyOfBuilder content(String content) {
      return (CopyOfBuilder) super.content(content);
    }
  }
  
}
