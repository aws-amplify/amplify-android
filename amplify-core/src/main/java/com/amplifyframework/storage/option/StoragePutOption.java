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

package com.amplifyframework.storage.option;

import com.amplifyframework.core.task.Options;
import com.amplifyframework.storage.StorageAccessLevel;

import java.util.HashMap;

public class StoragePutOption extends Options {
    public StorageAccessLevel accessLevel;
    public String contentType;
    public HashMap<String, String> metadata;
    public Options options;

    public StoragePutOption(StorageAccessLevel accessLevel,
                            String contentType,
                            HashMap<String, String> metadata,
                            Options options) {
        this.accessLevel = accessLevel;
        this.contentType = contentType;
        this.metadata = metadata;
        this.options = options;
    }
}
