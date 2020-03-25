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

package com.amplifyframework.predictions.result;

import androidx.annotation.NonNull;

import com.amplifyframework.predictions.models.EntityMatch;
import com.amplifyframework.util.Immutable;

import java.util.List;
import java.util.Objects;

/**
 * The result of the call to identify entity matching from an image.
 */
public final class IdentifyEntityMatchesResult implements IdentifyResult {
    private final List<EntityMatch> entityMatches;

    private IdentifyEntityMatchesResult(List<EntityMatch> entityMatches) {
        this.entityMatches = entityMatches;
    }

    /**
     * Gets the list of detected entity matches.
     * @return the list of entity matches
     */
    @NonNull
    public List<EntityMatch> getEntityMatches() {
        return Immutable.of(entityMatches);
    }

    /**
     * Constructs a new instance of {@link IdentifyEntityMatchesResult} from
     * the given list of entity matches.
     * @param entityMatches A list of entity matches
     * @return The result instance containing the given list of entity matches
     */
    @NonNull
    public static IdentifyEntityMatchesResult fromEntities(@NonNull List<EntityMatch> entityMatches) {
        return new IdentifyEntityMatchesResult(Objects.requireNonNull(entityMatches));
    }
}
