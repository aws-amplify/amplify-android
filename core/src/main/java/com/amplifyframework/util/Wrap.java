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

/**
 * A utility to wrap strings.
 */
public final class Wrap {
    private Wrap() {}

    /**
     * Returns original string wrapped with in backticks.
     * @param original Original string to modify.
     * @return Original string wrapped with backtick.
     *         If original string is null or empty, it just returns the original.
     */
    @Nullable
    public static String inBackticks(@Nullable String original) {
        if (Empty.check(original)) {
            return original;
        }
        return "`" + original + "`";
    }

    /**
     * Returns original string wrapped with single quotes.
     * @param original Original string to modify
     * @return Original string wrapped with single quotes.
     *         If original string is null or empty, it just returns the original.
     */
    @Nullable
    public static String inSingleQuotes(@Nullable String original) {
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
    @SuppressWarnings("WeakerAccess")
    @Nullable
    public static String inDoubleQuotes(@Nullable String original) {
        if (original == null) {
            return null;
        }
        return "\"" + original + "\"";
    }

    /**
     * Returns original string wrapped with braces.
     * @param original Original string to modify
     * @return Original string wrapped with braces.
     *         If original string is null or empty, it just returns the original.
     */
    @Nullable
    public static String inBraces(@Nullable String original) {
        if (original == null) {
            return null;
        }
        return "{" + original + "}";
    }

    /**
     * Returns original string wrapped with new lines and braces, and appropriate margin.
     *
     * Returned value is composed as:
     *  &lt;space&gt;{
     *  &lt;margin&gt;&lt;indent&gt;&lt;original&gt;
     *  &lt;margin&gt;}
     *
     * @param original Original string to modify
     * @param margin String representing the current margin to indent.
     * @param indent String representing how much to indent original in addition to margin.
     * @return Original string wrapped in new lines, then braces, and appropriate indents.
     */
    public static String inPrettyBraces(@Nullable String original, @NonNull String margin, @NonNull String indent) {
        if (original == null) {
            return null;
        }
        return " " + Wrap.inBraces("\n" + margin + indent + original + "\n" + margin);
    }

    /**
     * Returns original string wrapped with parentheses.
     * @param original Original string to modify
     * @return Original string wrapped with parentheses.
     *         If original string is null or empty, it just returns the original.
     */
    @Nullable
    public static String inParentheses(@Nullable String original) {
        if (original == null) {
            return null;
        }
        return "(" + original + ")";
    }
}
