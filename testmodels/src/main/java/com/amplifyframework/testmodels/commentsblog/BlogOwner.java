package com.amplifyframework.testmodels.commentsblog;

import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.annotations.HasOne;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.core.model.annotations.ModelField;
import com.amplifyframework.core.model.query.predicate.QueryField;

import java.util.Objects;
import java.util.UUID;

import static com.amplifyframework.core.model.query.predicate.QueryField.field;

/** This is an auto generated class representing the BlogOwner type in your schema. */
@SuppressWarnings("all")
@ModelConfig(pluralName = "BlogOwners")
public final class BlogOwner implements Model {
  public static final QueryField NAME = field("name");
  public static final QueryField ID = field("id");
  public static final QueryField WEA = field("wea");
  private final @ModelField(targetType="String", isRequired = true) String name;
  private final @ModelField(targetType="ID", isRequired = true) String id;
  private final @ModelField(targetType="Blog") @HasOne(associatedWith = "owner", type = Blog.class) Blog blog = null;
  private final @ModelField(targetType="String") String wea;
  public String getName() {
      return name;
  }
  
  public String getId() {
      return id;
  }
  
  public Blog getBlog() {
      return blog;
  }
  
  public String getWea() {
      return wea;
  }
  
  private BlogOwner(String name, String id, String wea) {
    this.name = name;
    this.id = id;
    this.wea = wea;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      BlogOwner blogOwner = (BlogOwner) obj;
      return ObjectsCompat.equals(getName(), blogOwner.getName()) &&
              ObjectsCompat.equals(getId(), blogOwner.getId()) &&
              ObjectsCompat.equals(getWea(), blogOwner.getWea());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getName())
      .append(getId())
      .append(getWea())
      .toString()
      .hashCode();
  }

    @Override
    public String toString() {
        return "BlogOwner{" +
                "name='" + name + '\'' +
                ", id='" + id + '\'' +
                ", blog=" + blog +
                ", wea='" + wea + '\'' +
                '}';
    }

    public static NameStep builder() {
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
  public static BlogOwner justId(String id) {
    try {
      UUID.fromString(id); // Check that ID is in the UUID format - if not an exception is thrown
    } catch (Exception exception) {
      throw new IllegalArgumentException(
              "Model IDs must be unique in the format of UUID. This method is for creating instances " +
              "of an existing object with only its ID field for sending as a mutation parameter. When " +
              "creating a new object, use the standard builder method and leave the ID field blank."
      );
    }
    return new BlogOwner(
      id,
      null,
      null
    );
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(name,
      id,
      wea);
  }
  public interface NameStep {
    BuildStep name(String name);
  }
  

  public interface BuildStep {
    BlogOwner build();
    BuildStep id(String id) throws IllegalArgumentException;
    BuildStep wea(String wea);
  }
  

  public static class Builder implements NameStep, BuildStep {
    private String name;
    private String id;
    private String wea;
    @Override
     public BlogOwner build() {
        String id = this.id != null ? this.id : UUID.randomUUID().toString();
        
        return new BlogOwner(
          name,
          id,
          wea);
    }
    
    @Override
     public BuildStep name(String name) {
        Objects.requireNonNull(name);
        this.name = name;
        return this;
    }
    
    @Override
     public BuildStep wea(String wea) {
        this.wea = wea;
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
    private CopyOfBuilder(String name, String id, String wea) {
      super.id(id);
      super.name(name)
        .wea(wea);
    }
    
    @Override
     public CopyOfBuilder name(String name) {
      return (CopyOfBuilder) super.name(name);
    }
    
    @Override
     public CopyOfBuilder wea(String wea) {
      return (CopyOfBuilder) super.wea(wea);
    }
  }
  
}
