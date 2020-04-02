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

package com.amplifyframework.predictions.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Enum of different types of language. These are the languages
 * that are recognized and supported by Amazon Translate Service
 * and Amazon Comprehend Service.
 *
 * @see <a href=https://docs.aws.amazon.com/translate/latest/dg/what-is.html>What is Amazon Translate?</a>
 * @see <a href=https://docs.aws.amazon.com/comprehend/latest/dg/how-languages.html>Detect the Dominant Language</a>
 */
@SuppressWarnings("JavadocVariable")
public enum LanguageType {
    AFRIKAANS("af"),
    ALBANIAN("sq"),
    AMHARIC("am"),
    ARABIC("ar"),
    ARMENIAN("hy"),
    ASSAMESE("as"),
    AZERBAIJANI("az"),
    BASHKIR("ba"),
    BASQUE("eu"),
    BELARUSIAN("be"),
    BENGALI("bn"),
    BOSNIAN("bs"),
    BULGARIAN("bg"),
    BURMESE("my"),
    CATALAN("ca"),
    CEBUANO("ceb"),
    CENTRAL_KHMER("km"),
    CHINESE_SIMPLIFIED("zh"),
    CHINESE_TRADITIONAL("zh-TW"),
    CHUVASH("cv"),
    CROATIAN("hr"),
    CZECH("cs"),
    DANISH("da"),
    DARI("fa-AF"),
    DUTCH("nl"),
    ENGLISH("en"),
    ESPERANTO("eo"),
    ESTONIAN("et"),
    FINNISH("fi"),
    FRENCH("fr"),
    FRENCH_CANADIAN("fr-CA"),
    GALICIAN("gl"),
    GEORGIAN("ka"),
    GERMAN("de"),
    GREEK("el"),
    GUJARATI("gu"),
    HAITIAN("ht"),
    HAUSA("ha"),
    HEBREW("he"),
    HINDI("hi"),
    HUNGARIAN("hu"),
    ICELANDIC("is"),
    ILOKO("ilo"),
    INDONESIAN("id"),
    IRISH("ga"),
    ITALIAN("it"),
    JAPANESE("ja"),
    JAVANESE("jv"),
    KANNADA("kn"),
    KAZAKH("kk"),
    KIRGHIZ("ky"),
    KOREAN("ko"),
    KURDISH("ku"),
    LATIN("la"),
    LATVIAN("lv"),
    LITHUANIAN("lt"),
    LUXEMBOURGISH("lb"),
    MACEDONIAN("mk"),
    MALAGASY("mg"),
    MALAY("ms"),
    MALAYALAM("ml"),
    MARATHI("mr"),
    MONGOLIAN("mn"),
    NEPALI("ne"),
    NEWARI("new"),
    NORWEGIAN("no"),
    ORIYA("or"),
    PASHTO("ps"),
    PERSIAN("fa"),
    POLISH("pl"),
    PORTUGUESE("pt"),
    PUNJABI("pa"),
    PUSHTO("ps"),
    QUECHUA("qu"),
    ROMANIAN("ro"),
    RUSSIAN("ru"),
    SANSKRIT("sa"),
    SCOTTISH_GAELIC("gd"),
    SERBIAN("sr"),
    SINDHI("sd"),
    SINHALA("si"),
    SLOVAK("sk"),
    SLOVENIAN("sl"),
    SOMALI("so"),
    SPANISH("es"),
    SUNDANESE("su"),
    SWAHILI("sw"),
    SWEDISH("sv"),
    TAGALOG("tl"),
    TAJIK("tg"),
    TAMIL("ta"),
    TATAR("tt"),
    TELUGU("te"),
    THAI("th"),
    TURKISH("tr"),
    TURKMEN("tk"),
    UIGHUR("ug"),
    UKRAINIAN("uk"),
    URDU("ur"),
    UZBEK("uz"),
    VIETNAMESE("vi"),
    WELSH("cy"),
    YIDDISH("yi"),
    YORUBA("yo"),
    UNKNOWN("unknown");

    private static final Map<String, LanguageType> CODES;

    private final String languageCode;

    // Reverse look-up table
    static {
        CODES = new HashMap<>();
        for (LanguageType language : LanguageType.values()) {
            CODES.put(language.getLanguageCode(), language);
        }
    }

    LanguageType(String languageCode) {
        this.languageCode = languageCode;
    }

    /**
     * Obtains a {@link LanguageType} enum value from language code.
     * If there is no equivalent enum for the given language code, a
     * {@link LanguageType#UNKNOWN} will be returned.
     * @param languageCode Language code to obtain equivalent enum from
     * @return An enum value of matching language code
     */
    public static LanguageType from(String languageCode) {
        if (!CODES.containsKey(languageCode)) {
            return UNKNOWN;
        }
        return CODES.get(languageCode);
    }

    /**
     * Get the language code for given enum value.
     * @return the language code
     */
    public String getLanguageCode() {
        return languageCode;
    }
}
