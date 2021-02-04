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

import com.amplifyframework.core.model.Model;
import com.amplifyframework.datastore.DataStoreConflictHandler.ConflictData;
import com.amplifyframework.datastore.DataStoreConflictHandler.ConflictResolutionDecision;
import com.amplifyframework.datastore.DataStoreConflictHandler.ResolutionStrategy;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;

import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Single;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Tests the {@link DataStoreConflictHandler}.
 */
public final class DataStoreConflictHandlerTest {
    private static final long TIMEOUT_SECONDS = 2;

    /**
     * The always-apply-remote conflict handler will respond to a conflict by
     * suggesting the {@link ResolutionStrategy#APPLY_REMOTE} strategy.
     */
    @Test
    public void alwaysApplyRemote() {
        ConflictData<BlogOwner> conflictedSusan =
            ConflictData.create(BloggerData.LOCAL_SUSAN, BloggerData.REMOTE_SUSAN);
        Single
            .<ConflictResolutionDecision<? extends Model>>create(emitter ->
                DataStoreConflictHandler.alwaysApplyRemote()
                    .onConflictDetected(conflictedSusan, emitter::onSuccess)
            )
            .test()
            .awaitDone(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .assertValue(ConflictResolutionDecision.applyRemote());
    }

    /**
     * The always-retry-local conflict handler always decides to use the
     * {@link ResolutionStrategy#RETRY_LOCAL} strategy.
     */
    @Test
    public void alwaysRetryLocal() {
        ConflictData<BlogOwner> conflictedSusan =
            ConflictData.create(BloggerData.LOCAL_SUSAN, BloggerData.REMOTE_SUSAN);
        Single
            .<ConflictResolutionDecision<? extends Model>>create(emitter ->
                DataStoreConflictHandler.alwaysRetryLocal()
                    .onConflictDetected(conflictedSusan, emitter::onSuccess)
            )
            .test()
            .awaitDone(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .assertValue(ConflictResolutionDecision.retryLocal());
    }

    /**
     * Alike ConflictData should evaluate as equals(), and should have the
     * same hashCode().
     */
    @Test
    public void conflictDataEqualsAndHashForAlikeObjects() {
        ConflictData<BlogOwner> first =
            ConflictData.create(BloggerData.LOCAL_SUSAN, BloggerData.REMOTE_SUSAN);
        ConflictData<BlogOwner> second =
            ConflictData.create(BloggerData.LOCAL_SUSAN, BloggerData.REMOTE_SUSAN);

        // Similar conflict data are equals().
        assertEquals(first, second);

        // Similar conflict data have the same hashCode().
        Set<ConflictData<BlogOwner>> set = new HashSet<>();
        set.add(first);
        set.add(second);
        assertEquals(1, set.size());
    }

    /**
     * Dissimilar ConflictData should not be equals(), and should evaluate
     * with difference hashCode().
     */
    @Test
    public void conflictDataEqualsAndHashForDissimilarObjects() {
        // Arrange two conflict data that are not similar.
        ConflictData<BlogOwner> conflictedSusan =
            ConflictData.create(BloggerData.LOCAL_SUSAN, BloggerData.REMOTE_SUSAN);
        ConflictData<BlogOwner> susansInAgreement =
            ConflictData.create(BloggerData.REMOTE_SUSAN, BloggerData.REMOTE_SUSAN);

        // Assert: they are not equals.
        assertNotEquals(conflictedSusan, susansInAgreement);

        // Assert: they have different hashCode(), as shown through their
        // behavior when used in a HashSet.
        Set<ConflictData<BlogOwner>> set = new HashSet<>();
        set.add(conflictedSusan);
        set.add(susansInAgreement);
        assertEquals(2, set.size());
    }

    /**
     * equals() and hashCode() work as expected on a ConflictResolutionDecision,
     * when the objects are alike.
     */
    @Test
    public void conflictResolutionDecisionEqualsAndHashForLikeObjects() {
        // Arrange two objects that are not the same, but have equal content.
        ConflictResolutionDecision<BlogOwner> first =
            ConflictResolutionDecision.retry(BloggerData.LOCAL_SUSAN);
        ConflictResolutionDecision<BlogOwner> second =
            ConflictResolutionDecision.retry(BloggerData.LOCAL_SUSAN);

        // The alike objects should compare as equal.
        assertEquals(first, second);

        // Test hashCode() via a HashSet...
        Set<ConflictResolutionDecision<BlogOwner>> set = new HashSet<>();
        set.add(first);
        set.add(second);
        assertEquals(1, set.size());
        assertEquals(Collections.singleton(first), set);
        assertEquals(Collections.singleton(second), set);
    }

    /**
     * equals() and hashCode() work as expected on a ConflictResolutionDecision,
     * when the objects are *NOT* alike.
     */
    @Test
    public void conflictResolutionDecisionEqualsAndHashForDissimilarObjects() {
        ConflictResolutionDecision<BlogOwner> retryLocalDecision =
            ConflictResolutionDecision.retryLocal();
        ConflictResolutionDecision<BlogOwner> applyRemoteDecision =
            ConflictResolutionDecision.applyRemote();

        // Test equals() by direct comparison of unlike objects...
        assertNotEquals(retryLocalDecision, applyRemoteDecision);

        // Test hashCode() via a HashSet...
        Set<ConflictResolutionDecision<BlogOwner>> set = new HashSet<>();
        set.add(retryLocalDecision);
        set.add(applyRemoteDecision);
        assertEquals(2, set.size());
    }

    /**
     * A bundle of some object data that can be used as expected test data.
     * There is a ModelMetadata that is shared by both local and remote representations
     * of a model. "Susan" is a fictitious blogger. Locally, she is known as "Local Susan",
     * and on the server, she is known as "Remote Susan". All other data agrees.
     * ModelWithMetadata is provided for both copies.
     */
    private static final class BloggerData {
        private static final BlogOwner LOCAL_SUSAN = BlogOwner.builder()
            .name("Local Susan")
            .build();

        private static final BlogOwner REMOTE_SUSAN = LOCAL_SUSAN.copyOfBuilder()
            .name("Remote Susan")
            .build();
    }
}
