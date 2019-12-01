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

package com.amplifyframework.storage;

/**
 * An enum of permission levels on storage operations.
 * This information should be passed in API options.
 */
public enum StorageAccessLevel {

    /**
     * Storage items are accessible by all users of your app.
     */
    PUBLIC,

    /**
     * Storage items are readable by all users, but writable only by the creating user.
     */
    PROTECTED,

    /**
     * Storage items are accessible for the individual user who performs the write.
     */
    PRIVATE
}
