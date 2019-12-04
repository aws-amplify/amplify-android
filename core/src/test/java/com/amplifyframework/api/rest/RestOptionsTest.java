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

package com.amplifyframework.api.rest;

import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the class {@link RestOptions}.
 */
public class RestOptionsTest {

    /**
     * Test if we can create a RestOptions object with builder.
     */
    @Test
    public void testInitialize() {
        final RestOptions.Builder builder = RestOptions.builder()
                .addPath("path")
                .addHeader("key1", "value1")
                .addQueryParameters(ImmutableMap.of("query1", "value1"));
        final RestOptions options = builder.build();
        Assert.assertNotNull("Object build should be non null", options);
        Assert.assertEquals("path", options.getPath());
        Assert.assertEquals(ImmutableMap.of("key1", "value1"), options.getHeaders());
        Assert.assertEquals(ImmutableMap.of("query1", "value1"), options.getQueryParameters());
    }

    /**
     * Test whether adding headers just append to the map.
     */
    @Test
    public void testHeaders() {
        final RestOptions.Builder builder = RestOptions.builder()
                .addPath("path")
                .addHeader("key1", "value1");

        builder.addHeader("key2", "value2");
        builder.addHeaders(ImmutableMap.of("key3", "value3"));

        final RestOptions options = builder.build();
        Assert.assertNotNull("Object build should be non null", options);

        Assert.assertEquals(
                ImmutableMap.of(
                        "key1", "value1",
                        "key2", "value2",
                        "key3", "value3"),
                options.getHeaders());

    }
}
