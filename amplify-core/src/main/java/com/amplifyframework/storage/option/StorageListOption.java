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

public class StorageListOption extends Options {
    public StorageAccessLevel accessLevel;
    public String targetIdentityId;
    public String path;
    public Options options;

    public StorageListOption(StorageAccessLevel accessLevel,
                             String targetIdentityId,
                             String path,
                             Options options) {
        this.accessLevel = accessLevel;
        this.targetIdentityId = targetIdentityId;
        this.path = path;
        this.options = options;
    }
}
