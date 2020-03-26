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
 * Tests the {@link S3Keys} utility.
 */
public final class S3KeysTest {
    /**
     * A public service key should just be the amplify key, prefixed by the path "public/".
     */
    @Test
    public void createdPublicServiceKey() {
        StorageAccessLevel accessLevel = StorageAccessLevel.PUBLIC;
        String identityId = RandomString.string(); // should be ignored for public
        String key = RandomString.string();
        final String expected = "public/" + key;

        assertEquals(expected, S3Keys.createServiceKey(accessLevel, identityId, key));
    }

    /**
     * Validates construction of a protected service key as "protected/identity_id/key".
     */
    @Test
    public void createdProtectedServiceKey() {
        StorageAccessLevel accessLevel = StorageAccessLevel.PROTECTED;
        String identityId = RandomString.string();
        String key = RandomString.string();
        final String expected = "protected/" + identityId + "/" + key;

        assertEquals(expected, S3Keys.createServiceKey(accessLevel, identityId, key));
    }

    /**
     * Validates construction of a private service key as "private/identity_id/key".
     */
    @Test
    public void createPrivateServiceKey() {
        StorageAccessLevel accessLevel = StorageAccessLevel.PRIVATE;
        String identityId = RandomString.string();
        String key = RandomString.string();
        final String expected = "private/" + identityId + "/" + key;

        assertEquals(expected, S3Keys.createServiceKey(accessLevel, identityId, key));
    }

    /**
     * Validates the extraction of an Amplify key, from a public service key.
     * The "public/" portion of the service key should be stripped off.
     */
    @Test
    public void amplifyKeyIsExtractedFromPublicServiceKey() {
        final String publicServiceKey = "public/foo/bar";
        assertEquals("foo/bar", S3Keys.extractAmplifyKey(publicServiceKey));
    }

    /**
     * Validates the extraction of an Amplify key, from a protected service key.
     * The "protected/" portion of the service key should be stripped off.
     */
    @Test
    public void amplifyKeyIsExtractedFromProtectedServiceKey() {
        final String serviceKey = "protected/foo/bar";
        assertEquals("bar", S3Keys.extractAmplifyKey(serviceKey));
    }

    /**
     * Validates the extraction of an Amplify key, from a private service key.
     * The "private/" portion of the service key should be stripped off.
     */
    @Test
    public void amplifyKeyIsExtractedFromPrivateServiceKey() {
        final String serviceKey = "private/foo/bar";
        assertEquals("bar", S3Keys.extractAmplifyKey(serviceKey));
    }

    /**
     * An attempt to extract an Amplify key from a service key that has a non-existent accessor
     * should throw an error.
     */
    @Test(expected = IllegalArgumentException.class)
    public void extractKeyFromServiceKeyWithBadAccessor() {
        final String serviceKey = "master/foo/bar";
        S3Keys.extractAmplifyKey(serviceKey);
    }

    /**
     * An attempt to extract an Amplify key from a service key that only has a prefix
     * should throw an error.
     */
    @Test(expected = IllegalArgumentException.class)
    public void extractKeyFromIncompleteServiceKey() {
        final String serviceKey = "private/foo";
        S3Keys.extractAmplifyKey(serviceKey);
    }
}
