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
import android.content.res.AssetFileDescriptor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.amplifyframework.core.Consumer;
import com.amplifyframework.predictions.PredictionsException;
import com.amplifyframework.predictions.tensorflow.adapter.SentimentTypeAdapter;
import com.amplifyframework.predictions.models.Sentiment;
import com.amplifyframework.predictions.models.SentimentType;
import com.amplifyframework.predictions.result.InterpretResult;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An implementation of text classification service using
 * pre-trained model from Tensorflow lite.
 */
final class TFLiteTextClassificationService {
    private static final String SERVICE_KEY = "textClassifier";
    private static final String MODEL_PATH = "text_classification.tflite";
    private static final String DICTIONARY_PATH = "text_classification_vocab.txt";
    private static final String LABEL_PATH = "text_classification_labels.txt";

    // The maximum length of an input sentence.
    private static final int SENTENCE_LEN = 256;

    // Percentage multiplier
    private static final int PERCENT = 100;

    // Simple delimiter to split words.
    private static final String SIMPLE_SPACE_OR_PUNCTUATION = "[ ,.!?\n]";

    /*
     * Reserved values in ImdbDataSet dictionary:
     * dictionary["<PAD>"] = 0      used for padding
     * dictionary["<START>"] = 1    mark for the start of a sentence
     * dictionary["<UNKNOWN>"] = 2  mark for unknown words (OOV)
     */
    private static final String PAD = "<PAD>";
    private static final String START = "<START>";
    private static final String UNKNOWN = "<UNKNOWN>";

    private final Map<String, Integer> dictionary = new HashMap<>();
    private final List<String> labels = new ArrayList<>();

    private final Context context;

    private Interpreter tflite;
    private AtomicBoolean loaded;

    /**
     * Constructs an instance of service to perform text
     * sentiment interpretation using Tensorflow Lite
     * interpreter.
     * @param context the Android context for loading model
     */
    TFLiteTextClassificationService(Context context) {
        this.context = context;
        this.loaded = new AtomicBoolean(false);

        try {
            // Try loading assets now if possible
            loadIfNotLoaded();
        } catch (PredictionsException exception) {
            // Ignore if it fails to load during configuration.

            // This may sound weird, but we want the model to be loaded in as soon as possible.
            // But we don't want to throw an error that needs to be caught at configuration time,
            // since these models are supposed to be optional.
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
        final Sentiment sentiment;
        try {
            loadIfNotLoaded();
            sentiment = fetchSentiment(text);
        } catch (PredictionsException exception) {
            onError.accept(exception);
            return;
        }

        InterpretResult result = InterpretResult.builder()
                .sentiment(sentiment)
                .build();
        onSuccess.accept(result);
    }

    private Sentiment fetchSentiment(String text) throws PredictionsException {
        float[][] input;
        float[][] output;

        // Pre-process input text
        try {
            input = tokenizeInputText(text);
            output = new float[1][labels.size()];
        } catch (IllegalArgumentException exception) {
            throw new PredictionsException(
                    "Tensorflow Lite failed to make inference.",
                    exception,
                    "Verify that the assets are loaded."
            );
        }

        // Run inference.
        tflite.run(input, output);

        // Find the predominant sentiment
        Sentiment sentiment = null;
        for (int i = 0; i < labels.size(); i++) {
            SentimentType sentimentType = SentimentTypeAdapter.fromTensorflow(labels.get(i));
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

    @SuppressWarnings("ConstantConditions")
    private float[][] tokenizeInputText(String text) {
        float[] tmp = new float[SENTENCE_LEN];

        int index = 0;
        tmp[index++] = dictionary.get(START);

        for (String word : text.split(SIMPLE_SPACE_OR_PUNCTUATION)) {
            if (index >= SENTENCE_LEN) {
                break;
            }

            tmp[index++] = dictionary.containsKey(word)
                    ? dictionary.get(word)
                    : dictionary.get(UNKNOWN);
        }

        // Padding and wrapping.
        Arrays.fill(tmp, index, SENTENCE_LEN - 1, dictionary.get(PAD));
        return new float[][]{tmp};
    }

    @WorkerThread
    private synchronized void loadIfNotLoaded() throws PredictionsException {
        if (loaded.get()) {
            return;
        }
        loadModel();
        loadDictionary();
        loadLabels();
        loaded.set(true);
    }

    @WorkerThread
    private synchronized void loadModel() throws PredictionsException {
        try {
            ByteBuffer buffer = loadModelFile(context);
            tflite = new Interpreter(buffer);
        } catch (IOException exception) {
            throw new PredictionsException(
                    "Error encountered while loading models.",
                    exception,
                    "Please verify that " + MODEL_PATH + " is present inside assets directory."
            );
        }
    }

    @WorkerThread
    private synchronized void loadDictionary() throws PredictionsException {
        try {
            loadDictionaryFile(context);
        } catch (IOException exception) {
            throw new PredictionsException(
                    "Error encountered while loading dictionary.",
                    exception,
                    "Please verify that " + DICTIONARY_PATH + " is present inside assets directory."
            );
        } catch (IllegalArgumentException exception) {
            throw new PredictionsException(
                    "Loaded dictionary does not contain required keywords.",
                    exception,
                    "Please verify the validity of the dictionary asset."
            );
        }
    }

    @WorkerThread
    private synchronized void loadLabels() throws PredictionsException {
        try {
            loadLabelFile(context);
        } catch (IOException exception) {
            throw new PredictionsException(
                    "Error encountered while loading labels.",
                    exception,
                    "Please verify that " + LABEL_PATH + " is present inside assets directory."
            );
        }
    }

    private MappedByteBuffer loadModelFile(Context context) throws IOException {
        try (
                AssetFileDescriptor fileDescriptor = context.getAssets().openFd(MODEL_PATH);
                FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor())
        ) {
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        }
    }

    private void loadLabelFile(Context context) throws IOException {
        try (
                InputStream inputStream = context.getAssets().open(LABEL_PATH);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))
        ) {
            // Each line in the label file is a label.
            while (reader.ready()) {
                labels.add(reader.readLine());
            }
        }
    }

    private void loadDictionaryFile(Context context) throws IOException {
        try (
                InputStream inputStream = context.getAssets().open(DICTIONARY_PATH);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))
        ) {
            // Each line in the dictionary has two columns.
            // First column is a word, and the second is the index of this word.
            while (reader.ready()) {
                List<String> line = Arrays.asList(reader.readLine().split(" "));
                if (line.size() < 2) {
                    continue;
                }
                dictionary.put(line.get(0), Integer.parseInt(line.get(1)));
            }
        }

        // Make sure that the reserved words are there
        if (dictionary.get(PAD) == null) {
            throw new IllegalArgumentException("Reserved word for padding \"<PAD>\" not found.");
        }
        if (dictionary.get(START) == null) {
            throw new IllegalArgumentException("Reserved word for start indicator \"<START>\" not found.");
        }
        if (dictionary.get(UNKNOWN) == null) {
            throw new IllegalArgumentException("Reserved word for unknown word \"<UNKNOWN>\" not found.");
        }
    }

    /**
     * Closes Tensorflow lite interpreter and clears
     * in-memory assets data to free up resources.
     */
    @WorkerThread
    synchronized void close() {
        if (tflite != null) {
            tflite.close();
        }
        dictionary.clear();
        labels.clear();
        loaded.set(false);
    }

    /**
     * Returns the interpreter with a pre-trained model
     * to detect predominant sentiment from a given text.
     * Null if the model was not loaded properly.
     * @return the interpreter with trained model
     */
    @Nullable
    Interpreter getInterpreter() {
        return tflite;
    }
}
