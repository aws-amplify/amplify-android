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
import com.amplifyframework.datastore.events.ModelSyncedEvent;
import com.amplifyframework.datastore.events.NetworkStatusEvent;
import com.amplifyframework.datastore.events.SyncQueriesStartedEvent;
import com.amplifyframework.testutils.EqualsToStringHashValidator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

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

        EqualsToStringHashValidator.validate(status1, status2, status3);
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
        EqualsToStringHashValidator.validate(status1, status2, status3);
    }

    /**
     * Verify {@link ModelSyncedEvent} behavior.
     */
    @Test
    public void verifyModelSyncedEvent() {
        ModelSyncedEvent status1 =
            new ModelSyncedEvent("Post", true, 1, 2, 3);
        ModelSyncedEvent status2 =
            new ModelSyncedEvent("Blog", true, 3, 2, 1);
        ModelSyncedEvent status3 =
            new ModelSyncedEvent("Post", true, 1, 2, 3);
        EqualsToStringHashValidator.validate(status1, status2, status3);
    }

    /**
     * Verify {@link SyncQueriesStartedEvent} behavior.
     */
    @Test
    public void verifySyncQueriesStartedEvent() {
        SyncQueriesStartedEvent status1 =
            new SyncQueriesStartedEvent(new String[] {"Blog", "Post"});
        SyncQueriesStartedEvent status2 =
            new SyncQueriesStartedEvent(new String[] {"Blog", "Post", "Car"});
        SyncQueriesStartedEvent status3 =
            new SyncQueriesStartedEvent(new String[] {"Blog", "Post"});
        EqualsToStringHashValidator.validate(status1, status2, status3);
    }
}
