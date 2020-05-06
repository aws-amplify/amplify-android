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

/**
 * Types of voice to synthesize speech with.
 *
 * @see <a href=https://docs.aws.amazon.com/polly/latest/dg/voicelist.html>Voices in Amazon Polly</a>
 */
@SuppressWarnings("JavadocVariable")
public enum VoiceType {
    ZEINA("Zeina", "arb"),
    ZHIYU("Zhiyu", "cmn-CN"),
    NAJA("Naja", "da-DK"),
    MADS("Mads", "da-DK"),
    LOTTE("Lotte", "nl-NL"),
    RUBEN("Ruben", "nl-NL"),
    NICOLE("Nicole", "en-AU"),
    RUSSELL("Russell", "en-AU"),
    AMY("Amy", "en-GB"),
    EMMA("Emma", "en-GB"),
    BRIAN("Brian", "en-GB"),
    RAVEENA("Raveena", "en-IN"),
    IVY("Ivy", "en-US"),
    JOANNA("Joanna", "en-US"),
    KENDRA("Kendra", "en-US"),
    KIMBERLY("Kimberly", "en-US"),
    SALLI("Salli", "en-US"),
    JOEY("Joey", "en-US"),
    JUSTIN("Justin", "en-US"),
    MATTHEW("Matthew", "en-US"),
    GERAINT("Geraint", "en-GB-WLS"),
    CELINE("Celine", "fr-FR"),
    LEA("Lea", "fr-FR"),
    MATHIEU("Mathieu", "fr-FR"),
    CHANTAL("Chantal", "fr-CA"),
    MARLENE("Marlene", "de-DE"),
    VICKI("Vicki", "de-DE"),
    HANS("Hans", "de-DE"),
    ADITI("Aditi", "hi-IN"),
    DORA("Dora", "is-IS"),
    KARL("Karl", "is-IS"),
    CARLA("Carla", "it-IT"),
    BIANCA("Bianca", "it-IT"),
    GIORGIO("Giorgio", "it-IT"),
    MIZUKI("Mizuki", "ja-JP"),
    TAKUMI("Takumi", "ja-JP"),
    SEOYEON("Seoyeon", "ko-KR"),
    LIV("Liv", "nb-NO"),
    EWA("Ewa", "pl-PL"),
    MAJA("Maja", "pl-PL"),
    JACEK("Jacek", "pl-PL"),
    JAN("Jan", "pl-PL"),
    CAMILA("Camila", "pt-BR"),
    VITORIA("Vitoria", "pt-BR"),
    RICARDO("Ricardo", "pt-BR"),
    INES("Ines", "pt-PT"),
    CRISTIANO("Cristiano", "pt-PT"),
    CARMEN("Carmen", "ro-RO"),
    TATYANA("Tatyana", "ru-RU"),
    MAXIM("Maxim", "ru-RU"),
    CONCHITA("Conchita", "es-ES"),
    LUCIA("Lucia", "es-ES"),
    ENRIQUE("Enrique", "es-ES"),
    MIA("Mia", "es-MX"),
    LUPE("Lupe", "es-US"),
    PENELOPE("Penelope", "es-US"),
    MIGUEL("Miguel", "es-US"),
    ASTRID("Astrid", "sv-SE"),
    FILIZ("Filiz", "tr-TR"),
    GWYNETH("Gwyneth", "cy-GB"),
    UNKNOWN("unknown", "unknown");

    private final String name;
    private final String languageCode;

    VoiceType(String name, String languageCode) {
        this.name = name;
        this.languageCode = languageCode;
    }

    /**
     * Obtains a {@link VoiceType} enum value from voice name.
     * If there is no equivalent enum for the given name, then a
     * {@link VoiceType#UNKNOWN} will be returned.
     * @param name Name of the voice to obtain equivalent enum from
     * @return An enum value of matching voice type
     */
    @NonNull
    public static VoiceType fromName(@Nullable String name) {
        if (name == null) {
            return UNKNOWN;
        }
        for (VoiceType voiceType : values()) {
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
