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

package com.amplifyframework.predictions.aws.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.predictions.models.VoiceType;

/**
 * Types of voice to synthesize speech with.
 *
 * @see <a href=https://docs.aws.amazon.com/polly/latest/dg/voicelist.html>Voices in Amazon Polly</a>
 */
@SuppressWarnings("JavadocVariable")
public enum AWSVoiceType implements VoiceType {
    ARABIC_ZEINA("Zeina", "arb"),
    MANDARIN_CHINESE_ZHIYU("Zhiyu", "cmn-CN"),
    DANISH_NAJA("Naja", "da-DK"),
    DANISH_MADS("Mads", "da-DK"),
    DUTCH_LOTTE("Lotte", "nl-NL"),
    DUTCH_RUBEN("Ruben", "nl-NL"),
    AUSTRALIAN_ENGLISH_NICOLE("Nicole", "en-AU"),
    AUSTRALIAN_ENGLISH_RUSSELL("Russell", "en-AU"),
    BRITISH_ENGLISH_AMY("Amy", "en-GB"),
    BRITISH_ENGLISH_EMMA("Emma", "en-GB"),
    BRITISH_ENGLISH_BRIAN("Brian", "en-GB"),
    INDIAN_ENGLISH_RAVEENA("Raveena", "en-IN"),
    INDIAN_ENGLISH_ADITI("Aditi", "en-IN"),
    US_ENGLISH_IVY("Ivy", "en-US"),
    US_ENGLISH_JOANNA("Joanna", "en-US"),
    US_ENGLISH_KENDRA("Kendra", "en-US"),
    US_ENGLISH_KIMBERLY("Kimberly", "en-US"),
    US_ENGLISH_SALLI("Salli", "en-US"),
    US_ENGLISH_JOEY("Joey", "en-US"),
    US_ENGLISH_JUSTIN("Justin", "en-US"),
    US_ENGLISH_MATTHEW("Matthew", "en-US"),
    WELSH_ENGLISH_GERAINT("Geraint", "en-GB-WLS"),
    FRENCH_CELINE("Celine", "fr-FR"),
    FRENCH_LEA("Lea", "fr-FR"),
    FRENCH_MATHIEU("Mathieu", "fr-FR"),
    CANADIAN_FRENCH_CHANTAL("Chantal", "fr-CA"),
    GERMAN_MARLENE("Marlene", "de-DE"),
    GERMAN_VICKI("Vicki", "de-DE"),
    GERMAN_HANS("Hans", "de-DE"),
    HINDI_ADITI("Aditi", "hi-IN"),
    ICELANDIC_DORA("Dora", "is-IS"),
    ICELANDIC_KARL("Karl", "is-IS"),
    ITALIAN_CARLA("Carla", "it-IT"),
    ITALIAN_BIANCA("Bianca", "it-IT"),
    ITALIAN_GIORGIO("Giorgio", "it-IT"),
    JAPANESE_MIZUKI("Mizuki", "ja-JP"),
    JAPANESE_TAKUMI("Takumi", "ja-JP"),
    KOREAN_SEOYEON("Seoyeon", "ko-KR"),
    NORWEGIAN_LIV("Liv", "nb-NO"),
    POLISH_EWA("Ewa", "pl-PL"),
    POLISH_MAJA("Maja", "pl-PL"),
    POLISH_JACEK("Jacek", "pl-PL"),
    POLISH_JAN("Jan", "pl-PL"),
    BRAZILIAN_PORTUGUESE_CAMILA("Camila", "pt-BR"),
    BRAZILIAN_PORTUGUESE_VITORIA("Vitoria", "pt-BR"),
    BRAZILIAN_PORTUGUESE_RICARDO("Ricardo", "pt-BR"),
    EUROPEAN_PORTUGUESE_INES("Ines", "pt-PT"),
    EUROPEAN_PORTUGUESE_CRISTIANO("Cristiano", "pt-PT"),
    ROMANIAN_CARMEN("Carmen", "ro-RO"),
    RUSSIAN_TATYANA("Tatyana", "ru-RU"),
    RUSSIAN_MAXIM("Maxim", "ru-RU"),
    EUROPEAN_SPANISH_CONCHITA("Conchita", "es-ES"),
    EUROPEAN_SPANISH_LUCIA("Lucia", "es-ES"),
    EUROPEAN_SPANISH_ENRIQUE("Enrique", "es-ES"),
    MEXICAN_SPANISH_MIA("Mia", "es-MX"),
    US_SPANISH_LUPE("Lupe", "es-US"),
    US_SPANISH_PENELOPE("Penelope", "es-US"),
    US_SPANISH_MIGUEL("Miguel", "es-US"),
    SWEDISH_ASTRID("Astrid", "sv-SE"),
    TURKISH_FILIZ("Filiz", "tr-TR"),
    WELSH_GWYNETH("Gwyneth", "cy-GB"),
    UNKNOWN("unknown", "unknown");

    private final String name;
    private final String languageCode;

    AWSVoiceType(String name, String languageCode) {
        this.name = name;
        this.languageCode = languageCode;
    }

    /**
     * Obtains a {@link AWSVoiceType} enum value from voice type.
     * If there is no equivalent enum for the given voice, then a
     * {@link AWSVoiceType#UNKNOWN} will be returned.
     * @param voice voice to obtain equivalent enum from
     * @return An enum value of matching voice type
     */
    @NonNull
    public static AWSVoiceType fromVoice(@Nullable VoiceType voice) {
        if (voice == null) {
            return UNKNOWN;
        }

        // Directly cast and return if instance of AWSVoiceType
        if (voice instanceof AWSVoiceType) {
            return (AWSVoiceType) voice;
        }

        return fromName(voice.getName());
    }

    /**
     * Obtains a {@link AWSVoiceType} enum value from voice name.
     * If there is no equivalent enum for the given name, then a
     * {@link AWSVoiceType#UNKNOWN} will be returned.
     * @param name name of the voice to obtain equivalent enum from
     * @return An enum value of matching voice name
     */
    @NonNull
    public static AWSVoiceType fromName(@Nullable String name) {
        if (name == null) {
            return UNKNOWN;
        }

        for (AWSVoiceType voiceType : values()) {
            if (voiceType.getName().equals(name)) {
                return voiceType;
            }
        }
        return UNKNOWN;
    }

    /**
     * Gets the owner of the voice.
     * @return the name of the voice
     */
    @NonNull
    public String getName() {
        return name;
    }

    /**
     * Gets the associated language code of this voice.
     * @return the language code and speaker's origin
     */
    @NonNull
    public String getLanguageCode() {
        return languageCode;
    }
}
