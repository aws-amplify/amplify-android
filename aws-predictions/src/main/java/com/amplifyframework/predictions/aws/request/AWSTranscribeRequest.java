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

package com.amplifyframework.predictions.aws.request;

import androidx.annotation.NonNull;

import java.io.InputStream;
import java.util.Objects;

/**
 * Simple request instance for speech to text operation.
 */
public final class AWSTranscribeRequest {
    private final InputStream speech;

    /**
     * Constructs an instance of {@link AWSTranscribeRequest}.
     * @param speech the speech audio data to transcribe to text
     */
    public AWSTranscribeRequest(@NonNull InputStream speech) {
        this.speech = Objects.requireNonNull(speech);
    }

    /**
     * Gets the speech to transcribe.
     * @return the input speech
     */
    @NonNull
    public InputStream getSpeech() {
        return speech;
    }
}
