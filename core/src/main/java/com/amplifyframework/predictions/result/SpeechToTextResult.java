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

import com.amplifyframework.core.async.Result;

import java.util.Objects;

/**
 * The result of the call to transcribe speech to text.
 */
public final class SpeechToTextResult implements Result {
    private final String transcription;

    private SpeechToTextResult(String transcription) {
        this.transcription = transcription;
    }

    /**
     * Return the transcribed text.
     * @return the transcribed text
     */
    @NonNull
    public String getTranscription() {
        return transcription;
    }

    /**
     * Creates a new result instance containing the transcribed text.
     * @param text The transcribed text
     * @return Result instance containing the transcribed text
     */
    @NonNull
    public static SpeechToTextResult fromText(@NonNull String text) {
        return new SpeechToTextResult(Objects.requireNonNull(text));
    }
}
