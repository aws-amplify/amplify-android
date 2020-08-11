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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

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

        assertNotEquals(status1, status2);
        assertEquals(status1, status3);

        assertNotEquals(status1.hashCode(), status2.hashCode());
        assertEquals(status1.hashCode(), status3.hashCode());
        assertFalse(status1 == status3);

        assertNotEquals(status1.toString(), status2.toString());
        assertEquals(status1.toString(), status3.toString());
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

        assertNotEquals(status1, status2);
        assertEquals(status1, status3);

        assertNotEquals(status1.hashCode(), status2.hashCode());
        assertEquals(status1.hashCode(), status3.hashCode());
        assertFalse(status1 == status3);

        assertNotEquals(status1.toString(), status2.toString());
        assertEquals(status1.toString(), status3.toString());
    }
}
