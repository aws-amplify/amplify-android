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

package com.amplifyframework.storage.options;

import com.amplifyframework.core.task.Options;
import com.amplifyframework.storage.StorageAccessLevel;

import java.util.Map;

/**
 * Options to specify attributes of put API invocation
 */
public class StoragePutOptions extends Options {
    public StorageAccessLevel accessLevel;
    public String contentType;
    public Map<String, String> metadata;
    public Options options;

    /**
     * Attaches storage access level attribute
     *
     * @param accessLevel access level for invoking API
     * @return this options object for chaining other attributes
     */
    public StoragePutOptions withAccessLevel(StorageAccessLevel accessLevel) {
        this.accessLevel = accessLevel;
        return this;
    }

    /**
     * Attaches content type of object
     *
     * @param contentType content type of object being stored
     * @return this options object for chaining other attributes
     */
    public StoragePutOptions withContentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    /**
     * Attaches metadata to object being stored
     *
     * @param metadata map of metadata being attached to stored object
     * @return this options object for chaining other attributes
     */
    public StoragePutOptions withMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
        return this;
    }

    /**
     * Attaches additional options
     *
     * @param options additional options for custom purposes
     * @return this options object for chaining other attributes
     */
    public StoragePutOptions withOptions(Options options) {
        this.options = options;
        return this;
    }
}
