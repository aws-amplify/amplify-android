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

package com.amplifyframework.storage.s3.utils;

import com.amplifyframework.storage.StorageAccessLevel;
import com.amplifyframework.testutils.random.RandomString;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests that S3RequestUtils behaves as expected.
 */
public final class S3RequestUtilsTest {
    /**
     * Tests that a public service key is constructed as expected.
     */
    @Test
    public void testPublicServiceKey() {
        StorageAccessLevel accessLevel = StorageAccessLevel.PUBLIC;
        String identityId = RandomString.string(); // should be ignored for public
        String key = RandomString.string();
        final String expected = "public/" + key;

        assertEquals(expected, S3RequestUtils.getServiceKey(accessLevel, identityId, key));
    }

    /**
     * Tests that a protected service key is constructed as expected.
     */
    @Test
    public void testProtectedServiceKey() {
        StorageAccessLevel accessLevel = StorageAccessLevel.PROTECTED;
        String identityId = RandomString.string();
        String key = RandomString.string();
        final String expected = "protected/" + identityId + "/" + key;

        assertEquals(expected, S3RequestUtils.getServiceKey(accessLevel, identityId, key));
    }

    /**
     * Tests that a private service key is constructed as expected.
     */
    @Test
    public void testPrivateServiceKey() {
        StorageAccessLevel accessLevel = StorageAccessLevel.PRIVATE;
        String identityId = RandomString.string();
        String key = RandomString.string();
        final String expected = "private/" + identityId + "/" + key;

        assertEquals(expected, S3RequestUtils.getServiceKey(accessLevel, identityId, key));
    }

    /**
     * Tests that a public service key can convert to amplify key as expected.
     */
    @Test
    public void testPublicServiceToAmplifyKey() {
        final String serviceKey = "public/foo/bar";
        assertEquals("foo/bar", S3RequestUtils.getAmplifyKey(serviceKey));
    }

    /**
     * Tests that a protected service key can convert to amplify key as expected.
     */
    @Test
    public void testProtectedServiceToAmplifyKey() {
        final String serviceKey = "protected/foo/bar";
        assertEquals("bar", S3RequestUtils.getAmplifyKey(serviceKey));
    }

    /**
     * Tests that a private service key can convert to amplify key as expected.
     */
    @Test
    public void testPrivateServiceToAmplifyKey() {
        final String serviceKey = "private/foo/bar";
        assertEquals("bar", S3RequestUtils.getAmplifyKey(serviceKey));
    }

    /**
     * Tests that non-existent accessor throws.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidAccessorServiceToAmplifyKey() {
        final String serviceKey = "master/foo/bar";
        S3RequestUtils.getAmplifyKey(serviceKey);
    }

    /**
     * Tests that only having the service prefix throws.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidFormatServiceToAmplifyKey() {
        final String serviceKey = "private/foo";
        S3RequestUtils.getAmplifyKey(serviceKey);
    }
}
