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

package com.amplifyframework.hub;

import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.util.Log;

import com.amplifyframework.core.Amplify;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class HubInstrumentedTest {

    private static final String TAG = HubInstrumentedTest.class.getSimpleName();

    @BeforeClass
    public static void setUpBeforeClass() {
        Amplify.addPlugin(new BackgroundExecutorHubPlugin());
        Amplify.configure(InstrumentationRegistry.getTargetContext());
    }

    @Test
    public void subscription_token_not_null() {
        final SubscriptionToken token = Amplify.Hub.subscribe(HubChannel.STORAGE, new HubListener() {
            @Override
            public void onEvent(@NonNull HubPayload payload) { }
        });
        assertNotNull(token);
        assertNotNull(token.getUuid());

        Amplify.Hub.unsubscribe(token);
    }


    /**
     * @throws InterruptedException when waiting for CountDownLatch to
     *                              meet the desired condition is interrupted.
     */
    @Test
    public void is_subscription_received() throws InterruptedException {
        final CountDownLatch waitUntilSubscriptionIsReceived = new CountDownLatch(1);

        final SubscriptionToken token = Amplify.Hub.subscribe(HubChannel.STORAGE, new HubListener() {
            @Override
            public void onEvent(@NonNull HubPayload payload) {
                if (payload.getEventData() instanceof String) {
                    Log.d(TAG, "String: => " + payload.getEventName() + ":" + payload.getEventData());
                    waitUntilSubscriptionIsReceived.countDown();
                }
            }
        });

        Amplify.Hub.publish(HubChannel.STORAGE,
                new HubPayload("weatherString","Too Cold in Seattle."));


        assertTrue("Subscription not received within the expected time.",
                waitUntilSubscriptionIsReceived.await(5, TimeUnit.SECONDS));

        Amplify.Hub.unsubscribe(token);
    }

    /**
     * @throws InterruptedException when waiting for CountDownLatch to
     *                              meet the desired condition is interrupted.
     */
    @Test
    public void no_subscription_received_after_unsubscribe() throws InterruptedException {
        final CountDownLatch subscriptionReceived = new CountDownLatch(1);

        final SubscriptionToken token = Amplify.Hub.subscribe(HubChannel.STORAGE, new HubListener() {
            @Override
            public void onEvent(@NonNull HubPayload payload) {
                Log.e(TAG, "Not expecting a subscription to be received after unsubscribe.");
                subscriptionReceived.countDown();
            }
        });

        Amplify.Hub.unsubscribe(token);

        Amplify.Hub.publish(HubChannel.STORAGE,
                new HubPayload("weatherString", "Too Cold in Seattle"));

        assertFalse("Expecting no subscription to be received within the given time.",
                    subscriptionReceived.await(5, TimeUnit.SECONDS));
    }

    /**
     * @throws InterruptedException when waiting for CountDownLatch to
     *                              meet the desired condition is interrupted.
     */
    @Test
    public void multiple_publications() throws InterruptedException {
        final int NUM_PUBLICATIONS = 10;
        final CountDownLatch allSubscriptionsReceived = new CountDownLatch(NUM_PUBLICATIONS);
        final List<Integer> subscriptionsReceived = new ArrayList<Integer>();

        final SubscriptionToken token = Amplify.Hub.subscribe(HubChannel.STORAGE, new HubListener() {
            @Override
            public void onEvent(@NonNull HubPayload payload) {
                if (payload.getEventData() instanceof Integer) {
                    Log.d(TAG, "Integer: => " + payload.getEventName() + ":" + payload.getEventData());
                    subscriptionsReceived.add((Integer) payload.getEventData());
                    allSubscriptionsReceived.countDown();
                }
            }
        });

        for (int i = 0; i < NUM_PUBLICATIONS; i++) {
            Amplify.Hub.publish(HubChannel.STORAGE,
                    new HubPayload("weatherInteger:" + i, i));
        }

        assertTrue("Expecting to receive all " + NUM_PUBLICATIONS + " subscriptions.",
                    allSubscriptionsReceived.await(10, TimeUnit.SECONDS));

        Collections.sort(subscriptionsReceived);
        int expectedMessageValue = 0;
        for (Integer message: subscriptionsReceived) {
            assertEquals(expectedMessageValue, message.intValue());
            expectedMessageValue++;
        }

        Amplify.Hub.unsubscribe(token);
    }

    /**
     * @throws InterruptedException when waiting for CountDownLatch to
     *                              meet the desired condition is interrupted.
     */
    @Test
    public void multiple_subscriptions_multiple_datatypes() throws InterruptedException {
        final int NUM_PUBLICATIONS = 10;
        final int NUM_DATA_TYPES = 2;
        final CountDownLatch allSubscriptionsReceived = new CountDownLatch(NUM_PUBLICATIONS);
        final List<Integer> integerSubscriptionsReceived = new ArrayList<Integer>();
        final List<String> stringSubscriptionsReceived = new ArrayList<String>();
        final String stringSubscriptionValue = "weatherAlwaysRemainsTheSame";

        final SubscriptionToken token = Amplify.Hub.subscribe(HubChannel.STORAGE, new HubListener() {
            @Override
            public void onEvent(@NonNull HubPayload payload) {
                if (payload.getEventData() instanceof Integer) {
                    Log.d(TAG, "Integer: => " + payload.getEventName() + ":" + payload.getEventData());
                    integerSubscriptionsReceived.add((Integer) payload.getEventData());
                    allSubscriptionsReceived.countDown();
                } else if (payload.getEventData() instanceof String) {
                    Log.d(TAG, "String: => " + payload.getEventName() + ":" + payload.getEventData());
                    stringSubscriptionsReceived.add((String) payload.getEventData());
                    allSubscriptionsReceived.countDown();
                }
            }
        });

        for (int i = 0; i < NUM_PUBLICATIONS / NUM_DATA_TYPES; i++) {
            Amplify.Hub.publish(HubChannel.STORAGE,
                    new HubPayload("weatherInteger:" + i, i));
            Amplify.Hub.publish(HubChannel.STORAGE,
                    new HubPayload("weatherString:" + i, stringSubscriptionValue));
        }

        assertTrue("Expecting to receive all " + NUM_PUBLICATIONS + " subscriptions.",
                    allSubscriptionsReceived.await(10, TimeUnit.SECONDS));

        Collections.sort(integerSubscriptionsReceived);
        int expectedIntegerSubscriptionValue = 0;
        for (Integer message: integerSubscriptionsReceived) {
            assertEquals(expectedIntegerSubscriptionValue, message.intValue());
            expectedIntegerSubscriptionValue++;
        }
        for (String message: stringSubscriptionsReceived) {
            assertEquals("weatherAlwaysRemainsTheSame", message);
        }

        Amplify.Hub.unsubscribe(token);
    }
}
