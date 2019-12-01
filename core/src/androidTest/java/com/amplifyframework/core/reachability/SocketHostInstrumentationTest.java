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

package com.amplifyframework.core.reachability;

import com.amplifyframework.testutils.RandomString;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests the {@link SocketHost}.
 */
public final class SocketHostInstrumentationTest {
    private static final int SECURE_HTTPS_PORT = 443;

    /**
     * The AWS website (aws.amazon.com) is reachable (with high availability).
     */
    @Test
    public void awsWebsiteIsReachable() {
        assertTrue(SocketHost.from(String.format(
            "https://aws.amazon.com:%d", SECURE_HTTPS_PORT
        )).isReachable());
    }

    /**
     * A random host under aws.amazon.com, which does not exist, is not reachable.
     */
    @Test
    public void randomAmazonWebServiceIsNotReachable() {
        assertFalse(SocketHost.from(String.format(
            // Theoretically this URI is well-formed, but the host won't exist,
            // so therefor it won't be reachable over the internet.
            "https://%s.aws.amazon.com:%d", RandomString.string(), SECURE_HTTPS_PORT
        )).isReachable());
    }

    /**
     * It is not possible to create an instance of {@link SocketHost} from a garbage string.
     */
    @Test(expected = NullPointerException.class)
    public void garbageUriThrowsNullPointerException() {
        SocketHost.from(RandomString.string());
    }

    /**
     * It is not possible to create an instance of {@link SocketHost} without providing a port.
     */
    @Test(expected = Exception.class)
    public void missingPortThrowsException() {
        SocketHost.from("amazon.com");
    }

    /**
     * It is not possible to create an instance of {@link SocketHost} by providing an illegal port.
     */
    @Test(expected = IllegalArgumentException.class)
    public void portOutOfRangeThrowsIllegalArgumentException() {
        SocketHost.from("amazon.com", -1);
    }

    /**
     * The Amazon retail web-page is reachable (with high availability).
     */
    @Test
    public void amazonRetailIsReachable() {
        assertTrue(SocketHost.from("amazon.com", SECURE_HTTPS_PORT).isReachable());
    }
}
