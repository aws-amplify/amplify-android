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

import com.amplifyframework.predictions.models.Entity;

import java.util.List;
import java.util.Objects;

/**
 * The result of the call to identify entities from an image.
 */
public final class IdentifyEntitiesResult implements IdentifyResult {
    private final List<Entity> entities;

    private IdentifyEntitiesResult(List<Entity> entities) {
        this.entities = entities;
    }

    /**
     * Gets the list of detected entities.
     * @return the list of entities
     */
    @NonNull
    public List<Entity> getEntities() {
        return entities;
    }

    /**
     * Constructs a new instance of {@link IdentifyEntitiesResult} from
     * the given list of entities.
     * @param entities A list of entities
     * @return The result instance containing the given list of entities
     */
    @NonNull
    public static IdentifyEntitiesResult fromEntities(@NonNull List<Entity> entities) {
        return new IdentifyEntitiesResult(Objects.requireNonNull(entities));
    }
}
