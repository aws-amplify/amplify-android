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

package com.amplifyframework.storage.result;

import androidx.annotation.NonNull;

import com.amplifyframework.core.async.Result;

import java.net.URL;
import java.util.Objects;

/**
 * The result of a call to get an item's URL from the Storage category.
 */
public final class StorageGetUrlResult implements Result {
    private final URL url;

    private StorageGetUrlResult(@NonNull URL url) {
        this.url = url;
    }

    /**
     * Creates a new StorageGetUrlResult containing the pre-signed URL
     * of requested object.
     * @param url The pre-signed URL
     * @return A StorageGetUrlResult
     */
    @NonNull
    public static StorageGetUrlResult fromUrl(@NonNull URL url) {
        return new StorageGetUrlResult(Objects.requireNonNull(url));
    }

    /**
     * Gets the pre-signed URL.
     * @return pre-signed URL
     */
    @NonNull
    public URL getUrl() {
        return url;
    }
}

