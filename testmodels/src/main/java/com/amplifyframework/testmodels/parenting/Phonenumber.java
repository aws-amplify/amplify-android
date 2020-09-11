package com.amplifyframework.testmodels.parenting;


import androidx.core.util.ObjectsCompat;

import java.util.Objects;
import java.util.List;

/** This is an auto generated class representing the Phonenumber type in your schema. */
public final class Phonenumber {
  private final Integer code;
  private final Integer carrier;
  private final Integer number;
  public Integer getCode() {
      return code;
  }
  
  public Integer getCarrier() {
      return carrier;
  }
  
  public Integer getNumber() {
      return number;
  }
  
  private Phonenumber(Integer code, Integer carrier, Integer number) {
    this.code = code;
    this.carrier = carrier;
    this.number = number;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      Phonenumber phonenumber = (Phonenumber) obj;
      return ObjectsCompat.equals(getCode(), phonenumber.getCode()) &&
              ObjectsCompat.equals(getCarrier(), phonenumber.getCarrier()) &&
              ObjectsCompat.equals(getNumber(), phonenumber.getNumber());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getCode())
      .append(getCarrier())
      .append(getNumber())
      .toString()
      .hashCode();
  }
  
  public static CodeStep builder() {
      return new Builder();
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(code,
      carrier,
      number);
  }
  public interface CodeStep {
    CarrierStep code(Integer code);
  }
  

  public interface CarrierStep {
    NumberStep carrier(Integer carrier);
  }
  

  public interface NumberStep {
    BuildStep number(Integer number);
  }
  

  public interface BuildStep {
    Phonenumber build();
  }
  

  public static class Builder implements CodeStep, CarrierStep, NumberStep, BuildStep {
    private Integer code;
    private Integer carrier;
    private Integer number;
    @Override
     public Phonenumber build() {
        
        return new Phonenumber(
          code,
          carrier,
          number);
    }
    
    @Override
     public CarrierStep code(Integer code) {
        Objects.requireNonNull(code);
        this.code = code;
        return this;
    }
    
    @Override
     public NumberStep carrier(Integer carrier) {
        Objects.requireNonNull(carrier);
        this.carrier = carrier;
        return this;
    }
    
    @Override
     public BuildStep number(Integer number) {
        Objects.requireNonNull(number);
        this.number = number;
        return this;
    }
  }
  

  public final class CopyOfBuilder extends Builder {
    private CopyOfBuilder(Integer code, Integer carrier, Integer number) {
      super.code(code)
        .carrier(carrier)
        .number(number);
    }
    
    @Override
     public CopyOfBuilder code(Integer code) {
      return (CopyOfBuilder) super.code(code);
    }
    
    @Override
     public CopyOfBuilder carrier(Integer carrier) {
      return (CopyOfBuilder) super.carrier(carrier);
    }
    
    @Override
     public CopyOfBuilder number(Integer number) {
      return (CopyOfBuilder) super.number(number);
    }
  }
  
}
