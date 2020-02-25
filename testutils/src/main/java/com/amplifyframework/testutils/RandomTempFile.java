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

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.UUID;

/**
 * Utility class to create a temporary file that can be quickly populated with
 * random data to desired size. It is faster than using RandomInputStream, but
 * it isn't thread-safe so approach with caution.
 */
public final class RandomTempFile extends File {

    // java.io.File implements Serializable
    private static final long serialVersionUID = 8262425156855108590L;

    // JRE decides where the temp files will be created
    private static final String TEMP_DIR_PROPERTY = "java.io.tmpdir";
    private static final String TEMP_DIR = System.getProperty(TEMP_DIR_PROPERTY);

    /**
     * Creates a temporary file with random name that follows UUID format with
     * the specified file size. The file is sparsely populated with random data.
     * @param byteSize The size of newly created temporary file
     * @throws IOException If a problem is encountered while creating the file
     */
    public RandomTempFile(long byteSize) throws IOException {
        this(UUID.randomUUID().toString(), byteSize);
    }

    /**
     * Creates an empty temp file.
     *
     * @param filename The name of temporary file
     * @throws IOException If a problem is encountered while creating the file
     */
    public RandomTempFile(@NonNull String filename) throws IOException {
        this(filename, 0L);
    }

    /**
     * Creates a temp file with the specified name and sparsely populates it
     * with the specified amount of random data.
     *
     * @param filename The name of temporary file
     * @param byteSize The size of newly created temporary file
     * @throws IOException If a problem is encountered while creating the file
     */
    public RandomTempFile(@NonNull String filename, long byteSize) throws IOException {
        super(TEMP_DIR + File.separator + filename);
        createFile(byteSize);
    }

    private void createFile(long byteSize) throws IOException {
        deleteOnExit();
        RandomAccessFile raf = new RandomAccessFile(this, "rw");
        raf.setLength(byteSize);
    }
}
