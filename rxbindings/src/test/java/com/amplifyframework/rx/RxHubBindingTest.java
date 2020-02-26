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

package com.amplifyframework.rx;

import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.testutils.random.RandomString;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import io.reactivex.observers.TestObserver;

/**
 * Tests the {@link RxHubBinding}.
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public final class RxHubBindingTest {
    private static final long REASONABLE_WAIT_TIME_MS = TimeUnit.SECONDS.toMillis(1);

    private RxHubCategoryBehavior rxHub;

    @Before
    public void setup() {
        this.rxHub = new RxHubBinding();
    }

    @Test
    public void publishedEventIsReceived() {
        // Act: Create a subscriber,
        TestObserver<HubEvent<?>> observer = rxHub.on(HubChannel.HUB).test();

        // Act: publish an event
        HubEvent<?> event = HubEvent.create(RandomString.string());
        rxHub.publish(HubChannel.HUB, event)
            .blockingAwait(REASONABLE_WAIT_TIME_MS, TimeUnit.MILLISECONDS);

        // Assert: subscriber got event
        observer.awaitCount(1).assertValue(event);
    }

    @Test
    public void subscriberDoesNotReceiveEventWhenCanceled() {
        // Act: subscribe, then cancel
        TestObserver<HubEvent<?>> observer = rxHub.on(HubChannel.HUB).test();
        observer.cancel();

        // Act: publish an event
        rxHub.publish(HubChannel.HUB, HubEvent.create(RandomString.string()));

        // Assert: observer was canceled, it never received any event(s).
        observer.isCancelled();
        observer.assertNoValues();
    }
}
