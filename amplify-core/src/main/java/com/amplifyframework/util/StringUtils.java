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

/**
 * Utility for common String operations which are not otherwise available
 */
public class StringUtils {

    /**
     * Dis-allows instantiation of this class
     */
    private StringUtils() { }

    /**
     * Returns original string in all lower case except first character
     * @param original Original string to modify
     * @return Original string but in all lower case except for the first character which is now capitalized
     *          If original string is null or empty, it just returns the original.
     */
    public static String capitalize(String original) {
        if (original == null || original.length() == 0) {
            return original;
        }
        return original.substring(0, 1).toUpperCase() + original.substring(1).toLowerCase();
    }
}
