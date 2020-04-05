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

package com.amplifyframework.predictions.tensorflow.service;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.amplifyframework.core.Consumer;
import com.amplifyframework.predictions.PredictionsException;
import com.amplifyframework.predictions.models.Sentiment;
import com.amplifyframework.predictions.models.SentimentType;
import com.amplifyframework.predictions.result.InterpretResult;
import com.amplifyframework.predictions.tensorflow.adapter.SentimentTypeAdapter;
import com.amplifyframework.predictions.tensorflow.asset.Loadable;
import com.amplifyframework.predictions.tensorflow.asset.TextClassificationDictionary;
import com.amplifyframework.predictions.tensorflow.asset.TextClassificationLabels;
import com.amplifyframework.predictions.tensorflow.asset.TextClassificationModel;

import org.tensorflow.lite.Interpreter;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * An implementation of text classification service using
 * pre-trained model from TensorFlow Lite.
 */
final class TensorFlowTextClassificationService {
    private static final String SERVICE_KEY = "textClassifier";

    // Percentage multiplier
    private static final int PERCENT = 100;

    private final TextClassificationModel interpreter;
    private final TextClassificationDictionary dictionary;
    private final TextClassificationLabels labels;
    private final List<Loadable<?, PredictionsException>> assets;
    private final CountDownLatch loaded;

    private PredictionsException loadingError;

    /**
     * Constructs an instance of service to perform text
     * sentiment interpretation using TensorFlow Lite
     * interpreter.
     * @param context the Android context
     */
    TensorFlowTextClassificationService(@NonNull Context context) {
        this.interpreter = new TextClassificationModel(context);
        this.dictionary = new TextClassificationDictionary(context);
        this.labels = new TextClassificationLabels(context);

        this.assets = Arrays.asList(interpreter, dictionary, labels);
        this.loaded = new CountDownLatch(assets.size());

        for (Loadable<?, PredictionsException> asset : assets) {
            asset.onLoaded(
                onLoad -> this.loaded.countDown(),
                error -> this.loadingError = error
            );
        }
    }

    /**
     * Gets the associated service key of this service for
     * identification.
     * @return the service key
     */
    @NonNull
    String getServiceKey() {
        return SERVICE_KEY;
    }

    @WorkerThread
    synchronized void loadIfNotLoaded() {
        for (Loadable<?, PredictionsException> asset : assets) {
            asset.load();
        }
    }

    /**
     * Classifies text to analyze associated sentiments.
     * @param text the text to classify
     * @param onSuccess notified when classification succeeds
     * @param onError notified when classification fails
     */
    void classify(
            @NonNull String text,
            @NonNull Consumer<InterpretResult> onSuccess,
            @NonNull Consumer<PredictionsException> onError
    ) {
        // Escape early if the initialization failed
        if (loadingError != null) {
            onError.accept(loadingError);
            return;
        }

        // Wait for initialization to complete
        // TODO: encapsulate blocking logic elsewhere
        try {
            loaded.await();
        } catch (InterruptedException exception) {
            onError.accept(new PredictionsException(
                    "Text classification service initialization was interrupted.",
                    "Please wait for the required assets to be fully loaded."
            ));
        }

        try {
            final Sentiment sentiment = fetchSentiment(text);
            onSuccess.accept(InterpretResult.builder()
                    .sentiment(sentiment)
                    .build());
        } catch (PredictionsException exception) {
            onError.accept(exception);
        }
    }

    private Sentiment fetchSentiment(String text) throws PredictionsException {
        float[][] input;
        float[][] output;

        try {
            // Pre-process input text
            input = dictionary.tokenizeInputText(text);
            output = new float[1][labels.size()];

            // Run inference.
            interpreter.run(input, output);
        } catch (IllegalArgumentException exception) {
            throw new PredictionsException(
                    "TensorFlow Lite failed to make an inference.",
                    exception,
                    "Verify that the label size matches the output size of the model."
            );
        }

        // Find the predominant sentiment
        Sentiment sentiment = null;
        for (int i = 0; i < labels.size(); i++) {
            SentimentType sentimentType = SentimentTypeAdapter.fromTensorFlow(labels.get(i));
            float confidenceScore = output[0][i] * PERCENT;
            if (sentiment == null || sentiment.getConfidence() < confidenceScore) {
                sentiment = Sentiment.builder()
                        .value(sentimentType)
                        .confidence(confidenceScore)
                        .build();
            }
        }
        return sentiment;
    }

    /**
     * Closes TensorFlow Lite interpreter and releases
     * in-memory assets data to free up resources.
     */
    @WorkerThread
    synchronized void release() {
        for (Loadable<?, PredictionsException> asset : assets) {
            asset.unload();
        }
    }

    /**
     * Returns the interpreter with a pre-trained model
     * to detect predominant sentiment from a given text.
     * Null if the model was not loaded properly.
     * @return the interpreter with trained model
     */
    @Nullable
    Interpreter getInterpreter() {
        return interpreter.getValue();
    }
}
