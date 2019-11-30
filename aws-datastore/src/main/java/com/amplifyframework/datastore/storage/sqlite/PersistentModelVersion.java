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

import io.reactivex.Single;

/**
 * A {@link Model} that will encapsulate the {@link ModelProvider#version()}
 * that will be persisted using the {@link LocalStorageAdapter}. The version
 * stored will be checked with the version available through the
 * {@link ModelProvider#version()} for detecting version changes.
 */
final class PersistentModelVersion implements Model {

    private static final String TAG = PersistentModelVersion.class.getSimpleName();

    // A static identifier that is used to store the version of model.
    private static final String STATIC_IDENTIFIER_FOR_VERSION = "version-in-local-storage";

    @ModelField(targetType = "ID", isRequired = true)
    private final String id;

    @ModelField(targetType = "String", isRequired = true)
    private final String version;

    /**
     * Construct the PersistentModelVersion object.
     * @param version version of the {@link com.amplifyframework.core.model.ModelProvider}
     */
    PersistentModelVersion(@NonNull String version) {
        Objects.requireNonNull(version);
        this.id = STATIC_IDENTIFIER_FOR_VERSION;
        this.version = version;
    }

    /**
     * Read the PersistentModelVersion stored by the LocalStorageAdapter on disk.
     * @param localStorageAdapter storage adapter that is used to query the data from disk.
     * @return a Single that emits the PersistentModelVersion read from disk upon success and
     *         error upon failure
     */
    static Single<Iterator<PersistentModelVersion>> fromLocalStorage(@NonNull LocalStorageAdapter localStorageAdapter) {
        return Single.create(emitter -> {
            final ResultListener<Iterator<PersistentModelVersion>> queryListener =
                    new ResultListener<Iterator<PersistentModelVersion>>() {
                @Override
                public void onResult(Iterator<PersistentModelVersion> result) {
                    emitter.onSuccess(result);
                }

                @Override
                public void onError(Throwable error) {
                    emitter.onError(error);
                }
            };
            localStorageAdapter.query(PersistentModelVersion.class, queryListener);
        });
    }

    /**
     * Persist the version object through the localStorageAdapter.
     * @param localStorageAdapter persists the version object
     * @param persistentModelVersion the object to be persisted
     * return a Single that emits the PersistentModelVersion read from disk upon success and
     *        error upon failure
     */
    static PersistentModelVersion saveToLocalStorage(@NonNull LocalStorageAdapter localStorageAdapter,
                                                             @NonNull PersistentModelVersion persistentModelVersion) {
        return Single.<PersistentModelVersion>create(emitter -> {
            final ResultListener<StorageItemChange.Record> saveListener =
                    new ResultListener<StorageItemChange.Record>() {
                @Override
                public void onResult(StorageItemChange.Record record) {
                    emitter.onSuccess(persistentModelVersion);
                }

                @Override
                public void onError(Throwable error) {
                    emitter.onError(error);
                }
            };
            localStorageAdapter.save(
                    persistentModelVersion,
                    StorageItemChange.Initiator.DATA_STORE_API,
                    saveListener);
        }).blockingGet();
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
