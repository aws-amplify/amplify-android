/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.predictions.aws.configuration

import com.amplifyframework.predictions.aws.NetworkPolicy
import com.amplifyframework.predictions.models.LanguageType
import org.json.JSONObject

/**
 * Configures the behavior for text interpretation.
 */
class TranslateTextConfiguration private constructor(
    /**
     * Gets the default language to translate from.
     * @return the default source language
     */
    val sourceLanguage: LanguageType,
    /**
     * Gets the default language to translate to.
     * @return the default target language
     */
    val targetLanguage: LanguageType,
    /**
     * Gets the type of network policy for resource access.
     * @return the network policy type
     */
    val networkPolicy: NetworkPolicy
) {
    companion object {
        private const val CONFIG_NAME = "translateText"

        @JvmStatic
        fun fromJson(configurationJson: JSONObject): TranslateTextConfiguration? {
            if (!configurationJson.has(CONFIG_NAME)) {
                return null
            }

            val translateTextJson = configurationJson.getJSONObject(CONFIG_NAME)
            val sourceLangCode = translateTextJson.getString("sourceLang")
            val targetLangCode = translateTextJson.getString("targetLang")
            val networkPolicyString = translateTextJson.getString("defaultNetworkPolicy")

            val sourceLanguage = LanguageType.from(sourceLangCode)
            val targetLanguage = LanguageType.from(targetLangCode)
            val networkPolicy = NetworkPolicy.fromKey(networkPolicyString)

            return TranslateTextConfiguration(
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                networkPolicy = networkPolicy
            )
        }
    }
}
