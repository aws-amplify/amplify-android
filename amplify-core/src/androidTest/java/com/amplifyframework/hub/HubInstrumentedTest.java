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

import android.util.Log;
import androidx.test.core.app.ApplicationProvider;

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

/**
 * Validates the functionality of the {@link BackgroundExecutorHubPlugin}.
 */
public final class HubInstrumentedTest {

    private static final String TAG = HubInstrumentedTest.class.getSimpleName();

    private static final long SUBSCRIPTION_RECEIVE_TIMEOUT_IN_MILLISECONDS = 100;

    /**
     * Before any test is run, configure Amplify to use an
     * {@link BackgroundExecutorHubPlugin} to satify the Hub category.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        Amplify.addPlugin(new BackgroundExecutorHubPlugin());
        Amplify.configure(ApplicationProvider.getApplicationContext());
    }

    /**
     * Validates that the token returned from a subscription call can be
     * used to unsubscribe from the hub.
     * @throws InterruptedException when waiting for CountDownLatch to
     *                              meet the desired condition is interrupted.
     */
    @Test
    public void subscriptionTokenCanBeUsedToUnsubscribe() throws InterruptedException {
        final CountDownLatch waitUntilSubscriptionIsReceived = new CountDownLatch(1);
        final SubscriptionToken token = Amplify.Hub.subscribe(HubChannel.STORAGE, payload -> {
            waitUntilSubscriptionIsReceived.countDown();
        });
        assertNotNull(token);
        assertNotNull(token.getUuid());

        Amplify.Hub.unsubscribe(token);

        assertFalse("Expecting no subscription to be received within the given time.",
                waitUntilSubscriptionIsReceived.await(SUBSCRIPTION_RECEIVE_TIMEOUT_IN_MILLISECONDS,
                        TimeUnit.MILLISECONDS));
    }


    /**
     * Validates that a subscribed listener will receive a published
     * event.
     * @throws InterruptedException when waiting for CountDownLatch to
     *                              meet the desired condition is interrupted.
     */
    @Test
    public void isSubscriptionReceived() throws InterruptedException {
        final CountDownLatch waitUntilSubscriptionIsReceived = new CountDownLatch(1);

        final SubscriptionToken token = Amplify.Hub.subscribe(HubChannel.STORAGE, payload -> {
            if (payload.getEventData() instanceof String) {
                Log.d(TAG, "String: => " + payload.getEventName() + ":" + payload.getEventData());
                waitUntilSubscriptionIsReceived.countDown();
            }
        });

        Amplify.Hub.publish(HubChannel.STORAGE,
                new HubPayload("weatherString", "Too Cold in Seattle."));

        assertTrue("Subscription not received within the expected time.",
                waitUntilSubscriptionIsReceived.await(SUBSCRIPTION_RECEIVE_TIMEOUT_IN_MILLISECONDS,
                        TimeUnit.MILLISECONDS));

        Amplify.Hub.unsubscribe(token);
    }

    /**
     * Validates that a listener will not continue to receive events
     * from the hub, once it has unsubscribed.
     * @throws InterruptedException when waiting for CountDownLatch to
     *                              meet the desired condition is interrupted.
     */
    @Test
    public void noSubscriptionReceivedAfterUnsubscribe() throws InterruptedException {
        final CountDownLatch subscriptionReceived = new CountDownLatch(1);

        final SubscriptionToken token = Amplify.Hub.subscribe(HubChannel.STORAGE, payload -> {
            Log.e(TAG, "Not expecting a subscription to be received after unsubscribe.");
            subscriptionReceived.countDown();
        });

        Amplify.Hub.unsubscribe(token);

        Amplify.Hub.publish(HubChannel.STORAGE,
                new HubPayload("weatherString", "Too Cold in Seattle"));

        assertFalse("Expecting no subscription to be received within the given time.",
                    subscriptionReceived.await(SUBSCRIPTION_RECEIVE_TIMEOUT_IN_MILLISECONDS,
                            TimeUnit.MILLISECONDS));
    }

    /**
     * Validates that a hub listener will receive all of a series of
     * multiple publications.
     * @throws InterruptedException when waiting for CountDownLatch to
     *                              meet the desired condition is interrupted.
     */
    @Test
    public void multiplePublications() throws InterruptedException {
        final int numPublications = 10;
        final CountDownLatch allSubscriptionsReceived = new CountDownLatch(numPublications);
        final List<Integer> subscriptionsReceived = new ArrayList<Integer>();

        final SubscriptionToken token = Amplify.Hub.subscribe(HubChannel.STORAGE, payload -> {
            if (payload.getEventData() instanceof Integer) {
                Log.d(TAG, "Integer: => " + payload.getEventName() + ":" + payload.getEventData());
                subscriptionsReceived.add((Integer) payload.getEventData());
                allSubscriptionsReceived.countDown();
            }
        });

        for (int i = 0; i < numPublications; i++) {
            Amplify.Hub.publish(HubChannel.STORAGE,
                    new HubPayload("weatherInteger:" + i, i));
        }

        assertTrue("Expecting to receive all " + numPublications + " subscriptions.",
                    allSubscriptionsReceived.await(SUBSCRIPTION_RECEIVE_TIMEOUT_IN_MILLISECONDS,
                            TimeUnit.MILLISECONDS));

        Collections.sort(subscriptionsReceived);
        int expectedMessageValue = 0;
        for (Integer message: subscriptionsReceived) {
            assertEquals(expectedMessageValue, message.intValue());
            expectedMessageValue++;
        }

        Amplify.Hub.unsubscribe(token);
    }

    /**
     * Validates that a hub listener will receive publications of
     * multiple events of different types.
     * @throws InterruptedException when waiting for CountDownLatch to
     *                              meet the desired condition is interrupted.
     */
    @Test
    public void multiplePublicationsMultipleDataTypes() throws InterruptedException {
        final int numPublications = 10;
        final int numDataTypes = 2;
        final CountDownLatch allSubscriptionsReceived = new CountDownLatch(numPublications);
        final List<Integer> integerSubscriptionsReceived = new ArrayList<Integer>();
        final List<String> stringSubscriptionsReceived = new ArrayList<String>();
        final String stringSubscriptionValue = "weatherAlwaysRemainsTheSame";

        final SubscriptionToken token = Amplify.Hub.subscribe(HubChannel.STORAGE, payload -> {
            if (payload.getEventData() instanceof Integer) {
                Log.d(TAG, "Integer: => " + payload.getEventName() + ":" + payload.getEventData());
                integerSubscriptionsReceived.add((Integer) payload.getEventData());
                allSubscriptionsReceived.countDown();
            } else if (payload.getEventData() instanceof String) {
                Log.d(TAG, "String: => " + payload.getEventName() + ":" + payload.getEventData());
                stringSubscriptionsReceived.add((String) payload.getEventData());
                allSubscriptionsReceived.countDown();
            }
        });

        for (int i = 0; i < numPublications / numDataTypes; i++) {
            Amplify.Hub.publish(HubChannel.STORAGE,
                    new HubPayload("weatherInteger:" + i, i));
            Amplify.Hub.publish(HubChannel.STORAGE,
                    new HubPayload("weatherString:" + i, stringSubscriptionValue));
        }

        assertTrue("Expecting to receive all " + numPublications + " subscriptions.",
                    allSubscriptionsReceived.await(SUBSCRIPTION_RECEIVE_TIMEOUT_IN_MILLISECONDS,
                            TimeUnit.MILLISECONDS));

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

