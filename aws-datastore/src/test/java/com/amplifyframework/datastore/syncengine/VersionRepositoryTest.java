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

import androidx.annotation.NonNull;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.appsync.ModelMetadata;
import com.amplifyframework.datastore.storage.InMemoryStorageAdapter;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testutils.Await;
import com.amplifyframework.testutils.Time;

import org.junit.Before;
import org.junit.Test;

import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import io.reactivex.observers.TestObserver;

import static org.junit.Assert.assertTrue;

/**
 * Tests the {@link VersionRepository}.
 */
public final class VersionRepositoryTest {
    private static final long REASONABLE_WAIT_TIME = TimeUnit.SECONDS.toMillis(1);

    private InMemoryStorageAdapter inMemoryStorageAdapter;
    private VersionRepository versionRepository;

    @Before
    public void setup() {
        this.inMemoryStorageAdapter = InMemoryStorageAdapter.create();
        this.versionRepository = new VersionRepository(inMemoryStorageAdapter);
    }

    /**
     * When you try to get a model version, but there's no metadata for that model,
     * this should fail with an {@link DataStoreException}.
     */
    @Test
    public void emitsErrorForNoMetadataInRepo() {
        // Arrange: no metadata is in the repo.
        BlogOwner blogOwner = BlogOwner.builder()
            .name("Jameson Williams")
            .build();
        // Note that this line is NOT executed in arrangement.
        // ModelMetadata metadata = new ModelMetadata(blogOwner.getId(), false, 1, Time.now());
        // putInStore(blogOwner blogOwner);

        // Act: try to lookup the metadata. Is it going to work? Duh.
        TestObserver<Integer> observer = versionRepository.findModelVersion(blogOwner).test();
        assertTrue(observer.awaitTerminalEvent(REASONABLE_WAIT_TIME, TimeUnit.MILLISECONDS));

        // Assert: this failed. There was no version available.
        String expectedMessage =
            String.format(Locale.US, "Wanted 1 metadata for item with id = %s, but had 0.", blogOwner.getId());
        observer
            .assertError(DataStoreException.class)
            .assertErrorMessage(expectedMessage);
    }

    /**
     * When you try to get the version for a model, and there is metadata for the model
     * in the DataStore, BUT the version info is not populated, this should return an
     * {@link DataStoreException}.
     * @throws DataStoreException
     *         NOT EXPECTED. This happens on failure to arrange data before test action.
     *         The expected DataStoreException is communicated via callback, not thrown
     *         on the calling thread. It's a different thing than this.
     */
    @Test
    public void emitsErrorWhenMetadataHasNullVersion() throws DataStoreException {
        // Arrange a model an metadata into the store, but the metadtaa doesn't contain a valid version
        BlogOwner blogOwner = BlogOwner.builder()
            .name("Jameson")
            .build();
        ModelMetadata metadata = new ModelMetadata(blogOwner.getId(), null, null, null);
        putInStore(blogOwner, metadata);

        // Act: try to get the version.
        TestObserver<Integer> observer = versionRepository.findModelVersion(blogOwner).test();
        assertTrue(observer.awaitTerminalEvent(REASONABLE_WAIT_TIME, TimeUnit.MILLISECONDS));

        // Assert: the single emitted a DataStoreException.
        String expectedMessage =
            String.format(Locale.US, "Metadata for item with id = %s had null version.", blogOwner.getId());
        observer
            .assertError(DataStoreException.class)
            .assertErrorMessage(expectedMessage);
    }

    /**
     * When there is metadata for a model in the store, and that metadata includes a version -
     * for heaven's sake, man - do please emit the dang thing.
     * @throws DataStoreException On failure to arrange data into store
     */
    @SuppressWarnings("checkstyle:MagicNumber") // 1_000 is magic. Big. Whooping. Deal.
    @Test
    public void emitsSuccessWithValueWhenVersionInStore() throws DataStoreException {
        // Arrange versioning info into the store.
        BlogOwner owner = BlogOwner.builder()
            .name("Jameson")
            .build();
        int expectedVersion = new Random().nextInt(1_000);
        putInStore(new ModelMetadata(owner.getId(), false, expectedVersion, Time.now()));

        // Act! Try to obtain it via the Versioning Repository.
        TestObserver<Integer> observer = versionRepository.findModelVersion(owner).test();
        assertTrue(observer.awaitTerminalEvent(REASONABLE_WAIT_TIME, TimeUnit.MILLISECONDS));

        // Assert: we got a version.
        observer
            .assertNoErrors()
            .assertComplete()
            .assertValue(expectedVersion);
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    private final <T extends Model> void putInStore(@NonNull T... models) throws DataStoreException {
        Objects.requireNonNull(models);
        for (T model : models) {
            Await.<StorageItemChange.Record, DataStoreException>result((onResult, onError) ->
                inMemoryStorageAdapter.save(model, StorageItemChange.Initiator.DATA_STORE_API, onResult, onError)
            );
        }
    }
}
