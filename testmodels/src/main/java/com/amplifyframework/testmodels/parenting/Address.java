package com.amplifyframework.testmodels.parenting;


import androidx.core.util.ObjectsCompat;

import java.util.Objects;
import java.util.List;

/** This is an auto generated class representing the Address type in your schema. */
public final class Address {
  private final String street;
  private final String street2;
  private final City city;
  private final Phonenumber phonenumber;
  private final String country;
  public String getStreet() {
      return street;
  }
  
  public String getStreet2() {
      return street2;
  }
  
  public City getCity() {
      return city;
  }
  
  public Phonenumber getPhonenumber() {
      return phonenumber;
  }
  
  public String getCountry() {
      return country;
  }
  
  private Address(String street, String street2, City city, Phonenumber phonenumber, String country) {
    this.street = street;
    this.street2 = street2;
    this.city = city;
    this.phonenumber = phonenumber;
    this.country = country;
  }
  
  @Override
   public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      } else if(obj == null || getClass() != obj.getClass()) {
        return false;
      } else {
      Address address = (Address) obj;
      return ObjectsCompat.equals(getStreet(), address.getStreet()) &&
              ObjectsCompat.equals(getStreet2(), address.getStreet2()) &&
              ObjectsCompat.equals(getCity(), address.getCity()) &&
              ObjectsCompat.equals(getPhonenumber(), address.getPhonenumber()) &&
              ObjectsCompat.equals(getCountry(), address.getCountry());
      }
  }
  
  @Override
   public int hashCode() {
    return new StringBuilder()
      .append(getStreet())
      .append(getStreet2())
      .append(getCity())
      .append(getPhonenumber())
      .append(getCountry())
      .toString()
      .hashCode();
  }
  
  public static StreetStep builder() {
      return new Builder();
  }
  
  public CopyOfBuilder copyOfBuilder() {
    return new CopyOfBuilder(street,
      street2,
      city,
      phonenumber,
      country);
  }
  public interface StreetStep {
    Street2Step street(String street);
  }
  

  public interface Street2Step {
    CityStep street2(String street2);
  }
  

  public interface CityStep {
    PhonenumberStep city(City city);
  }
  

  public interface PhonenumberStep {
    CountryStep phonenumber(Phonenumber phonenumber);
  }
  

  public interface CountryStep {
    BuildStep country(String country);
  }
  

  public interface BuildStep {
    Address build();
  }
  

  public static class Builder implements StreetStep, Street2Step, CityStep, PhonenumberStep, CountryStep, BuildStep {
    private String street;
    private String street2;
    private City city;
    private Phonenumber phonenumber;
    private String country;
    @Override
     public Address build() {
        
        return new Address(
          street,
          street2,
          city,
          phonenumber,
          country);
    }
    
    @Override
     public Street2Step street(String street) {
        Objects.requireNonNull(street);
        this.street = street;
        return this;
    }
    
    @Override
     public CityStep street2(String street2) {
        Objects.requireNonNull(street2);
        this.street2 = street2;
        return this;
    }
    
    @Override
     public PhonenumberStep city(City city) {
        Objects.requireNonNull(city);
        this.city = city;
        return this;
    }
    
    @Override
     public CountryStep phonenumber(Phonenumber phonenumber) {
        Objects.requireNonNull(phonenumber);
        this.phonenumber = phonenumber;
        return this;
    }
    
    @Override
     public BuildStep country(String country) {
        Objects.requireNonNull(country);
        this.country = country;
        return this;
    }
  }
  

  public final class CopyOfBuilder extends Builder {
    private CopyOfBuilder(String street, String street2, City city, Phonenumber phonenumber, String country) {
      super.street(street)
        .street2(street2)
        .city(city)
        .phonenumber(phonenumber)
        .country(country);
    }
    
    @Override
     public CopyOfBuilder street(String street) {
      return (CopyOfBuilder) super.street(street);
    }
    
    @Override
     public CopyOfBuilder street2(String street2) {
      return (CopyOfBuilder) super.street2(street2);
    }
    
    @Override
     public CopyOfBuilder city(City city) {
      return (CopyOfBuilder) super.city(city);
    }
    
    @Override
     public CopyOfBuilder phonenumber(Phonenumber phonenumber) {
      return (CopyOfBuilder) super.phonenumber(phonenumber);
    }
    
    @Override
     public CopyOfBuilder country(String country) {
      return (CopyOfBuilder) super.country(country);
    }
  }
  
}
