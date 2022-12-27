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

package com.amplifyframework.testutils;

import androidx.annotation.Nullable;

import org.junit.Assert;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Test utility for comparing file content.
 */
public final class FileAssert {
    private FileAssert() {}

    /**
     * Asserts that two files are equal in the respect that:
     *  1. Both are non-null, or both are null, bot not a mix;
     *  2. Both exist, or do not exist, but not a mix;
     *  3. If both are non-null and exist, then they must have the same content.
     * @param expectedFile A baseline to compare against
     * @param actualFile A file which may or may not be the same as the expected file
     */
    public static void assertEquals(@Nullable File expectedFile, @Nullable File actualFile) {
        // Ensure that the files are either both null, or are both non-null.
        if (expectedFile == null) {
            Assert.assertNull("Expected file was null, but actual file was non-null.", actualFile);
            return; // Linting doesn't catch logic on Assert, just if/else
        } else if (actualFile == null) {
            // Expected was not null, and this is null. So it fails.
            Assert.fail("Expected file was non-null, but actual file was null.");
            return; // Linting doesn't catch logic on Assert, just if/else
        }
        // else, neither are null ...

        // Alright, both are non-null. Cool. Do they have the same existence?
        Assert.assertEquals(expectedFile.exists(), actualFile.exists());
        // Both exist, are they the same length?
        Assert.assertEquals(expectedFile.length(), actualFile.length());

        try {
            FileInputStream expectedInputStream = new FileInputStream(expectedFile);
            FileInputStream actualInputStream = new FileInputStream(actualFile);
            assertStreamEqualStream(expectedInputStream, actualInputStream);
        } catch (IOException errorOpeningFileStream) {
            throw new RuntimeException("Failed to open file stream while comparing content.", errorOpeningFileStream);
        }
    }

    private static void assertStreamEqualStream(
            InputStream expectedInputStream,
            InputStream actualInputStream
    ) throws IOException {
        final byte[] expectedDigest;
        final byte[] actualDigest;
        try {
            expectedDigest = calculateDigest(expectedInputStream);
            actualDigest = calculateDigest(actualInputStream);
            Assert.assertArrayEquals(expectedDigest, actualDigest);
        } finally {
            expectedInputStream.close();
            actualInputStream.close();
        }
    }

    @SuppressWarnings("MagicNumber") // Buffer size
    private static byte[] calculateDigest(InputStream stream) {
        final byte[] buffer = new byte[2_048];
        final MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException noMd5AvailableError) {
            throw new RuntimeException("No MD5 algorithm available to use for hashing.", noMd5AvailableError);
        }

        int bytesRead;
        try {
            while ((bytesRead = stream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        } catch (IOException readError) {
            throw new RuntimeException("Failed to read input stream.", readError);
        }

        return digest.digest();
    }
}
