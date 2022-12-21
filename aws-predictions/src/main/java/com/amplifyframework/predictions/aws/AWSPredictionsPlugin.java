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

package com.amplifyframework.predictions.aws;

import android.content.Context;
import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.amplifyframework.core.Consumer;
import com.amplifyframework.predictions.PredictionsException;
import com.amplifyframework.predictions.PredictionsPlugin;
import com.amplifyframework.predictions.aws.auth.CognitoCredentialsProvider;
import com.amplifyframework.predictions.aws.models.AWSVoiceType;
import com.amplifyframework.predictions.aws.operation.AWSIdentifyOperation;
import com.amplifyframework.predictions.aws.operation.AWSInterpretOperation;
import com.amplifyframework.predictions.aws.operation.AWSTextToSpeechOperation;
import com.amplifyframework.predictions.aws.operation.AWSTranslateTextOperation;
import com.amplifyframework.predictions.aws.request.AWSComprehendRequest;
import com.amplifyframework.predictions.aws.request.AWSImageIdentifyRequest;
import com.amplifyframework.predictions.aws.request.AWSPollyRequest;
import com.amplifyframework.predictions.aws.request.AWSTranslateRequest;
import com.amplifyframework.predictions.aws.service.AWSPredictionsService;
import com.amplifyframework.predictions.models.IdentifyAction;
import com.amplifyframework.predictions.models.LanguageType;
import com.amplifyframework.predictions.operation.IdentifyOperation;
import com.amplifyframework.predictions.operation.InterpretOperation;
import com.amplifyframework.predictions.operation.TextToSpeechOperation;
import com.amplifyframework.predictions.operation.TranslateTextOperation;
import com.amplifyframework.predictions.options.IdentifyOptions;
import com.amplifyframework.predictions.options.InterpretOptions;
import com.amplifyframework.predictions.options.TextToSpeechOptions;
import com.amplifyframework.predictions.options.TranslateTextOptions;
import com.amplifyframework.predictions.result.IdentifyResult;
import com.amplifyframework.predictions.result.InterpretResult;
import com.amplifyframework.predictions.result.TextToSpeechResult;
import com.amplifyframework.predictions.result.TranslateTextResult;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider;

/**
 * A plugin for the predictions category.
 */
public final class AWSPredictionsPlugin extends PredictionsPlugin<AWSPredictionsEscapeHatch> {
    private static final String AWS_PREDICTIONS_PLUGIN_KEY = "awsPredictionsPlugin";

    private final ExecutorService executorService;

    private AWSPredictionsPluginConfiguration configuration;
    private AWSPredictionsService predictionsService;
    private CredentialsProvider credentialsProviderOverride; // Currently used for integration testing purposes

    /**
     * Constructs the AWS Predictions Plugin initializing the executor service.
     */
    public AWSPredictionsPlugin() {
        this.executorService = Executors.newCachedThreadPool();
    }

    @VisibleForTesting
    AWSPredictionsPlugin(CredentialsProvider credentialsProviderOverride) {
        this();
        this.credentialsProviderOverride = credentialsProviderOverride;
    }

    @NonNull
    @Override
    public String getPluginKey() {
        return AWS_PREDICTIONS_PLUGIN_KEY;
    }

    @Override
    public void configure(JSONObject pluginConfiguration, @NonNull Context context) throws PredictionsException {
        this.configuration = AWSPredictionsPluginConfiguration.fromJson(pluginConfiguration);

        CredentialsProvider credentialsProvider;

        if (credentialsProviderOverride != null) {
            credentialsProvider = credentialsProviderOverride;
        } else {
            try {
                credentialsProvider = new CognitoCredentialsProvider();
            } catch (IllegalStateException exception) {
                throw new PredictionsException(
                        "AWSPredictionsPlugin depends on AWSCognitoAuthPlugin but it is currently missing",
                        exception,
                        "Before configuring Amplify, be sure to add AWSPredictionsPlugin same as you added " +
                                "AWSPinpointAnalyticsPlugin."
                );
            }
        }

        this.predictionsService = new AWSPredictionsService(configuration, credentialsProvider);
    }

    @NonNull
    @Override
    public AWSPredictionsEscapeHatch getEscapeHatch() {
        return new AWSPredictionsEscapeHatch(
                predictionsService.getTranslateClient(),
                predictionsService.getPollyClient(),
                predictionsService.getRekognitionClient(),
                predictionsService.getTextractClient(),
                predictionsService.getComprehendClient()
        );
    }

