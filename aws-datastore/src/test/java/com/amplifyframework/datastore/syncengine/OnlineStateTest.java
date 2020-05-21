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

package com.amplifyframework.datastore.syncengine;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.reachability.Reachability;
import com.amplifyframework.datastore.DataStoreChannelEventName;
import com.amplifyframework.hub.HubEvent;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import io.reactivex.disposables.Disposable;
import io.reactivex.observers.TestObserver;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the {@link OnlineState}.
 */
public final class OnlineStateTest {
    private static final long TIMEOUT_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * The system receives an "offline!" event. However, when the connectivity
     * is check, it comes back online. So, the online is state is false then true.
     */
    @Test
    public void startsOfflineEntersOnline() {
        Reachability reachability = mock(Reachability.class);
        setReachable(reachability, true);

        OnlineState onlineState = new OnlineState(Amplify.Hub, reachability);
        TestObserver<Boolean> observer = onlineState.observe().test();
        Disposable disposable = onlineState.startDetecting();

        onlineState.onEvent(HubEvent.create(DataStoreChannelEventName.LOST_CONNECTION));
        observer.awaitCount(2, new NoOpRunnable(), TIMEOUT_MS);
        observer.assertValues(false, true);

        disposable.dispose();
    }

    /**
     * The system starts online, then goes offline.
     */
    @Test
    public void startsOnlineGoesOffline() {
        Reachability reachability = mock(Reachability.class);
        setReachable(reachability, false);

        OnlineState onlineState = new OnlineState(Amplify.Hub, reachability);
        Disposable disposable = onlineState.startDetecting();
        TestObserver<Boolean> observer = onlineState.observe().test();

        onlineState.onEvent(HubEvent.create(DataStoreChannelEventName.REGAINED_CONNECTION));
        observer.awaitCount(1);
        observer.assertValues(true);

        disposable.dispose();
    }

    private static void setReachable(Reachability reachable, boolean isReachable) {
        if (!isReachable) {
            doAnswer(invocation -> null).when(reachable).whenReachable(any(), any());
            when(reachable.isReachable(any())).thenReturn(false);
            return;
        }

        doAnswer(invocation -> {
            ((Reachability.OnHostReachableAction) invocation.getArgument(1))
                .onHostReachable(invocation.getArgument(0));
            return /* void */ null;
        }).when(reachable).whenReachable(any(), any());
        when(reachable.isReachable(any())).thenReturn(true);
    }

    /**
     * A runnable that does nothing. Equivalent to () -> {}, but passes checkstyle!
     */
    private static final class NoOpRunnable implements Runnable {
        @Override
        public void run() {
        }
    }
}
