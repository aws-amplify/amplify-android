/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.datastore.storage.sqlite.model;

import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.annotations.Index;
import com.amplifyframework.core.model.annotations.ModelConfig;
import com.amplifyframework.core.model.annotations.ModelField;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;

/**
 * An example of a class that would be generated by Amplify Codegen.
 */
@ModelConfig(targetName = "Person")
@Index(fields = {"firstName", "age"}, name = "firstNameBasedIndex")
public final class Person implements Model {

    @ModelField(targetName = "id", targetType = "ID", isRequired = true)
    private String id;

    @ModelField(targetName = "first_name", targetType = "String", isRequired = true)
    private String firstName;

    @ModelField(targetName = "last_name", targetType = "String", isRequired = true)
    private String lastName;

    // Default for isRequired is false
    @ModelField(targetName = "age", targetType = "Int")
    private int age;

    @ModelField(targetName = "dob", targetType = "AWSDate")
    private Date dob;

    /**
     * Default constructor.
     */
    public Person() {
        super();
    }

    private Person(String uniqueId,
                   String firstName,
                   String lastName,
                   int age,
                   Date dob) {
        this.id = uniqueId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.age = age;
        this.dob = dob;
    }

    /**
     * Returns an instance of the builder.
     * @return an instance of the builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns Id.
     * @return Id.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns first name.
     * @return first name.
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Returns last name.
     * @return last name.
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Returns age.
     * @return age.
     */
    public int getAge() {
        return age;
    }

    /**
     * Return the date of birth.
     * @return the date of birth.
     */
    public Date getDob() {
        return dob;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Person)) {
            return false;
        }
        Person person = (Person) obj;
        return getAge() == person.getAge() &&
                getId().equals(person.getId()) &&
                Objects.equals(getFirstName(), person.getFirstName()) &&
                Objects.equals(getLastName(), person.getLastName()) &&
                Objects.equals(getDob(), person.getDob());
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(
                getId(),
                getFirstName(),
                getLastName(),
                getAge(),
                getDob());
    }

    @Override
    public String toString() {
        return "Person{" +
                "id='" + id + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", age=" + age +
                ", dob=" + dob +
                '}';
    }

    /**
     * Interface for first name step.
     */
    public interface FirstNameStep {
        /**
         * Set the first name.
         * @param firstName first name.
         * @return next step.
         */
        LastNameStep firstName(String firstName);
    }

    /**
     * Interface for last name step.
     */
    public interface LastNameStep {
        /**
         * Set the last name.
         * @param lastName last name.
         * @return next step.
         */
        AgeStep lastName(String lastName);
    }

    /**
     * Interface for age step.
     */
    public interface AgeStep {
        /**
         * Set the age.
         * @param age age.
         * @return next step.
         */
        DOBStep age(int age);
    }

    /**
     * Interface for date of birth step.
     */
    public interface DOBStep {
        /**
         * Set the date of birth.
         * @param dob date of birth.
         * @return next step.
         */
        FinalStep dob(Date dob);
    }

    /**
     * Interface for final step.
     */
    public interface FinalStep {
        /**
         * Returns the built Person object.
         * @return the built Person object.
         */
        Person build();
    }

    /**
     * Builder to build the Person object.
     */
    public static final class Builder implements
            FirstNameStep, LastNameStep, AgeStep, DOBStep, FinalStep {
        private String firstName;
        private String lastName;
        private int age;
        private Date dob;

        /**
         * Set the first name and proceed to LastNameStep.
         * @param firstName first name
         * @return next step
         */
        public LastNameStep firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        /**
         * Set the last name and proceed to AgeStep.
         * @param lastName last name
         * @return next step
         */
        public AgeStep lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        /**
         * Set the age and proceed to FinalStep.
         * @param age age
         * @return next step
         */
        public DOBStep age(int age) {
            this.age = age;
            return this;
        }

        /**
         * Set the date of birth.
         *
         * @param dob date of birth.
         * @return next step.
         */
        @Override
        public FinalStep dob(Date dob) {
            this.dob = dob;
            return this;
        }

        /**
         * Returns the builder object.
         * @return the builder object.
         */
        public Person build() {
            return new Person(
                    UUID.randomUUID().toString(),
                    firstName,
                    lastName,
                    age,
                    dob);
        }
    }
}
