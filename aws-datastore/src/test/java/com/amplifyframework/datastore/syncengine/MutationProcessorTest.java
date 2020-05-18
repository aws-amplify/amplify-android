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

import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.appsync.AppSync;
import com.amplifyframework.datastore.appsync.AppSyncMocking;
import com.amplifyframework.datastore.storage.InMemoryStorageAdapter;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;
import com.amplifyframework.datastore.storage.SynchronousStorageAdapter;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testutils.HubAccumulator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

import static com.amplifyframework.datastore.syncengine.TestHubEventFilters.publicationOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests the {@link MutationProcessor}.
 */
@RunWith(RobolectricTestRunner.class)
public final class MutationProcessorTest {
    private SynchronousStorageAdapter synchronousStorageAdapter;
    private MutationOutbox mutationOutbox;
    private AppSync appSync;
    private MutationProcessor mutationProcessor;

    /**
     * A {@link MutationProcessor} is being tested. To do so, we arrange mutations into
     * an {@link MutationOutbox}. Fake responses are returned from a mock {@link AppSync}.
     */
    @Before
    public void setup() {
        LocalStorageAdapter localStorageAdapter = InMemoryStorageAdapter.create();
        this.synchronousStorageAdapter = SynchronousStorageAdapter.delegatingTo(localStorageAdapter);
        this.mutationOutbox = new PersistentMutationOutbox(localStorageAdapter);
        Merger merger = new Merger(mutationOutbox, localStorageAdapter);
        VersionRepository versionRepository = new VersionRepository(localStorageAdapter);
        this.appSync = mock(AppSync.class);
        this.mutationProcessor = new MutationProcessor(merger, versionRepository, mutationOutbox, appSync);
    }

    /**
     * Tests the {@link MutationProcessor#startDrainingMutationOutbox()}. After this method
     * is called, any content in the {@link MutationOutbox} should be published via the {@link AppSync}
     * and then removed.
     * @throws DataStoreException On failure to interact with storage adapter during arrangement
     *                            and verification
     */
    @Test
    public void canDrainMutationOutbox() throws DataStoreException {
        // We will attempt to "sync" this model.
        BlogOwner tony = BlogOwner.builder()
            .name("Tony Daniels")
            .build();
        synchronousStorageAdapter.save(tony);

        // Arrange a cooked response from AppSync.
        AppSyncMocking.onCreate(appSync).mockResponse(tony);

        // Start listening for publication events.
        HubAccumulator accumulator =
            HubAccumulator.create(HubChannel.DATASTORE, publicationOf(tony), 1)
                .start();

        PendingMutation<BlogOwner> createTony = PendingMutation.creation(tony, BlogOwner.class);
        mutationOutbox.enqueue(createTony).blockingAwait();

        // Act! Start draining the outbox.
        mutationProcessor.startDrainingMutationOutbox();

        // Assert: the event was published
        List<HubEvent<?>> events = accumulator.await();
        assertEquals(1, events.size());
        @SuppressWarnings("unchecked")
        PendingMutation<BlogOwner> mutation = (PendingMutation<BlogOwner>) events.get(0).getData();
        assertEquals(createTony, mutation);

        // And that it is no longer in the outbox.
        assertFalse(mutationOutbox.hasPendingMutation(tony.getId()));

        // And that it was passed to AppSync for publication.
        verify(appSync).create(eq(tony), any(), any());
    }
}
