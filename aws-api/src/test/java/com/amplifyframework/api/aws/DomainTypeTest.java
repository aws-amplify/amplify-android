/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.api.aws;

import org.junit.Assert;
import org.junit.Test;

import static com.amplifyframework.api.aws.DomainType.CUSTOM;
import static com.amplifyframework.api.aws.DomainType.STANDARD;

public class DomainTypeTest {
    private static final String STANDARD_URL =
            "https://abcdefghijklmnopqrstuvwxyz.appsync-api.us-west-2.amazonaws.com/graphql";
    private static final String CUSTOM_URL = "https://something.in.somedomain.com/graphql";

    /**
     * Test that Domain type is {@link DomainType#STANDARD} for generated URL.
     */
    @Test
    public void testStandardURLMatch() {
        Assert.assertEquals(STANDARD, DomainType.from(STANDARD_URL));
    }

    /**
     * Test that Domain type is set to {@link DomainType#CUSTOM} for custom URLs.
     */
    @Test
    public void testCustomURLMatch() {
        Assert.assertEquals(CUSTOM, DomainType.from(CUSTOM_URL));
    }
}
