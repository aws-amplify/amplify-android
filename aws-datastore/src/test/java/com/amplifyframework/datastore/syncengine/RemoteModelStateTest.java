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

package com.amplifyframework.datastore.syncengine;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchemaRegistry;
import com.amplifyframework.datastore.appsync.AppSync;
import com.amplifyframework.datastore.appsync.AppSyncMocking;
import com.amplifyframework.datastore.appsync.ModelWithMetadata;
import com.amplifyframework.datastore.appsync.TestModelWithMetadataInstances;
import com.amplifyframework.testmodels.commentsblog.AmplifyModelProvider;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testmodels.commentsblog.Post;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import io.reactivex.observers.TestObserver;

import static com.amplifyframework.datastore.appsync.TestModelWithMetadataInstances.BLOGGER_ISLA;
import static com.amplifyframework.datastore.appsync.TestModelWithMetadataInstances.BLOGGER_JAMESON;
import static com.amplifyframework.datastore.appsync.TestModelWithMetadataInstances.DELETED_DRUM_POST;
import static com.amplifyframework.datastore.appsync.TestModelWithMetadataInstances.DRUM_POST;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Tests the {@link RemoteModelState}.
 */
@SuppressWarnings("checkstyle:MagicNumber") // Arranged data picked arbitrarily
public final class RemoteModelStateTest {
    private static final long REASONABLE_WAIT_TIME_MS = TimeUnit.SECONDS.toMillis(2);

    private AppSync appSync;
    private RemoteModelState remoteModelState;

    /**
     * Mock out our dependencies. We'll want to pretend we have some different models
     * being provided by the system. And, we want to mock away the AppSync, so we
     * can test our logic in isolation without going out to the network / depending
     * on some backend to have a certain configuration / state.
     * @throws AmplifyException If registry fails to load schema
     */
    @Before
    public void setup() throws AmplifyException {
        appSync = mock(AppSync.class);

        ModelProvider modelProvider = AmplifyModelProvider.getInstance();
        ModelSchemaRegistry modelSchemaRegistry = ModelSchemaRegistry.instance();
        modelSchemaRegistry.clear();
        modelSchemaRegistry.load(modelProvider.models());

        remoteModelState = new RemoteModelState(appSync, modelProvider, modelSchemaRegistry);
    }

    /**
     * Observe the remote model state, via {@link RemoteModelState#observe()}.
     * Validate that the observed items are the ones that were provided by our
     * arranged {@link AppSync} mock.
     */
    @Test
    public void observeReceivesAllModelInstances() {
        // Arrange: the AppSync endpoint will give us some MetaData for items
        // having these types.
        AppSyncMocking.configure(appSync)
            .mockSuccessResponse(Post.class, DRUM_POST, DELETED_DRUM_POST)
            .mockSuccessResponse(BlogOwner.class, BLOGGER_JAMESON, BLOGGER_ISLA);

        // Act: Observe the RemoteModelState via observe().
        TestObserver<ModelWithMetadata<? extends Model>> observer = remoteModelState.observe().test();

        assertTrue(observer.awaitTerminalEvent(REASONABLE_WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        observer.assertValueCount(4);

        // assertValueSet(..., varargs, ...) would be cleanest. But equals() is broken
        // on the generated models right now. So, instead, use our own assertEquals() for right now...
        TestModelWithMetadataInstances.assertEquals(
            Arrays.asList(BLOGGER_JAMESON, BLOGGER_ISLA, DRUM_POST, DELETED_DRUM_POST),
            observer.values()
        );
        observer.dispose();
    }
}

