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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;
import java.util.Objects;

/**
 * A utility to affect the casing of strings.
 */
public final class Casing {
    private Casing() {}

    /**
     * Select a case type to begin converting a string's casing.
     * @param caseType The type of casing the input string is in.
     * @return A next step, where you can specify the target casing
     */
    @NonNull
    public static CasingSource from(@NonNull CaseType caseType) {
        Objects.requireNonNull(caseType);
        return new CasingSource(caseType);
    }

    /**
     * Converts a string from PascalCase, into a target casing.
     * @param pascalString String to convert, expected in PascalCase
     * @param targetCasing Casing to convert to
     * @return The input string, in the target casing
     */
    @Nullable
    private static String fromPascalCase(@Nullable String pascalString, @NonNull CaseType targetCasing) {
        Objects.requireNonNull(targetCasing);
        if (pascalString == null) {
            return null;
        }
        switch (targetCasing) {
            case SCREAMING_SNAKE_CASE:
                return toScreamingSnake(pascalString);
            case CAMEL_CASE:
                return pascalToCamel(pascalString);
            case PASCAL_CASE:
                return pascalString;
            default:
                throw new IllegalStateException("No such casing = " + targetCasing);
        }
    }

    /**
     * Converts a string from camelCase, into a target casing.
     * @param camelString String to convert, expected in camelCase
     * @param targetCasing Casing to convert to
     * @return The input string, in the target casing
     */
    @Nullable
    private static String fromCamelCase(@Nullable String camelString, @NonNull CaseType targetCasing) {
        Objects.requireNonNull(targetCasing);
        if (camelString == null) {
            return null;
        }
        switch (targetCasing) {
            case SCREAMING_SNAKE_CASE:
                return toScreamingSnake(camelString);
            case CAMEL_CASE:
                return camelString;
            case PASCAL_CASE:
                return camelToPascal(camelString);
            default:
                throw new IllegalStateException("No such casing = " + targetCasing);
        }
    }

    /**
     * Converts a string into SCREAMING_SNAKE_CASE.
     * @param string Any String, maybe null, maybe blank.
     * @return string, but converted to SCREAMING_SNAKE_CASE
     */
    private static String toScreamingSnake(String string) {
        if (string == null || string.length() == 0) {
            return string;
        }
        StringBuilder builder = new StringBuilder();
        final char[] chars = string.toCharArray();
        for (int index = 0; index < chars.length; index++) {
            char character = chars[index];
            if (Character.isLetterOrDigit(character)) {
                if (Character.isLowerCase(character)) {
                    builder.append(Character.toUpperCase(character));
                } else if (index != 0) {
                    builder.append("_").append(character);
                } else {
                    builder.append(character);
                }
            } else {
                builder.append("_");
            }
        }
        return builder.toString();
    }

    /**
     * Converts a string from SCREAMING_SNAKE_CASE, into a target casing.
     * @param screamingSnakeString String to convert, expected in SCREAMING_SNAKE_CASE
     * @param targetCasing Casing to convert to
     * @return The input string, in the target casing
     */
    @Nullable
    private static String fromScreamingSnakeCase(@Nullable String screamingSnakeString, CaseType targetCasing) {
        Objects.requireNonNull(targetCasing);
        if (screamingSnakeString == null || screamingSnakeString.length() == 0) {
            return screamingSnakeString;
        }
        switch (targetCasing) {
            case PASCAL_CASE:
                return screamingSnakeToPascal(screamingSnakeString);
            case CAMEL_CASE:
                return screamingSnakeToCamel(screamingSnakeString);
            case SCREAMING_SNAKE_CASE:
                return screamingSnakeString;
            default:
                throw new IllegalStateException("Unknown target casing = " + targetCasing);
        }
    }

    @Nullable
    private static String screamingSnakeToCamel(@Nullable String screamingSnake) {
        if (screamingSnake == null || screamingSnake.length() == 0) {
            return screamingSnake;
        }

        String pascalCase = screamingSnakeToPascal(screamingSnake);
        return pascalToCamel(pascalCase);
    }

