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

public final class StoragePutResult implements Result {
    private String key;

    private StoragePutResult(String key) {
        this.key = key;
    }

    public static StoragePutResult fromKey(String key) {
        return new StoragePutResult(key);
    }

    public String getKey() {
        return key;
    }
}
