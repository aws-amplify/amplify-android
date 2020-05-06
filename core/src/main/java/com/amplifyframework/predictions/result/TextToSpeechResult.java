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

import java.io.InputStream;
import java.util.Objects;

/**
 * The result of the call to synthesize speech from text.
 */
public final class TextToSpeechResult implements Result {
    private final InputStream audioData;

    private TextToSpeechResult(InputStream audioData) {
        this.audioData = audioData;
    }

    /**
     * Constructs a text to speech result containing audio output
     * data of the synthesized speech.
     * @param audioData the audio data containing synthesized speech
     * @return the result instance of text to speech operation
     */
    @NonNull
    public static TextToSpeechResult fromAudioData(@NonNull InputStream audioData) {
        return new TextToSpeechResult(Objects.requireNonNull(audioData));
    }

    /**
     * Gets the outputted audio data.
     * @return the audio data
     */
    @NonNull
    public InputStream getAudioData() {
        return audioData;
    }
}