    @NonNull
    @Override
    public String getVersion() {
        return BuildConfig.VERSION_NAME;
    }

    @NonNull
    @Override
    public TextToSpeechOperation<?> convertTextToSpeech(
            @NonNull String text,
            @NonNull Consumer<TextToSpeechResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        return convertTextToSpeech(text, TextToSpeechOptions.defaults(), onSuccess, onError);
    }

    @NonNull
    @Override
    public TextToSpeechOperation<?> convertTextToSpeech(
            @NonNull String text,
            @NonNull TextToSpeechOptions options,
            @NonNull Consumer<TextToSpeechResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        // Create translate request for Amazon Polly
        AWSVoiceType voiceType = AWSVoiceType.fromVoice(options.getVoiceType());
        AWSPollyRequest request = new AWSPollyRequest(text, voiceType);

        AWSTextToSpeechOperation operation = new AWSTextToSpeechOperation(
                predictionsService,
                executorService,
                request,
                onSuccess,
                onError
        );

        // Start operation and return
        operation.start();
        return operation;
    }

    @NonNull
    @Override
    public TranslateTextOperation<?> translateText(
            @NonNull String text,
            @NonNull Consumer<TranslateTextResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        return translateText(text, TranslateTextOptions.defaults(),
                onSuccess, onError);
    }

    @NonNull
    @Override
    public TranslateTextOperation<?> translateText(
            @NonNull String text,
            @NonNull TranslateTextOptions options,
            @NonNull Consumer<TranslateTextResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        return translateText(text, LanguageType.UNKNOWN,
                LanguageType.UNKNOWN, options, onSuccess, onError);
    }

    @NonNull
    @Override
    public TranslateTextOperation<?> translateText(
            @NonNull String text,
            @NonNull LanguageType fromLanguage,
            @NonNull LanguageType toLanguage,
            @NonNull Consumer<TranslateTextResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        return translateText(text, fromLanguage, toLanguage,
                TranslateTextOptions.defaults(), onSuccess, onError);
    }

    @NonNull
    @Override
    public TranslateTextOperation<?> translateText(
            @NonNull String text,
            @NonNull LanguageType fromLanguage,
            @NonNull LanguageType toLanguage,
            @NonNull TranslateTextOptions options,
            @NonNull Consumer<TranslateTextResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        // Create translate request for AWS Translate
        AWSTranslateRequest request = new AWSTranslateRequest(text, fromLanguage, toLanguage);

        AWSTranslateTextOperation operation = new AWSTranslateTextOperation(
                predictionsService,
                executorService,
                request,
                onSuccess,
                onError
        );

        // Start operation and return
        operation.start();
        return operation;
    }

    @NonNull
    @Override
    public IdentifyOperation<?> identify(
            @NonNull IdentifyAction actionType,
            @NonNull Bitmap image,
            @NonNull Consumer<IdentifyResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        return identify(actionType, image, IdentifyOptions.defaults(), onSuccess, onError);
    }

    @NonNull
    @Override
    public IdentifyOperation<?> identify(
            @NonNull IdentifyAction actionType,
            @NonNull Bitmap image,
            @NonNull IdentifyOptions options,
            @NonNull Consumer<IdentifyResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        // Create identify request for AWS Rekognition/Textract
        AWSImageIdentifyRequest request = AWSImageIdentifyRequest.fromBitmap(image);

        AWSIdentifyOperation operation = new AWSIdentifyOperation(
                predictionsService,
                executorService,
                actionType,
                request,
                onSuccess,
                onError
        );

        // Start operation and return
        operation.start();
        return operation;
    }

    @NonNull
    @Override
    public InterpretOperation<?> interpret(
            @NonNull String text,
            @NonNull Consumer<InterpretResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        return interpret(text, InterpretOptions.defaults(), onSuccess, onError);
    }

    @NonNull
    @Override
    public InterpretOperation<?> interpret(
            @NonNull String text,
            @NonNull InterpretOptions options,
            @NonNull Consumer<InterpretResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError) {
        // Create interpret request for AWS Comprehend
        AWSComprehendRequest request = new AWSComprehendRequest(text);

        AWSInterpretOperation operation = new AWSInterpretOperation(
                predictionsService,
                executorService,
                request,
                onSuccess,
                onError
        );

        // Start operation and return
        operation.start();
        return operation;
    }
}
