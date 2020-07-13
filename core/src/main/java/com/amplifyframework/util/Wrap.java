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

import androidx.annotation.Nullable;

/**
 * A utility to wrap strings.
 */
public final class Wrap {
    private Wrap() {}

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
