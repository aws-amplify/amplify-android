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

import com.amplifyframework.core.model.temporal.Temporal;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.appsync.ModelMetadata;
import com.amplifyframework.datastore.storage.InMemoryStorageAdapter;
import com.amplifyframework.datastore.storage.SynchronousStorageAdapter;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;

import org.junit.Before;
import org.junit.Test;

import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.observers.TestObserver;

import static org.junit.Assert.assertTrue;

/**
 * Tests the {@link VersionRepository}.
 */
public final class VersionRepositoryTest {
    private static final long REASONABLE_WAIT_TIME = TimeUnit.SECONDS.toMillis(1);

    private SynchronousStorageAdapter storageAdapter;
    private VersionRepository versionRepository;

    /**
     * Sets up the test. A {@link VersionRepository} is tested with respect to the varying
     * state of its dependency, the {@link InMemoryStorageAdapter}.
     *
     * An {@link InMemoryStorageAdapter} is used as a test fake. Versions can be arranged into
     * it, and validated against it. To facilitate that arrangement, an {@link SynchronousStorageAdapter}
     * utility is used to wrap the {@link InMemoryStorageAdapter}.
     */
    @Before
    public void setup() {
        InMemoryStorageAdapter inMemoryStorageAdapter = InMemoryStorageAdapter.create();
        this.storageAdapter = SynchronousStorageAdapter.delegatingTo(inMemoryStorageAdapter);
        this.versionRepository = new VersionRepository(inMemoryStorageAdapter);
    }

    /**
     * When you try to get a model version, but there's no metadata for that model,
     * this should fail with an {@link DataStoreException}.
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     */
    @Test
    public void emitsErrorForNoMetadataInRepo() throws InterruptedException {
        // Arrange: no metadata is in the repo.
        BlogOwner blogOwner = BlogOwner.builder()
            .name("Jameson Williams")
            .build();
        // Note that this line is NOT executed in arrangement.
        // ModelMetadata metadata = new ModelMetadata(blogOwner.getId(), false, 1, Time.now());
        // putInStore(blogOwner blogOwner);

        // Act: try to lookup the metadata. Is it going to work? Duh.
        TestObserver<Integer> observer = versionRepository.findModelVersion(blogOwner).test();
        assertTrue(observer.await(REASONABLE_WAIT_TIME, TimeUnit.MILLISECONDS));

        // Assert: this failed. There was no version available.
        observer.assertError(error -> {
            if (!(error instanceof DataStoreException)) {
                return false;
            }
            String expectedMessage =
                String.format(Locale.US, "Wanted 1 metadata for item with id = %s, but had 0.", blogOwner.getId());
            return expectedMessage.equals(error.getMessage());
        });
    }

    /**
     * When you try to get the version for a model, and there is metadata for the model
     * in the DataStore, BUT the version info is not populated, this should return an
     * {@link DataStoreException}.
     * @throws DataStoreException
     *         NOT EXPECTED. This happens on failure to arrange data before test action.
     *         The expected DataStoreException is communicated via callback, not thrown
     *         on the calling thread. It's a different thing than this.
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     */
    @Test
    public void emitsErrorWhenMetadataHasNullVersion() throws DataStoreException, InterruptedException {
        // Arrange a model an metadata into the store, but the metadtaa doesn't contain a valid version
        BlogOwner blogOwner = BlogOwner.builder()
            .name("Jameson")
            .build();
        ModelMetadata metadata = new ModelMetadata(blogOwner.getId(), null, null, null);
        storageAdapter.save(blogOwner, metadata);

        // Act: try to get the version.
        TestObserver<Integer> observer = versionRepository.findModelVersion(blogOwner).test();
        assertTrue(observer.await(REASONABLE_WAIT_TIME, TimeUnit.MILLISECONDS));

        // Assert: the single emitted a DataStoreException.
        observer.assertError(error -> {
            if (!(error instanceof DataStoreException)) {
                return false;
            }
            String expectedMessage =
                String.format(Locale.US, "Metadata for item with id = %s had null version.", blogOwner.getId());
            return expectedMessage.equals(error.getMessage());
        });
    }

    /**
     * When there is metadata for a model in the store, and that metadata includes a version -
     * for heaven's sake, man - do please emit the dang thing.
     * @throws DataStoreException On failure to arrange data into store
     * @throws InterruptedException If interrupted while awaiting terminal result in test observer
     */
    @Test
    public void emitsSuccessWithValueWhenVersionInStore() throws DataStoreException, InterruptedException {
        // Arrange versioning info into the store.
        BlogOwner owner = BlogOwner.builder()
            .name("Jameson")
            .build();
        final int maxRandomVersion = 1_000;
        int expectedVersion = new Random().nextInt(maxRandomVersion);
        storageAdapter.save(new ModelMetadata(owner.getId(), false, expectedVersion, Temporal.Timestamp.now()));

        // Act! Try to obtain it via the Versioning Repository.
        TestObserver<Integer> observer = versionRepository.findModelVersion(owner).test();
        assertTrue(observer.await(REASONABLE_WAIT_TIME, TimeUnit.MILLISECONDS));

        // Assert: we got a version.
        observer
            .assertNoErrors()
            .assertComplete()
            .assertValue(expectedVersion);
    }
}
