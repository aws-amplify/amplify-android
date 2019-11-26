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

package com.amplifyframework.storage.result;

import androidx.annotation.NonNull;

import com.amplifyframework.core.async.Result;

import java.io.File;
import java.util.Objects;

/**
 * The result of a call to get an item from the Storage category.
 */
public final class StorageDownloadFileResult implements Result {
    private final File file;

    private StorageDownloadFileResult(File file) {
        this.file = file;
    }

    /**
     * Gets the file.
     * @return file
     */
    @NonNull
    public File getFile() {
        return file;
    }

    /**
     * Creates a new StorageDownloadFileResult containing a reference to
     * the downloaded file.
     * @param file The downloaded file
     * @return A StorageDownloadFileResult
     */
    @NonNull
    public static StorageDownloadFileResult fromFile(@NonNull File file) {
        return new StorageDownloadFileResult(Objects.requireNonNull(file));
    }
}

