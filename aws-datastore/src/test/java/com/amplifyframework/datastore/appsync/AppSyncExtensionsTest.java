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

package com.amplifyframework.datastore.appsync;

import com.google.gson.internal.LinkedTreeMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Tests {@link AppSyncExtensions}.
 */
@RunWith(RobolectricTestRunner.class)
public class AppSyncExtensionsTest {

    /**
     * Validates the construction of an AppSyncExtensions object from a Map&lt;String, Object&gt;.
     */
    @Test
    public void validateObjectCreation() {
        String errorType = "conflictUnhandled";
        String errorInfo = null;
        Map<String, Object> data = new LinkedTreeMap<>();
        data.put("id", "EF48518C-92EB-4F7A-A64E-D1B9325205CF");
        data.put("title", "new3");
        data.put("content", "Original content from DataStoreEndToEndTests at 2020-03-26 21:55:47 " +
                "+0000");
        data.put("_version", 2.0);

        AppSyncExtensions expected = new AppSyncExtensions(errorType, errorInfo, data);

        Map<String, Object> extensions = new LinkedTreeMap<>();
        extensions.put("errorType", errorType);
        extensions.put("errorInfo", null);
        extensions.put("data", data);
        AppSyncExtensions actual = new AppSyncExtensions(extensions);

        assertEquals(expected, actual);
    }
}
