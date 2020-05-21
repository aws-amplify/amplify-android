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

import com.amplifyframework.core.reachability.Host;
import com.amplifyframework.core.reachability.PeriodicReachabilityChecker;
import com.amplifyframework.core.reachability.Reachability;
import com.amplifyframework.core.reachability.SocketHost;

import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.reactivex.observers.TestObserver;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

/**
 * Test the {@link AwsReachability}.
 */
public final class AwsReachabilityTest {
    private static final long SHORT_DELAY_MS = 100;
    private static final long OP_TIMEOUT_MS = 10 * SHORT_DELAY_MS;

    /**
     * Validates that the host is reachable, when it is.
     * @throws IOException On failure to start/stop the mock web server
     */
    @Test
    public void isReachable() throws IOException {
        MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.start(8080);
        HttpUrl url = mockWebServer.url("/");
        mockWebServer.enqueue(new MockResponse().setBody("{}").setResponseCode(200));

        Host host = SocketHost.from(url.host(), url.port());
        Reachability periodic = PeriodicReachabilityChecker.instance(SHORT_DELAY_MS);

        AwsReachability reachability = new AwsReachability(host, periodic);
        TestObserver<Boolean> observer = reachability.isReachable().test();
        observer.awaitTerminalEvent(OP_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        observer.assertValues(true).assertNoErrors().assertComplete();

        mockWebServer.shutdown();
    }

    /**
     * When the internet starts offline, then goes online, {@link AwsReachability#awaitReachable()}
     * will complete.
     * @throws IOException on failure to setup/teardown MockWebServer
     */
    @Test
    public void awaitReachableWillComplete() throws IOException {
        MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.start(8080);
        HttpUrl url = mockWebServer.url("/");
        Host host = SocketHost.from(url.host(), url.port());
        // mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));

        PeriodicReachabilityChecker periodic = PeriodicReachabilityChecker.instance(SHORT_DELAY_MS);
        AwsReachability reachability = new AwsReachability(host, periodic);
        TestObserver<Void> observer = reachability.awaitReachable().test();

        observer.awaitTerminalEvent(OP_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        observer.assertNoErrors().assertComplete();

        mockWebServer.shutdown();
    }
}
