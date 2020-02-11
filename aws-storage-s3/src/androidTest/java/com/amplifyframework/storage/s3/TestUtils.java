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

package com.amplifyframework.storage.s3;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test utility for comparing file content.
 */
final class TestUtils {

    @SuppressWarnings("WhitespaceAround")
    private TestUtils() {} // Prevent instantiation

    static void assertFileEqualsFile(File expectedFile, File actualFile) {
        assertTrue("Expected file must exist.", expectedFile.exists());
        assertTrue("Testing file must exist.", actualFile.exists());
        assertEquals(expectedFile.length(), actualFile.length());

        try {
            FileInputStream expectedInputStream = new FileInputStream(expectedFile);
            FileInputStream actualInputStream = new FileInputStream(actualFile);
            assertStreamEqualStream(expectedInputStream, actualInputStream);
        } catch (Exception exception) {
            exception.printStackTrace();
            fail("Unable to compare files: " + exception.getMessage());
        }
    }

    static void assertStreamEqualStream(
            InputStream expectedInputStream,
            InputStream actualInputStream
    ) throws IOException {
        final byte[] expectedDigest;
        final byte[] actualDigest;

        try {
            expectedDigest = calculateMD5Digest(expectedInputStream);
            actualDigest = calculateMD5Digest(actualInputStream);

            assertArrayEquals(expectedDigest, actualDigest);
        } catch (NoSuchAlgorithmException exception) {
            exception.printStackTrace();
            fail("Unable to compare input streams: " + exception.getMessage());
        } finally {
            expectedInputStream.close();
            actualInputStream.close();
        }
    }

    @SuppressWarnings("MagicNumber")
    private static byte[] calculateMD5Digest(InputStream stream)
            throws NoSuchAlgorithmException, IOException {
        int bytesRead;
        byte[] buffer = new byte[2048];
        MessageDigest md5 = MessageDigest.getInstance("MD5");

        while ((bytesRead = stream.read(buffer)) != -1) {
            md5.update(buffer, 0, bytesRead);
        }
        return md5.digest();
    }
}
