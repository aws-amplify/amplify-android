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

import com.amplifyframework.core.async.Result;

import java.io.File;

public class StorageGetResult implements Result {
    /**
     * Downloaded local file
     */
    public File file;

    /**
     * Remote pre-signed URL for downloading file
     */
    public String url;

    public StorageGetResult(File file) {
        this.file = file;
    }

    public StorageGetResult(String url) {
        this.url = url;
    }
}
