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

package com.amplifyframework.datastore;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.temporal.Temporal;
import com.amplifyframework.datastore.appsync.ModelMetadata;
import com.amplifyframework.datastore.appsync.ModelWithMetadata;
import com.amplifyframework.datastore.model.SimpleModelProvider;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testutils.sync.SynchronousDataStore;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Single;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertEquals;

/**
 * Tests the {@link ApplyRemoteConflictHandler}.
 */
@RunWith(RobolectricTestRunner.class)
public final class ApplyRemoteConflictHandlerTest {
    /**
     * When the handler is invoked, it is supplied with the remote and local
     * copies of the conflicting model. After it is done with its work,
     * the local database should contain the remote version, and not the (old)
     * local version anymore.
     * @throws AmplifyException During arrangement of DataStore, etc.
     */
    @Test
    public void remoteModelOverridesLocal() throws AmplifyException {
        ShadowLog.stream = System.out;

        ModelProvider modelProvider = SimpleModelProvider.withRandomVersion(BlogOwner.class);
        DataStoreCategory dataStore = new DataStoreCategory();
        dataStore.addPlugin(new AWSDataStorePlugin(modelProvider));
        dataStore.configure(new DataStoreCategoryConfiguration(), getApplicationContext());
        dataStore.initialize(getApplicationContext());
        SynchronousDataStore synchronousDataStore = SynchronousDataStore.delegatingTo(dataStore);

        // Prepare an apply-remote conflict handler
        ApplyRemoteConflictHandler conflictHandler =
            ApplyRemoteConflictHandler.instance(dataStore, DefaultDataStoreErrorHandler.instance());

        // Arrange some local content
        BlogOwner localModel = BlogOwner.builder()
            .name("Local Blogger")
            .build();
        Temporal.Timestamp now = Temporal.Timestamp.now();
        ModelMetadata localMetadata = new ModelMetadata(localModel.getId(), false, 1, now);
        ModelWithMetadata<BlogOwner> local = new ModelWithMetadata<>(localModel, localMetadata);

        synchronousDataStore.save(localModel);

        // Arrange some remote content
        BlogOwner remoteModel = BlogOwner.builder()
            .name("Remote blogger")
            .id(localModel.getId())
            .build();
        ModelMetadata remoteMetadata = new ModelMetadata(remoteModel.getId(), false, 1, now);
        ModelWithMetadata<BlogOwner> remote = new ModelWithMetadata<>(remoteModel, remoteMetadata);

        // Invoke the handler, with the provided conflict data
        DataStoreConflictData<BlogOwner> conflictData = DataStoreConflictData.create(local, remote);
        Single.create(emitter -> conflictHandler.resolveConflict(conflictData, emitter::onSuccess))
            .test()
            .awaitDone(5, TimeUnit.SECONDS)
            .assertValue(DataStoreConflictHandlerResult.APPLY_REMOTE);

        assertEquals(
            remote.getModel(),
            synchronousDataStore.get(BlogOwner.class, local.getModel().getId())
        );
    }
}
