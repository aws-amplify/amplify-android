/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.core.model;

import androidx.annotation.NonNull;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ModelTest {
    /**
     * Tests that correct primary key is returned when Primary key is default String.
     */
    @Test
    public void testGetPrimaryKeyStringReturnsStringKeyWhenDefaultPK() {
        // Arrange
        String testId = "TestId";
        Model blog = new Model() {
            @NonNull
            @Override
            public String resolveIdentifier() {
                return testId;
            }
        };
        //Act
        String pk = blog.getPrimaryKeyString();
        //Assert
        assertEquals(testId, pk);
    }

    /**
     * Tests that correct primary key is returned when Custom Primary Key.
     */
    @Test
    public void testGetPrimaryKeyStringReturnsCustomPrimarygKeyWhenCustomPrimaryKey() {
        // Arrange
        String testId = "\"first\"\"is\"#\"last\"";
        Model blog = new TestModel("first\"is", "last");
        //Act
        String pk = blog.getPrimaryKeyString();
        //Assert
        assertEquals(testId, pk);
    }

    public class TestModel implements Model {
        private final String first;
        private final String last;

        /**
         * Test model.
         * @param first First name.
         * @param last Last name.
         */
        public TestModel(String first, String last) {
            this.first = first;
            this.last = last;
        }

        /**
         * {@inheritDoc}
         */
        @NonNull
        @Override
        public TestModelPrimaryKey resolveIdentifier() {
            return new TestModelPrimaryKey(first, last);
        }
    }

    public class TestModelPrimaryKey extends ModelPrimaryKey<TestModel> {

        /**
         * Test model primary key.
         * @param first partition key.
         * @param last sort key.
         */
        public TestModelPrimaryKey(String first, String last) {
            super(first, last);
        }
    }
}
