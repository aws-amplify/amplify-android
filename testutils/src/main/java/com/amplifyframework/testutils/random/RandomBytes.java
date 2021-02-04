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

package com.amplifyframework.testutils.random;

import java.util.Random;

/**
 * A test utility to build random bytes.
 */
public final class RandomBytes {
    private static final int DEFAULT_BYTE_COUNT = 1024;

    private RandomBytes() {}

    /**
     * Builds an array of random bytes with default array length.
     * @return an array of random bytes
     */
    public static byte[] bytes() {
        return bytes(DEFAULT_BYTE_COUNT);
    }

    /**
     * Builds an array of random bytes.
     * @param count number of bytes
     * @return an array of random bytes
     */
    public static byte[] bytes(int count) {
        byte[] byteArray = new byte[count];
        new Random().nextBytes(byteArray);
        return byteArray;
    }
}
