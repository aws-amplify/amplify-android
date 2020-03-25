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

import com.amplifyframework.predictions.models.CelebrityDetails;
import com.amplifyframework.util.Immutable;

import java.util.List;
import java.util.Objects;

/**
 * The result of the call to identify celebrities from an image.
 */
public final class IdentifyCelebritiesResult implements IdentifyResult {
    private final List<CelebrityDetails> celebrities;

    private IdentifyCelebritiesResult(List<CelebrityDetails> celebrities) {
        this.celebrities = celebrities;
    }

    /**
     * Gets the list of detected celebrities.
     * @return the list of celebrities
     */
    @NonNull
    public List<CelebrityDetails> getCelebrities() {
        return Immutable.of(celebrities);
    }

    /**
     * Constructs a new instance of {@link IdentifyCelebritiesResult} from
     * the given list of celebrities.
     * @param celebrities A list of celebrities
     * @return The result instance containing the given list of celebrities
     */
    @NonNull
    public static IdentifyCelebritiesResult fromCelebrities(@NonNull List<CelebrityDetails> celebrities) {
        return new IdentifyCelebritiesResult(Objects.requireNonNull(celebrities));
    }
}
