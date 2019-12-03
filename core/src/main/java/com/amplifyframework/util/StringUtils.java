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

package com.amplifyframework.util;

import android.text.TextUtils;

import java.util.Locale;

/**
 * Utility for common String operations which are not otherwise available.
 */
public final class StringUtils {

    /**
     * Dis-allows instantiation of this class.
     */
    private StringUtils() { }

    /**
     * Takes a string in ALL_CAPS_FORMAT and converts it to camelCaseFormat.
     * @param original Original ALL_CAPS_FORMAT string
     * @return camelCaseFormat formatted version of that string
     */
    public static String allCapsToCamelCase(String original) {
        if (TextUtils.isEmpty(original)) {
            return original;
        }

        String pascalCase = allCapsToPascalCase(original);

        return pascalCase.substring(0, 1).toLowerCase(Locale.getDefault()) +
                pascalCase.substring(1);
    }

    /**
     * Takes a string in ALL_CAPS_FORMAT and converts it to PascalCaseFormat.
     * @param original Original ALL_CAPS_FORMAT string
     * @return PascalCaseFormat formatted version of that string
     */
    public static String allCapsToPascalCase(String original) {
        if (TextUtils.isEmpty(original)) {
            return original;
        }

        String[] parts = original.split("_");
        StringBuilder camelCaseString = new StringBuilder();
        for (String part : parts) {
            camelCaseString.append(capitalize(part));
        }
        return camelCaseString.toString();
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
     * Returns original string wrapped with single quotes.
     * @param original Original string to modify
     * @return Original string wrapped with single quotes.
     *         If original string is null or empty, it just returns the original.
     */
    public static String singleQuote(String original) {
        if (original == null) {
            return null;
        }
        return "'" + original + "'";
    }

    /**
     * Returns original string wrapped with double quotes.
     * @param original Original string to modify
     * @return Original string wrapped with double quotes.
     *         If original string is null or empty, it just returns the original.
     */
    public static String doubleQuote(String original) {
        if (original == null) {
            return null;
        }
        return "\"" + original + "\"";
    }
}
