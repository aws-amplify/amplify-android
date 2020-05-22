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

package com.amplifyframework.predictions.aws.service;

import androidx.annotation.NonNull;

import com.amplifyframework.core.Consumer;
import com.amplifyframework.predictions.PredictionsException;
import com.amplifyframework.predictions.aws.AWSPredictionsPluginConfiguration;
import com.amplifyframework.predictions.models.LanguageType;
import com.amplifyframework.predictions.result.TranslateTextResult;
import com.amplifyframework.util.UserAgent;

import com.amazonaws.AmazonClientException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.translate.AmazonTranslateClient;
import com.amazonaws.services.translate.model.TranslateTextRequest;

/**
 * Predictions service for performing text translation.
 */
final class AWSTranslateService {
    private final AmazonTranslateClient translate;
    private final AWSPredictionsPluginConfiguration pluginConfiguration;

    AWSTranslateService(@NonNull AWSPredictionsPluginConfiguration pluginConfiguration,
                        @NonNull AWSCredentialsProvider credentialsProvider) {
        this.translate = createTranslateClient(credentialsProvider);
        this.pluginConfiguration = pluginConfiguration;
    }

    private AmazonTranslateClient createTranslateClient(@NonNull AWSCredentialsProvider credentialsProvider) {
        ClientConfiguration configuration = new ClientConfiguration();
        configuration.setUserAgent(UserAgent.string());
        return new AmazonTranslateClient(credentialsProvider, configuration);
    }

    void translate(
            @NonNull String text,
            @NonNull LanguageType sourceLanguage,
            @NonNull LanguageType targetLanguage,
            @NonNull Consumer<TranslateTextResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        try {
            onSuccess.accept(fetchTranslation(text, sourceLanguage, targetLanguage));
        } catch (PredictionsException exception) {
            onError.accept(exception);
        }
    }

    private TranslateTextResult fetchTranslation(
            String text,
            LanguageType sourceLanguage,
            LanguageType targetLanguage
    ) throws PredictionsException {
        // Throw if default language is not configured
        LanguageType source = !LanguageType.UNKNOWN.equals(sourceLanguage) ? sourceLanguage
                : pluginConfiguration.getTranslateTextConfiguration().getSourceLanguage();
        LanguageType target = !LanguageType.UNKNOWN.equals(targetLanguage) ? targetLanguage
                : pluginConfiguration.getTranslateTextConfiguration().getTargetLanguage();

        TranslateTextRequest request = new TranslateTextRequest()
                .withText(text)
                .withSourceLanguageCode(source.getLanguageCode())
                .withTargetLanguageCode(target.getLanguageCode());

        // Translate given text via AWS Translate
        final com.amazonaws.services.translate.model.TranslateTextResult result;
        try {
            result = translate.translateText(request);
        } catch (AmazonClientException serviceException) {
            throw new PredictionsException(
                    "AWS Translate encountered an error while translating text.",
                    serviceException, "See attached service exception for more details."
            );
        }

        String translation = result.getTranslatedText();
        String targetCode = result.getTargetLanguageCode();
        LanguageType language = LanguageType.from(targetCode);
        return TranslateTextResult.builder()
                .translatedText(translation)
                .targetLanguage(language)
                .build();
    }

    @NonNull
    AmazonTranslateClient getClient() {
        return translate;
    }
}
