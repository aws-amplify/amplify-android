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

import com.amplifyframework.core.async.Options;
import com.amplifyframework.storage.StorageAccessLevel;

/**
 * Options to specify attributes of removeListener API invocation
 */
public class StorageRemoveOptions extends Options {
    public StorageAccessLevel accessLevel;
    public Options options;

    /**
     * Attaches storage access level attribute
     *
     * @param accessLevel access level for invoking API
     * @return this options object for chaining other attributes
     */
    public StorageRemoveOptions withAccessLevel(StorageAccessLevel accessLevel) {
        this.accessLevel = accessLevel;
        return this;
    }

    /**
     * Attaches additional options
     *
     * @param options additional options for custom purposes
     * @return this options object for chaining other attributes
     */
    public StorageRemoveOptions withOptions(Options options) {
        this.options = options;
        return this;
    }
}
