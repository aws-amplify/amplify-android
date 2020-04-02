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

package com.amplifyframework.predictions.tensorflow.asset;

import android.content.Context;
import android.content.res.AssetManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.amplifyframework.core.Action;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.predictions.PredictionsException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads the list of labels for the feature being detected by
 * text classification model.
 */
public class TextClassificationLabels implements Loadable<List<String>, PredictionsException> {
    private static final String LABELS_PATH = "text_classification_labels.txt";

    private final AssetManager assets;
    private final List<String> labels;

    private Consumer<List<String>> onLoaded;
    private Consumer<PredictionsException> onError;
    private Action onUnloaded;
    private boolean loaded;

    /**
     * Constructs a loader for text classification labels.
     * @param context the Android context
     */
    public TextClassificationLabels(@NonNull Context context) {
        this.assets = context.getAssets();
        this.labels = new ArrayList<>();
    }

    /**
     * Gets the size of the loaded label.
     * @return the size of the label
     */
    public int size() {
        return labels.size();
    }

    /**
     * Gets the label at provided index.
     * @param index the label index
     * @return the label at provided index
     */
    public String get(int index) {
        return labels.get(index);
    }

    /**
     * Loads the list of labels for text classification category.
     */
    @WorkerThread
    @Override
    public synchronized void load() {
        // No-op if loaded already
        if (loaded) {
            return;
        }

        // Load
        try {
            loadLabelsFile();
            if (onLoaded != null) {
                onLoaded.accept(labels);
            }
            loaded = true;
        } catch (PredictionsException exception) {
            if (onError != null) {
                onError.accept(exception);
            }
        }
    }

    /**
     * Clears the in-memory list of labels.
     */
    @Override
    public synchronized void unload() {
        // No-op if not loaded yet
        if (!loaded) {
            return;
        }

        // Clear the list
        labels.clear();

        // Call the action for unloaded if set
        if (onUnloaded != null) {
            onUnloaded.call();
        }
        loaded = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isLoaded() {
        return loaded;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TextClassificationLabels onLoaded(Consumer<List<String>> onLoaded) {
        this.onLoaded = onLoaded;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TextClassificationLabels onError(Consumer<PredictionsException> onError) {
        this.onError = onError;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TextClassificationLabels onUnloaded(Action onUnloaded) {
        this.onUnloaded = onUnloaded;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    @Override
    public List<String> value() {
        return labels;
    }

    // This code comes from the official TensorFlow Lite sample app
    // https://github.com/tensorflow/examples/tree/master/lite/examples/text_classification/android
    private synchronized void loadLabelsFile() throws PredictionsException {
        try (
                InputStream inputStream = assets.open(LABELS_PATH);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))
        ) {
            // Each line in the label file is a label.
            while (reader.ready()) {
                labels.add(reader.readLine());
            }
        } catch (IOException exception) {
            throw new PredictionsException(
                    "Error encountered while loading labels.", exception,
                    "Verify that " + LABELS_PATH + " is present inside the assets directory."
            );
        }
    }
}
