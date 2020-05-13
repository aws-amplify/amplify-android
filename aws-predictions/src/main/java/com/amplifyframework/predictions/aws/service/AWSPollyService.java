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
import com.amplifyframework.predictions.aws.configuration.SpeechGeneratorConfiguration;
import com.amplifyframework.predictions.aws.models.AWSVoiceType;
import com.amplifyframework.predictions.result.TextToSpeechResult;
import com.amplifyframework.util.UserAgent;

import com.amazonaws.AmazonClientException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.services.polly.AmazonPollyClient;
import com.amazonaws.services.polly.AmazonPollyPresigningClient;
import com.amazonaws.services.polly.model.OutputFormat;
import com.amazonaws.services.polly.model.SynthesizeSpeechRequest;
import com.amazonaws.services.polly.model.SynthesizeSpeechResult;
import com.amazonaws.services.polly.model.TextType;

import java.io.InputStream;

/**
 * Predictions service for performing text to speech conversion.
 */
final class AWSPollyService {
    private static final int MP3_SAMPLE_RATE = 24_000;

    private final AmazonPollyClient polly;
    private final AWSPredictionsPluginConfiguration pluginConfiguration;

    AWSPollyService(AWSPredictionsPluginConfiguration pluginConfiguration) {
        this.polly = createPollyClient();
        this.pluginConfiguration = pluginConfiguration;
    }

    private AmazonPollyClient createPollyClient() {
        AWSCredentialsProvider credentialsProvider = AWSMobileClient.getInstance();
        ClientConfiguration configuration = new ClientConfiguration();
        configuration.setUserAgent(UserAgent.string());
        return new AmazonPollyPresigningClient(credentialsProvider, configuration);
    }

    void synthesizeSpeech(
            @NonNull String text,
            @NonNull AWSVoiceType voiceType,
            @NonNull Consumer<TextToSpeechResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        try {
            InputStream data = synthesizeSpeech(text, voiceType);
            onSuccess.accept(TextToSpeechResult.fromAudioData(data));
        } catch (PredictionsException exception) {
            onError.accept(exception);
        }
    }

    private InputStream synthesizeSpeech(String text, AWSVoiceType voiceType) throws PredictionsException {
        final String languageCode;
        final String voiceId;
        if (AWSVoiceType.UNKNOWN.equals(voiceType)) {
            // Obtain voice + language from plugin configuration by default
            SpeechGeneratorConfiguration config = pluginConfiguration.getSpeechGeneratorConfiguration();
            languageCode = config.getLanguage();
            voiceId = config.getVoice();
        } else {
            // Override configuration defaults if explicitly specified in the options
            languageCode = voiceType.getLanguageCode();
            voiceId = voiceType.getName();
        }

        SynthesizeSpeechRequest request = new SynthesizeSpeechRequest()
                 .withText(text)
                 .withTextType(TextType.Text)
                 .withLanguageCode(languageCode)
                 .withVoiceId(voiceId)
                 .withOutputFormat(OutputFormat.Mp3)
                 .withSampleRate(Integer.toString(MP3_SAMPLE_RATE));

        // Synthesize speech from given text via Amazon Polly
        final SynthesizeSpeechResult result;
        try {
            result = polly.synthesizeSpeech(request);
        } catch (AmazonClientException serviceException) {
            throw new PredictionsException(
                    "AWS Polly encountered an error while synthesizing speech.",
                    serviceException, "See attached service exception for more details."
            );
        }

        return result.getAudioStream();
    }

    @NonNull
    AmazonPollyClient getClient() {
        return polly;
    }
}
