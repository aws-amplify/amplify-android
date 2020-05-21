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

package com.amplifyframework.datastore.syncengine;

/**
 * The mode of operation of the Sync Engine.
 */
public enum SyncMode {
    /**
     * The sync engine is operating offline, right now. This is either because there
     * is no remote system configured at the moment, or because there is a loss of
     * connectivity.
     */
    OFFLINE_ONLY,

    /**
     * The sync engine is online, and is transacting data to and from a remote system.
     */
    SYNC_VIA_API
}
