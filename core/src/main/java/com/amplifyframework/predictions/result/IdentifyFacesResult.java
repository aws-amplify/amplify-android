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

import com.amplifyframework.predictions.models.FaceDetails;
import com.amplifyframework.util.Immutable;

import java.util.List;
import java.util.Objects;

/**
 * The result of the call to identify entities from an image.
 */
public final class IdentifyFacesResult implements IdentifyResult {
    private final List<FaceDetails> faces;

    private IdentifyFacesResult(List<FaceDetails> faces) {
        this.faces = faces;
    }

    /**
     * Gets the list of detected faces and other details.
     * @return the list of faces
     */
    @NonNull
    public List<FaceDetails> getFaces() {
        return Immutable.of(faces);
    }

    /**
     * Constructs a new instance of {@link IdentifyFacesResult} from
     * the given list of faces.
     * @param faces A list of detected faces
     * @return The result instance containing the given list of detected faces
     */
    @NonNull
    public static IdentifyFacesResult fromFaces(@NonNull List<FaceDetails> faces) {
        return new IdentifyFacesResult(Objects.requireNonNull(faces));
    }
}
