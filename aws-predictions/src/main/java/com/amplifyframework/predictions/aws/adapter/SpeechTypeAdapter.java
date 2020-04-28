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

import com.amazonaws.services.comprehend.model.PartOfSpeechTagType;

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
        PartOfSpeechTagType type = PartOfSpeechTagType.fromValue(tag);
        switch (type) {
            case ADJ:
                return SpeechType.ADJECTIVE;
            case ADP:
                return SpeechType.ADPOSITION;
            case ADV:
                return SpeechType.ADVERB;
            case AUX:
                return SpeechType.AUXILIARY;
            case CCONJ:
                return SpeechType.COORDINATING_CONJUNCTION;
            case DET:
                return SpeechType.DETERMINER;
            case INTJ:
                return SpeechType.INTERJECTION;
            case NOUN:
                return SpeechType.NOUN;
            case NUM:
                return SpeechType.NUMERAL;
            case PART:
                return SpeechType.PARTICLE;
            case PRON:
                return SpeechType.PRONOUN;
            case PROPN:
                return SpeechType.PROPER_NOUN;
            case PUNCT:
                return SpeechType.PUNCTUATION;
            case SCONJ:
                return SpeechType.SUBORDINATING_CONJUNCTION;
            case SYM:
                return SpeechType.SYMBOL;
            case VERB:
                return SpeechType.VERB;
            case O:
            default:
                return SpeechType.UNKNOWN;
        }
    }
}
