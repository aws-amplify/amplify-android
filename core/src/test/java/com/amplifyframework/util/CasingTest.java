/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.util;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

/**
 * Tests the {@link Casing} utility.
 */
public final class CasingTest {
    /**
     * Test conversion from camelCase to PascalCase.
     */
    @Test
    public void camelToPascal() {
        assertEquals("CamelCase", Casing.from(Casing.CaseType.CAMEL_CASE)
            .to(Casing.CaseType.PASCAL_CASE)
            .convert("camelCase"));
    }

    /**
     * Test conversion from camelCase to SCREAMING_SNAKE.
     */
    @Test
    public void camelToScreamingSnake() {
        assertEquals("INPUT_STRING", Casing.from(Casing.CaseType.CAMEL_CASE)
            .to(Casing.CaseType.SCREAMING_SNAKE_CASE)
            .convert("inputString"));
    }

    /**
     * Tests conversion of camelCase to camelCase.
     */
    @Test
    public void camelToCamel() {
        assertEquals("inputString", Casing.from(Casing.CaseType.CAMEL_CASE)
            .to(Casing.CaseType.CAMEL_CASE)
            .convert("inputString"));
    }

    /**
     * Tests conversion from PascalCase to camelCase.
     */
    @Test
    public void pascalToCamel() {
        assertEquals("inputString", Casing.from(Casing.CaseType.PASCAL_CASE)
            .to(Casing.CaseType.CAMEL_CASE)
            .convert("InputString"));
    }

    /**
     * Tests conversion from pascal to pascal.
     */
    @Test
    public void pascalToPascal() {
        assertEquals("InputType", Casing.from(Casing.CaseType.PASCAL_CASE)
            .to(Casing.CaseType.PASCAL_CASE)
            .convert("InputType"));
    }

    /**
     * Tests conversion from PascalCase to SCREAMING_SNAKE.
     */
    @Test
    public void pascalToScreamingSnake() {
        assertEquals("INPUT_TYPE", Casing.from(Casing.CaseType.PASCAL_CASE)
            .to(Casing.CaseType.SCREAMING_SNAKE_CASE)
            .convert("InputType"));
    }

    /**
     * Tests conversion from SCREAMING_SNAKE to PascalCase.
     */
    @Test
    public void screamingSnakeToPascal() {
        assertEquals("InputType", Casing.from(Casing.CaseType.SCREAMING_SNAKE_CASE)
            .to(Casing.CaseType.PASCAL_CASE)
            .convert("INPUT_TYPE"));
    }

    /**
     * Tests conversion from SCREAMING_SNAKE to camelCase.
     */
    @Test
    public void screamingSnakeToCamel() {
        assertEquals("inputType", Casing.from(Casing.CaseType.SCREAMING_SNAKE_CASE)
            .to(Casing.CaseType.CAMEL_CASE)
            .convert("INPUT_TYPE"));

    }

    /**
     * Tests conversion from SCREAMING_SNAKE to SCREAMING_SNAKE.
     */
    @Test
    public void screamingSnakeToScreamingSnake() {
        assertEquals("INPUT_TYPE", Casing.from(Casing.CaseType.SCREAMING_SNAKE_CASE)
            .to(Casing.CaseType.SCREAMING_SNAKE_CASE)
            .convert("INPUT_TYPE"));
    }

    /**
     * {@link Casing#capitalize(String)} will return a string like:
     * "This is a capitalized string," where the first character is upper-case,
     * and all subsequent alphabetic characters are lowercase.
     */
    @Test
    public void capitalizeNormalizesInput() {
        assertEquals(
            "This is a normalized string.",
            Casing.capitalize("tHIS IS a NORmALiZEd strinG.")
        );
    }

    /**
     * {@link Casing#capitalizeFirst(String)} will only effect the first character.
     */
    @Test
    public void capitalizeFirst() {
        assertEquals(
            "This IS A string WITH ITS first letter CAPITALIZED, only.",
            Casing.capitalizeFirst("this IS A string WITH ITS first letter CAPITALIZED, only.")
        );
    }
}
