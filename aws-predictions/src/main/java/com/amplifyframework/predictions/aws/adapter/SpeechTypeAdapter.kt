/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.predictions.aws.adapter

import aws.sdk.kotlin.services.comprehend.model.PartOfSpeechTagType
import com.amplifyframework.predictions.models.SpeechType

/**
 * Utility to convert AWS Comprehend's part of speech type
 * into Amplify-compatible data structure
 * (i.e. [SpeechType]).
 */
object SpeechTypeAdapter {
    /**
     * Converts the part of speech tag returned by AWS Comprehend
     * into a format supported by Amplify Predictions.
     * @param tag Speech type tag returned by AWS Comprehend
     * @return Amplify's [SpeechType] enum
     */
    @JvmStatic
    fun fromComprehend(tag: String): SpeechType {
        return when (PartOfSpeechTagType.fromValue(tag)) {
            PartOfSpeechTagType.Adj -> SpeechType.ADJECTIVE
            PartOfSpeechTagType.Adp -> SpeechType.ADPOSITION
            PartOfSpeechTagType.Adv -> SpeechType.ADVERB
            PartOfSpeechTagType.Aux -> SpeechType.AUXILIARY
            PartOfSpeechTagType.Cconj -> SpeechType.COORDINATING_CONJUNCTION
            PartOfSpeechTagType.Det -> SpeechType.DETERMINER
            PartOfSpeechTagType.Intj -> SpeechType.INTERJECTION
            PartOfSpeechTagType.Noun -> SpeechType.NOUN
            PartOfSpeechTagType.Num -> SpeechType.NUMERAL
            PartOfSpeechTagType.Part -> SpeechType.PARTICLE
            PartOfSpeechTagType.Pron -> SpeechType.PRONOUN
            PartOfSpeechTagType.Propn -> SpeechType.PROPER_NOUN
            PartOfSpeechTagType.Punct -> SpeechType.PUNCTUATION
            PartOfSpeechTagType.Sconj -> SpeechType.SUBORDINATING_CONJUNCTION
            PartOfSpeechTagType.Sym -> SpeechType.SYMBOL
            PartOfSpeechTagType.Verb -> SpeechType.VERB
            else -> SpeechType.UNKNOWN
        }
    }
}
