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

package com.amplifyframework.datastore.appsync;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.temporal.Temporal;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.testmodels.commentsblog.Post;
import com.amplifyframework.testmodels.commentsblog.PostStatus;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * A bucket for some test data that is cumbersome/verbose to write out.
 */
@SuppressWarnings("checkstyle:JavadocVariable")
public final class TestModelWithMetadataInstances {
    public static final ModelWithMetadata<BlogOwner> BLOGGER_JAMESON =
        new ModelWithMetadata<>(
            BlogOwner.builder()
                .name("Jameson")
                .id("d5b44350-b8e9-4deb-94c2-7fe986d6a0e1")
                .build(),
            new ModelMetadata("d5b44350-b8e9-4deb-94c2-7fe986d6a0e1",
                    null,
                    3,
                    new Temporal.Timestamp(223344L, TimeUnit.SECONDS))
        );
    public static final ModelWithMetadata<BlogOwner> BLOGGER_ISLA =
        new ModelWithMetadata<>(
            BlogOwner.builder()
                .name("Isla")
                .id("c0601168-2931-4bc0-bf13-5963cd31f828")
                .build(),
            new ModelMetadata("c0601168-2931-4bc0-bf13-5963cd31f828",
                    null,
                    11,
                    new Temporal.Timestamp(998877L, TimeUnit.SECONDS))
        );
    public static final ModelWithMetadata<Post> DRUM_POST =
        new ModelWithMetadata<>(
            Post.builder()
                .title("Inactive Post About Drums")
                .status(PostStatus.INACTIVE)
                .rating(3)
                .id("83ceb757-c8c8-4b6a-bee0-a43afb53a73a")
                .build(),
            new ModelMetadata("83ceb757-c8c8-4b6a-bee0-a43afb53a73a",
                    null,
                    5,
                    new Temporal.Timestamp(123123L, TimeUnit.SECONDS))
        );
    public static final ModelWithMetadata<Post> DELETED_DRUM_POST =
        new ModelWithMetadata<>(
            DRUM_POST.getModel(),
            new ModelMetadata("83ceb757-c8c8-4b6a-bee0-a43afb53a73a",
                    Boolean.TRUE,
                    5,
                    new Temporal.Timestamp(123123L, TimeUnit.SECONDS))
        );

    private TestModelWithMetadataInstances() {}

    /**
     * Asserts that two collections of {@link ModelWithMetadata} are equals.
     * This is a WORKAROUND until equals() and hashCode() work correctly for these.
     * @param expected Expected collection
     * @param actual Actual collection to compare to expected
     */
    public static void assertEquals(
        // They're "equal" if they deal with the same item ID... for now...
        // (it's not actually true, though, this is not a sufficient equality condition.)

        Collection<ModelWithMetadata<? extends Model>> expected,
        Collection<ModelWithMetadata<? extends Model>> actual) {

        final Set<String> actualModelIds = new HashSet<>();
        for (final ModelWithMetadata<? extends Model> modelWithMetadata : actual) {
            actualModelIds.add(modelWithMetadata.getModel().getId());
        }
        final Set<String> expectedModelIds = new HashSet<>();
        for (final ModelWithMetadata<? extends Model> modelWithMetadata : expected) {
            expectedModelIds.add(modelWithMetadata.getModel().getId());
        }
        org.junit.Assert.assertEquals(expectedModelIds, actualModelIds);
    }
}
