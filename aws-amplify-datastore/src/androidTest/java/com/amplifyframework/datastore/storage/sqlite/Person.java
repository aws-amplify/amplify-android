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

package com.amplifyframework.datastore.storage.sqlite;

import com.amplifyframework.datastore.annotations.Index;
import com.amplifyframework.datastore.annotations.ModelConfig;
import com.amplifyframework.datastore.annotations.ModelField;
import com.amplifyframework.datastore.model.Model;

import java.util.UUID;

/**
 * An example of a class that would be generated by Amplify Codegen.
 */
@ModelConfig(targetName = "Person")
@Index(fields = {"firstName", "age"}, name = "firstNameBasedIndex")
public final class Person implements Model {

    @ModelField(targetName = "id", targetType = "ID", isRequired = true)
    private final String id;

    @ModelField(targetName = "first_name", targetType = "String", isRequired = true)
    private final String firstName;

    @ModelField(targetName = "last_name", targetType = "String", isRequired = true)
    private final String lastName;

    // Default for isRequired is false
    @ModelField(targetName = "age", targetType = "Int")
    private final int age;

    private Person(String uniqueId,
                   String firstName,
                   String lastName,
                   int age) {
        this.id = uniqueId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.age = age;
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
        FinalStep age(int age);
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
     * Returns an instance of the builder.
     * @return an instance of the builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder implements FirstNameStep, LastNameStep, AgeStep, FinalStep {
        private String firstName;
        private String lastName;
        private int age;

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
        public FinalStep age(int age) {
            this.age = age;
            return this;
        }

        /**
         * Returns the builder object.
         * @return the builder object.
         */
        public Person build() {
            return new Person(UUID.randomUUID().toString(),
                    firstName,
                    lastName,
                    age);
        }
    }
}
