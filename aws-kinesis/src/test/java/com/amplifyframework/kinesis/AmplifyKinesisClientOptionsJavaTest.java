/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.kinesis;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.amplifyframework.foundation.credentials.AwsCredentials;
import com.amplifyframework.foundation.credentials.AwsCredentialsProvider;
import com.amplifyframework.recordcache.FlushStrategy;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import kotlin.coroutines.Continuation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Verifies that the configureClient API is ergonomic from Java —
 * a simple lambda without {@code return Unit.INSTANCE}.
 */
@RunWith(RobolectricTestRunner.class)
public class AmplifyKinesisClientOptionsJavaTest {

    private final AwsCredentialsProvider<AwsCredentials> fakeCredentials =
        (Continuation<? super AwsCredentials> continuation) ->
            new AwsCredentials.Static("FAKE_KEY", "FAKE_SECRET");

    /** Verifies that configureClient properly configures the SDK client. */
    @Test
    public void configureClientConfiguresSdkClient() {
        Context context = ApplicationProvider.getApplicationContext();

        AmplifyKinesisClient client = new AmplifyKinesisClient(
                context,
                "us-east-1",
                fakeCredentials,
                AmplifyKinesisClientOptions.builder()
                        .maxRetries(3)
                        .flushStrategy(FlushStrategy.None.INSTANCE)
                        .configureClient(clientBuilder -> {
                            clientBuilder.retryStrategy(strategy -> {
                                strategy.setMaxAttempts(10);
                                return kotlin.Unit.INSTANCE;
                            });
                        })
                        .build()
        );

        assertEquals(3, client.getOptions().getMaxRetries());
        assertNotNull(client.getOptions().getConfigureClient());
        assertNotNull(client.getKinesisClient());
        assertEquals(10, client.getKinesisClient().getConfig().getRetryStrategy().getConfig().getMaxAttempts());
    }
}
