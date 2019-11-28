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

package com.amplifyframework.datastore.storage.sqlite;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.ResultListener;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.annotations.ModelField;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;
import com.amplifyframework.datastore.storage.StorageItemChange;

import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A {@link Model} that will encapsulate the {@link ModelProvider#version()}
 * that will be persisted using the {@link LocalStorageAdapter}. The version
 * stored will be checked with the version available through the
 * {@link ModelProvider#version()} for detecting version changes.
 */
final class PersistentModelVersion implements Model {

    private static final String TAG = PersistentModelVersion.class.getSimpleName();

    @ModelField(targetName = "id", targetType = "ID", isRequired = true)
    private String id;

    @ModelField(targetName = "version", targetType = "String", isRequired = true)
    private String version;

    /**
     * Construct the PersistentModelVersion object.
     * @param uniqueId the unique identifier
     * @param version version of the {@link com.amplifyframework.core.model.ModelProvider}
     */
    PersistentModelVersion(@NonNull String uniqueId, @NonNull String version) {
        Objects.requireNonNull(uniqueId);
        Objects.requireNonNull(version);
        this.id = uniqueId;
        this.version = version;
    }

    /**
     * Read the PersistentModelVersion stored by the LocalStorageAdapter on disk.
     * @param localStorageAdapter storage adapter that is used to query the data from disk.
     * @return PersistentModelVersion object
     */
    static PersistentModelVersion fromLocalStorage(@NonNull LocalStorageAdapter localStorageAdapter) {
        final AtomicReference<PersistentModelVersion> resultRef = new AtomicReference<>();
        final AtomicReference<Throwable> errorRef = new AtomicReference<>();
        final CountDownLatch waitForQuery = new CountDownLatch(1);

        localStorageAdapter.query(
                PersistentModelVersion.class,
                new ResultListener<Iterator<PersistentModelVersion>>() {
                    @Override
                    public void onResult(Iterator<PersistentModelVersion> result) {
                        if (result.hasNext()) {
                            resultRef.set(result.next());
                        }
                        waitForQuery.countDown();
                    }

                    @Override
                    public void onError(Throwable error) {
                        errorRef.set(error);
                        waitForQuery.countDown();
                    }
                });
        try {
            waitForQuery.await();
        } catch (InterruptedException latchInterruptedException) {
            Log.e(TAG, "Waiting to query " + PersistentModelVersion.class.getSimpleName() +
                    " is interrupted.", latchInterruptedException);
        }

        if (resultRef.get() != null) {
            return resultRef.get();
        }

        return null;
    }

    /**
     * Persist the version object through the localStorageAdapter.
     * @param localStorageAdapter persists the version object
     * @param persistentModelVersion the object to be persisted
     */
    static void saveToLocalStorage(@NonNull LocalStorageAdapter localStorageAdapter,
                                   @NonNull PersistentModelVersion persistentModelVersion) {
        localStorageAdapter.save(
                persistentModelVersion,
                StorageItemChange.Initiator.DATA_STORE_API,
                new ResultListener<StorageItemChange.Record>() {
                    @Override
                    public void onResult(StorageItemChange.Record result) {
                        Log.v(TAG, "Successfully written the model version: " + result);
                    }

                    @Override
                    public void onError(Throwable error) {
                        Log.e(TAG, "Error in writing the model version", error);
                    }
                });
    }

    /** {@inheritDoc}. */
    @Override
    public String getId() {
        return this.id;
    }

    /**
     * Return the version of the models.
     * @return the version of the models.
     */
    public String getVersion() {
        return this.version;
    }

    @Override
    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        PersistentModelVersion versionObject = (PersistentModelVersion) thatObject;

        if (!ObjectsCompat.equals(id, versionObject.id)) {
            return false;
        }
        if (!ObjectsCompat.equals(version, versionObject.version)) {
            return false;
        }
        return true;
    }

    @SuppressWarnings("checkstyle:MagicNumber") // 31 is IDE-generated
    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + id.hashCode();
        result = 31 * result + version.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "PersistentModelVersion{" +
                "id='" + id + '\'' +
                ", version='" + version + '\'' +
                '}';
    }
}
