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

package com.amplifyframework.predictions.aws;

import android.content.Context;
import androidx.annotation.NonNull;

import com.amplifyframework.core.Consumer;
import com.amplifyframework.predictions.PredictionsException;
import com.amplifyframework.predictions.PredictionsPlugin;
import com.amplifyframework.predictions.aws.operation.AWSInterpretOperation;
import com.amplifyframework.predictions.aws.operation.AWSTranslateTextOperation;
import com.amplifyframework.predictions.aws.request.AWSComprehendRequest;
import com.amplifyframework.predictions.aws.request.AWSTranslateRequest;
import com.amplifyframework.predictions.aws.service.AWSPredictionsService;
import com.amplifyframework.predictions.models.LanguageType;
import com.amplifyframework.predictions.operation.InterpretOperation;
import com.amplifyframework.predictions.operation.TranslateTextOperation;
import com.amplifyframework.predictions.options.InterpretOptions;
import com.amplifyframework.predictions.options.TranslateTextOptions;
import com.amplifyframework.predictions.result.InterpretResult;
import com.amplifyframework.predictions.result.TranslateTextResult;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A plugin for the predictions category.
 */
public final class AWSPredictionsPlugin extends PredictionsPlugin<AWSPredictionsEscapeHatch> {
    private static final String AWS_PREDICTIONS_PLUGIN_KEY = "awsPredictionsPlugin";

    private final ExecutorService executorService;

    private AWSPredictionsPluginConfiguration configuration;
    private AWSPredictionsService predictionsService;

    /**
     * Constructs the AWS Predictions Plugin initializing the executor service.
     */
    public AWSPredictionsPlugin() {
        this.executorService = Executors.newCachedThreadPool();
    }

    @NonNull
    @Override
    public String getPluginKey() {
        return AWS_PREDICTIONS_PLUGIN_KEY;
    }

    @Override
    public void configure(JSONObject pluginConfiguration, @NonNull Context context) throws PredictionsException {
        this.configuration = AWSPredictionsPluginConfiguration.fromJson(pluginConfiguration);
        this.predictionsService = new AWSPredictionsService(configuration);
    }

    @NonNull
    @Override
    public AWSPredictionsEscapeHatch getEscapeHatch() {
        return new AWSPredictionsEscapeHatch(
                predictionsService.getTranslateClient(),
                predictionsService.getComprehendClient()
        );
    }

    @NonNull
    @Override
    public TranslateTextOperation<?> translateText(
            @NonNull String text,
            @NonNull Consumer<TranslateTextResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        final TranslateTextOptions options = TranslateTextOptions.defaults();
        return translateText(text, options, onSuccess, onError);
    }

    @NonNull
    @Override
    public TranslateTextOperation<?> translateText(
            @NonNull String text,
            @NonNull TranslateTextOptions options,
            @NonNull Consumer<TranslateTextResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        // Create translate request for AWS Translate
        AWSTranslateRequest request = new AWSTranslateRequest(text);

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
    public TranslateTextOperation<?> translateText(
            @NonNull String text,
            @NonNull LanguageType fromLanguage,
            @NonNull LanguageType toLanguage,
            @NonNull Consumer<TranslateTextResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        final TranslateTextOptions options = TranslateTextOptions.defaults();
        return translateText(text, fromLanguage, toLanguage, options, onSuccess, onError);
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
    public InterpretOperation<?> interpret(
            @NonNull String text,
            @NonNull Consumer<InterpretResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        final InterpretOptions options = InterpretOptions.defaults();
        return interpret(text, options, onSuccess, onError);
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
