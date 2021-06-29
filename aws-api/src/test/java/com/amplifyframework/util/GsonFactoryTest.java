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

package com.amplifyframework.util;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.temporal.Temporal;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;

public class GsonFactoryTest {
    /**
     * Validate serializing a {@link Model} to a String and then deserializing it returns the same value.
     */
    @Test
    public void validateGsonFactorySerde() {
        BlogOwner expected = BlogOwner.builder()
                .name("Richard")
                .createdAt(new Temporal.DateTime(new Date(), 0))
                .build();
        String str = GsonFactory.instance().toJson(expected);
        BlogOwner actual = GsonFactory.instance().fromJson(str, BlogOwner.class);
        assertEquals(expected, actual);
    }
}
