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

package com.amplifyframework.predictions.aws.adapter;

import androidx.annotation.NonNull;

import com.amplifyframework.predictions.models.SpeechType;

import java.util.Locale;

/**
 * Utility to convert AWS Comprehend's part of speech type
 * into Amplify-compatible data structure
 * (i.e. {@link SpeechType}).
 */
public final class SpeechTypeAdapter {
    private SpeechTypeAdapter() {}

    /**
     * Converts the part of speech tag returned by AWS Comprehend
     * into a format supported by Amplify Predictions.
     * @param tag Speech type tag returned by AWS Comprehend
     * @return Amplify's {@link SpeechType} enum
     */
    @NonNull
    public static SpeechType fromComprehend(@NonNull String tag) {
        switch (tag.toLowerCase(Locale.US)) {
            case "adj":
                return SpeechType.ADJECTIVE;
            case "adp":
                return SpeechType.ADPOSITION;
            case "adv":
                return SpeechType.ADVERB;
            case "aux":
                return SpeechType.AUXILIARY;
            case "cconj":
                return SpeechType.COORDINATING_CONJUNCTION;
            case "det":
                return SpeechType.DETERMINER;
            case "intj":
                return SpeechType.INTERJECTION;
            case "noun":
                return SpeechType.NOUN;
            case "num":
                return SpeechType.NUMERAL;
            case "part":
                return SpeechType.PARTICLE;
            case "pron":
                return SpeechType.PRONOUN;
            case "propn":
                return SpeechType.PROPER_NOUN;
            case "punct":
                return SpeechType.PUNCTUATION;
            case "sconj":
                return SpeechType.SUBORDINATING_CONJUNCTION;
            case "sym":
                return SpeechType.SYMBOL;
            case "verb":
                return SpeechType.VERB;
            case "o":
            default:
                return SpeechType.UNKNOWN;
        }
    }
}
