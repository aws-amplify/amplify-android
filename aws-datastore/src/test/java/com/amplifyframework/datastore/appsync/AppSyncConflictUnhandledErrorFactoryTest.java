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

package com.amplifyframework.datastore.appsync;

import com.amplifyframework.core.model.temporal.Temporal;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * This is a test for a test utility. 🤯.
 */
public final class AppSyncConflictUnhandledErrorFactoryTest {
    /**
     * Validates that the {@link AppSyncConflictUnhandledErrorFactory} is capable
     * of creating {@link AppSyncConflictUnhandledError}s from {@link ModelWithMetadata}.
     */
    @Test
    public void createUnhandledConflictError() {
        BlogOwner model = BlogOwner.builder()
            .name("Blogger Tony")
            .build();
        Temporal.Timestamp lastChangedAt = new Temporal.Timestamp(1602732606L, TimeUnit.SECONDS);
        ModelMetadata metadata = new ModelMetadata(model.getId(), true, 6, lastChangedAt);
        ModelWithMetadata<BlogOwner> serverData = new ModelWithMetadata<>(model, metadata);
        AppSyncConflictUnhandledError<BlogOwner> error =
            AppSyncConflictUnhandledErrorFactory.createUnhandledConflictError(serverData);
        assertEquals(serverData, error.getServerVersion());
    }
}
