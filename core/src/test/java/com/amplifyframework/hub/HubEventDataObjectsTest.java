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

package com.amplifyframework.hub;

import com.amplifyframework.api.events.ApiEndpointStatusChangeEvent;
import com.amplifyframework.api.events.ApiEndpointStatusChangeEvent.ApiEndpointStatus;
import com.amplifyframework.datastore.events.NetworkStatusEvent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test basic operations of objects used as Hub event payloads.
 */
@RunWith(RobolectricTestRunner.class)
public class HubEventDataObjectsTest {

    /**
     * Verify {@link NetworkStatusEvent} behavior.
     */
    @Test
    public void verifyNetworkStatus() {
        NetworkStatusEvent status1 = new NetworkStatusEvent(true);
        NetworkStatusEvent status2 = new NetworkStatusEvent(false);
        NetworkStatusEvent status3 = new NetworkStatusEvent(true);

        assertCoreObjectBehavior(status1, status2, status3);
    }

    /**
     * Verify {@link ApiEndpointStatusChangeEvent} behavior.
     */
    @Test
    public void verifyApiEndpointStatusChangeEvent() {
        ApiEndpointStatusChangeEvent status1 =
            new ApiEndpointStatusChangeEvent(ApiEndpointStatus.REACHABLE, ApiEndpointStatus.NOT_REACHABLE);
        ApiEndpointStatusChangeEvent status2 =
            new ApiEndpointStatusChangeEvent(ApiEndpointStatus.NOT_REACHABLE, ApiEndpointStatus.REACHABLE);
        ApiEndpointStatusChangeEvent status3 =
            new ApiEndpointStatusChangeEvent(ApiEndpointStatus.REACHABLE, ApiEndpointStatus.NOT_REACHABLE);
        assertCoreObjectBehavior(status1, status2, status3);
    }

    /**
     * Function to make some basic assertions as it related to the
     * proper implementation of equals, hashCode and toString.
     * @param subject1 An instance of T
     * @param suject2 An instance of T that is not equal to subject1.
     * @param subject3ThatEqualsSubject1 An instance of T such that when passed as
     *                                   a parameter to subject1.equals, will return true.
     * @param <T> The type being tested.
     */
    private <T> void assertCoreObjectBehavior(T subject1, T suject2, T subject3ThatEqualsSubject1) {
        Set<T> set = new HashSet<>();
        set.add(subject1);
        set.add(suject2);
        set.add(subject3ThatEqualsSubject1);

        assertNotEquals(subject1, suject2);
        // Check that subject1 and subject3ThatEqualsSubject1 are equivalent
        // as far as the equals method is concerned.
        assertEquals(subject1, subject3ThatEqualsSubject1);
        // Check that subject1 and subject3ThatEqualsSubject1 are not the same instance.
        assertFalse(subject1 == subject3ThatEqualsSubject1);

        // Since subject1 and subject3ThatEqualsSubject1 are equals,
        // we should only have two items in the set.
        assertEquals(2, set.size());
        assertTrue(set.contains(subject1));
        assertTrue(set.contains(suject2));
        assertTrue(set.contains(subject3ThatEqualsSubject1));

        assertNotEquals(subject1.toString(), suject2.toString());
        assertEquals(subject1.toString(), subject3ThatEqualsSubject1.toString());
    }
}
