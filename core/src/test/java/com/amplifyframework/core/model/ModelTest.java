package com.amplifyframework.core.model;

import androidx.annotation.NonNull;

import org.junit.Test;

import java.io.Serializable;

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
        String testId = "first#last";
        Model blog = new TestModel("first", "last");
        //Act
        String pk = blog.getPrimaryKeyString();
        //Assert
        assertEquals(testId, pk);
    }

    public class TestModel implements Model{
        private final String first;
        private final String last;

        public TestModel(String first, String last){

            this.first = first;
            this.last = last;
        }
        @NonNull
        @Override
        public TestModelPrimaryKey resolveIdentifier(){
            return new TestModelPrimaryKey(first,last);
        }
    }

    public class TestModelPrimaryKey extends ModelPrimaryKey<TestModel>{

        public TestModelPrimaryKey(String first, String last) {
            super(first, last);
        }
    }
}