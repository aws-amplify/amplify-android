package com.amplifyframework.testmodels.noteswithauth;


import java.util.UUID;
import java.util.Objects;

import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.core.model.annotations.ModelField;
import com.amplifyframework.core.model.query.predicate.QueryField;

import static com.amplifyframework.core.model.query.predicate.QueryField.field;

/** This is an auto generated class representing the PublicNote type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "PublicNotes")
public final class PublicNote implements Model {
  public static final QueryField ID = field("id");
  public static final QueryField CONTENT = field("content");
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="String", isRequired = true) String content;
  public String getId() {
      return id;
  }
  
  public String getContent() {
      return content;
  }
  
  private PublicNote(String id, String content) {
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
      PublicNote publicNote = (PublicNote) obj;
      return ObjectsCompat.equals(getId(), publicNote.getId()) &&
              ObjectsCompat.equals(getContent(), publicNote.getContent());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getId())
      .append(getContent())
      .toString()
      .hashCode();
  }
  
  public static ContentStep builder() {
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
   **/
  public static PublicNote justId(String id) {
    try {
      UUID.fromString(id); // Check that ID is in the UUID format - if not an exception is thrown
    } catch (Exception exception) {
      throw new IllegalArgumentException(
              "Model IDs must be unique in the format of UUID. This method is for creating instances " +
              "of an existing object with only its ID field for sending as a mutation parameter. When " +
              "creating a new object, use the standard builder method and leave the ID field blank."
      );
    }
    return new PublicNote(
      id,
      null
    );
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(id,
      content);
  }
  public interface ContentStep {
    BuildStep content(String content);
  }
  

  public interface BuildStep {
    PublicNote build();
    BuildStep id(String id) throws IllegalArgumentException;
  }
  

  public static class Builder implements ContentStep, BuildStep {
    private String id;
    private String content;
    @Override
     public PublicNote build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new PublicNote(
          id,
          content);
    }
    
    @Override
     public BuildStep content(String content) {
        Objects.requireNonNull(content);
        this.content = content;
        return this;
    }
    
    /** 
     * WARNING: Do not set ID when creating a new object. Leave this blank and one will be auto generated for you.
     * This should only be set when referring to an already existing object.
     * @param id id
     * @return Current Builder instance, for fluent method chaining
     * @throws IllegalArgumentException Checks that ID is in the proper format
     **/
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