    @Nullable
    private static String pascalToCamel(@Nullable String pascal) {
        if (pascal == null || pascal.length() == 0) {
            return pascal;
        }
        return pascal.substring(0, 1).toLowerCase(Locale.getDefault()) +
            pascal.substring(1);
    }

    @Nullable
    private static String camelToPascal(@Nullable String camel) {
        if (camel == null || camel.length() == 0) {
            return camel;
        }
        return camel.substring(0, 1).toUpperCase(Locale.getDefault()) +
            camel.substring(1);
    }

    @Nullable
    private static String screamingSnakeToPascal(@Nullable String screamingSnake) {
        if (screamingSnake == null || screamingSnake.length() == 0) {
            return screamingSnake;
        }

        String[] parts = screamingSnake.split("_");
        StringBuilder pascalCaseString = new StringBuilder();
        for (String part : parts) {
            pascalCaseString.append(capitalize(part));
        }
        return pascalCaseString.toString();
    }

    /**
     * Returns original string in all lower case except first character.
     * @param original Original string to modify
     * @return Original string but in all lower case except for the first character which is now capitalized
     *          If original string is null or empty, it just returns the original.
     */
    public static String capitalize(String original) {
        if (original == null || original.length() == 0) {
            return original;
        }

        return original.substring(0, 1).toUpperCase(Locale.getDefault()) +
                original.substring(1).toLowerCase(Locale.getDefault());
    }

    /**
     * Returns original string with first character capitalized and remaining string left unchanged.
     * @param original Original string to modify
     * @return Original string but with first character capitalized (if it already was, String is unchanged)
     */
    public static String capitalizeFirst(String original) {
        if (original == null || original.length() == 0) {
            return original;
        }

        return original.substring(0, 1).toUpperCase(Locale.getDefault()) + original.substring(1);
    }

    /**
     * There are three conventional cases in Java.
     */
    public enum CaseType {
        /**
         * "SCREAMING_SNAKE_CASE" is frequently used by Java constants.
         */
        SCREAMING_SNAKE_CASE,

        /**
         * "camelCase" is frequently used by Java variables.
         */
        CAMEL_CASE,

        /**
         * "PascalCase" is like camel, but starts with an upper-case letter.
         * This is used in Java for class/interface/enum names.
         */
        PASCAL_CASE
    }

    /**
     * A step where you can specify the casing of an input string.
     */
    public static final class CasingSource {
        private final CaseType sourceCasing;

        private CasingSource(CaseType sourceCasing) {
            this.sourceCasing = sourceCasing;
        }

        /**
         * Selects the casing to which to convert.
         * @param targetCasing String will be converted to this case.
         * @return A step where you can perform a string conversion.
         */
        public CasingTarget to(CaseType targetCasing) {
            return new CasingTarget(sourceCasing, targetCasing);
        }
    }

    /**
     * A step where you can perform a case conversion.
     */
    public static final class CasingTarget {
        private CaseType sourceCasing;
        private CaseType targetCasing;

        private CasingTarget(CaseType sourceCasing, CaseType targetCasing) {
            this.sourceCasing = sourceCasing;
            this.targetCasing = targetCasing;
        }

        /**
         * Converts the casing of an input text, using the configured source and target
         * casings.
         * @param input Input text, assumed to be in the source casing
         * @return Output text, with its casing in the target casing.
         */
        public String convert(String input) {
            if (input == null || input.length() == 0) {
                return input;
            }

            switch (sourceCasing) {
                case CAMEL_CASE:
                    return Casing.fromCamelCase(input, targetCasing);
                case PASCAL_CASE:
                    return Casing.fromPascalCase(input, targetCasing);
                case SCREAMING_SNAKE_CASE:
                    return Casing.fromScreamingSnakeCase(input, targetCasing);
                default:
                    throw new IllegalStateException("Unknown source casing = " + sourceCasing);
            }
        }
    }
}
