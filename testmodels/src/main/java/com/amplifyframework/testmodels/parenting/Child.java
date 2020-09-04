package com.amplifyframework.testmodels.parenting;


import androidx.core.util.ObjectsCompat;

import java.util.Objects;
import java.util.List;

/** This is an auto generated class representing the Child type in your schema. */
public final class Child {
  private final String name;
  private final Address address;
  public String getName() {
      return name;
  }
  
  public Address getAddress() {
      return address;
  }
  
  private Child(String name, Address address) {
    this.name = name;
    this.address = address;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      Child child = (Child) obj;
      return ObjectsCompat.equals(getName(), child.getName()) &&
              ObjectsCompat.equals(getAddress(), child.getAddress());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getName())
      .append(getAddress())
      .toString()
      .hashCode();
  }
  
  public static NameStep builder() {
      return new Builder();
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(name,
      address);
  }
  public interface NameStep {
    AddressStep name(String name);
  }
  

  public interface AddressStep {
    BuildStep address(Address address);
  }
  

  public interface BuildStep {
    Child build();
  }
  

  public static class Builder implements NameStep, AddressStep, BuildStep {
    private String name;
    private Address address;
    @Override
     public Child build() {
        
        return new Child(
          name,
          address);
    }
    
    @Override
     public AddressStep name(String name) {
        Objects.requireNonNull(name);
        this.name = name;
        return this;
    }
    
    @Override
     public BuildStep address(Address address) {
        Objects.requireNonNull(address);
        this.address = address;
        return this;
    }
  }
  

  public final class CopyOfBuilder extends Builder {
    private CopyOfBuilder(String name, Address address) {
      super.name(name)
        .address(address);
    }
    
    @Override
     public CopyOfBuilder name(String name) {
      return (CopyOfBuilder) super.name(name);
    }
    
    @Override
     public CopyOfBuilder address(Address address) {
      return (CopyOfBuilder) super.address(address);
    }
  }
  
}
