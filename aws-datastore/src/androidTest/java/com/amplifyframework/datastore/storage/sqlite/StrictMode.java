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

package com.amplifyframework.datastore.storage.sqlite;

/**
 * Utility class to easily manipulate strict mode policies
 * for testing purposes.
 */
final class StrictMode {
    @SuppressWarnings("WhitespaceAround")
    private StrictMode() {}

    /**
     * Enable strict mode for testing SQLite operations.
     */
    static void enable() {
        android.os.StrictMode.setVmPolicy(strictModePolicy());
    }

    private static android.os.StrictMode.VmPolicy strictModePolicy() {
        return new android.os.StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .penaltyDeath()
                .build();
    }
}
