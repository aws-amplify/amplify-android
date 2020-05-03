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

package com.amplifyframework.predictions.aws.configuration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.predictions.aws.NetworkPolicy;
import com.amplifyframework.predictions.models.LanguageType;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Configures the behavior for text interpretation.
 */
public final class TranslateTextConfiguration {
    private static final String CONFIG_NAME = "translateText";
    private final LanguageType sourceLanguage;
    private final LanguageType targetLanguage;
    private final NetworkPolicy networkPolicy;

    private TranslateTextConfiguration(
            LanguageType sourceLanguage,
            LanguageType targetLanguage,
            NetworkPolicy networkPolicy
    ) {
        this.sourceLanguage = sourceLanguage;
        this.targetLanguage = targetLanguage;
        this.networkPolicy = networkPolicy;
    }

    /**
     * Construct an instance of {@link TranslateTextConfiguration} from
     * plugin configuration JSON object.
     * @param configurationJson the plugin configuration
     * @return the configuration for text translation
     * @throws JSONException if translate configuration is malformed
     */
    @Nullable
    public static TranslateTextConfiguration fromJson(@NonNull JSONObject configurationJson) throws JSONException {
        if (!configurationJson.has(CONFIG_NAME)) {
            return null;
        }

        JSONObject translateTextJson = configurationJson.getJSONObject(CONFIG_NAME);
        String sourceLangCode = translateTextJson.getString("sourceLang");
        String targetLangCode = translateTextJson.getString("targetLang");
        String networkPolicyString = translateTextJson.getString("defaultNetworkPolicy");

        final LanguageType sourceLanguage = LanguageType.from(sourceLangCode);
        final LanguageType targetLanguage = LanguageType.from(targetLangCode);
        final NetworkPolicy networkPolicy = NetworkPolicy.fromKey(networkPolicyString);

        return new TranslateTextConfiguration(sourceLanguage, targetLanguage, networkPolicy);
    }

    /**
     * Gets the default language to translate from.
     * @return the default source language
     */
    @NonNull
    public LanguageType getSourceLanguage() {
        return sourceLanguage;
    }

    /**
     * Gets the default language to translate to.
     * @return the default target language
     */
    @NonNull
    public LanguageType getTargetLanguage() {
        return targetLanguage;
    }

    /**
     * Gets the type of network policy for resource access.
     * @return the network policy type
     */
    @NonNull
    public NetworkPolicy getNetworkPolicy() {
        return networkPolicy;
    }
}
