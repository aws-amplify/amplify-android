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
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * Simple request instance for speech synthesis operation.
 */
public final class AWSPollyRequest {
    private final String text;
    private final String voiceType;

    /**
     * Constructs an instance of {@link AWSPollyRequest}.
     * @param text the text to synthesize into speech
     * @param voiceType the voice type supported by AWS Polly
     */
    public AWSPollyRequest(
            @NonNull String text,
            @Nullable String voiceType
    ) {
        this.text = Objects.requireNonNull(text);
        this.voiceType = voiceType;
    }

    /**
     * Gets the text to translate.
     * @return the input text
     */
    @NonNull
    public String getText() {
        return text;
    }

    /**
     * Gets the custom voice type for speech.
     * @return the voice type
     */
    @Nullable
    public String getVoiceType() {
        return voiceType;
    }
}
