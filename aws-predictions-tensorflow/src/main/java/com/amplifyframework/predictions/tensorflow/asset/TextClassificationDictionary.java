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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads the list of words and their numerical indices that the
 * text classifier can recognize.
 *
 * The file being loaded should be in the format of:
 * <pre>
 * &#60;PAD&#62; 0
 * &#60;START&#62; 1
 * &#60;UNKNOWN&#62; 2
 * word 3
 * word 4
 * ...
 * </pre>
 */
public class TextClassificationDictionary implements Loadable<Map<String, Integer>, PredictionsException> {
    private static final String DICTIONARY_PATH = "text_classification_vocab.txt";

    // The maximum length of an input sentence.
    private static final int MAX_SENTENCE_LENGTH = 256;

    // Simple delimiter to split words.
    private static final String DELIMITER_REGEX = "[ ,.!?\n]";

    /*
     * Reserved values in ImdbDataSet dictionary:
     * dictionary["<PAD>"] = 0      used for padding
     * dictionary["<START>"] = 1    mark for the start of a sentence
     * dictionary["<UNKNOWN>"] = 2  mark for unknown words (OOV)
     */
    private static final String PAD = "<PAD>";
    private static final String START = "<START>";
    private static final String UNKNOWN = "<UNKNOWN>";

    private final AssetManager assets;
    private final Map<String, Integer> dictionary;

    private Consumer<Map<String, Integer>> onLoaded;
    private Consumer<PredictionsException> onError;
    private Action onUnloaded;
    private boolean loaded;

    /**
     * Constructs a loader for text classification dictionary.
     * @param context the Android context
     */
    public TextClassificationDictionary(@NonNull Context context) {
        this.assets = context.getAssets();
        this.dictionary = new HashMap<>();
    }

    /**
     * From TensorFlow Lite example code.
     * Pre-processes the input text to be compatible with the model's shape.
     * @param text input text to tokenize
     * @return 2-D nested float array where the first index represents the
     *          sentence, and the second index represents the tokenized word
     */
    @SuppressWarnings("ConstantConditions")
    public float[][] tokenizeInputText(String text) {
        float[] tmp = new float[MAX_SENTENCE_LENGTH];

        int index = 0;
        tmp[index++] = dictionary.get(START);

        for (String word : text.split(DELIMITER_REGEX)) {
            if (index >= MAX_SENTENCE_LENGTH) {
                break;
            }

            tmp[index++] = dictionary.containsKey(word)
                    ? dictionary.get(word)
                    : dictionary.get(UNKNOWN);
        }

        // Padding and wrapping.
        Arrays.fill(tmp, index, MAX_SENTENCE_LENGTH - 1, dictionary.get(PAD));
        return new float[][]{tmp};
    }

    /**
     * Loads the list of recognizable vocabulary and maps each
     * word to a specific index token.
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
            loadDictionaryFile();
            if (onLoaded != null) {
                onLoaded.accept(dictionary);
            }
            loaded = true;
        } catch (PredictionsException exception) {
            if (onError != null) {
                onError.accept(exception);
            }
        }
    }

    /**
     * Clears the in-memory map of word index tokens.
     */
    @Override
    public synchronized void unload() {
        // No-op if not loaded yet
        if (!loaded) {
            return;
        }

        // Clear the map
        dictionary.clear();

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
    public TextClassificationDictionary onLoaded(Consumer<Map<String, Integer>> onLoaded) {
        this.onLoaded = onLoaded;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TextClassificationDictionary onError(Consumer<PredictionsException> onError) {
        this.onError = onError;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TextClassificationDictionary onUnloaded(Action onUnloaded) {
        this.onUnloaded = onUnloaded;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    @Override
    public Map<String, Integer> value() {
        return dictionary;
    }

    // This code comes from the official TensorFlow Lite sample app
    // https://github.com/tensorflow/examples/tree/master/lite/examples/text_classification/android
    private void loadDictionaryFile() throws PredictionsException {
        try (
                InputStream inputStream = assets.open(DICTIONARY_PATH);
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
        } catch (IOException exception) {
            throw new PredictionsException(
                    "Error encountered while loading dictionary.", exception,
                    "Verify that " + DICTIONARY_PATH + " is present inside the assets directory."
            );
        } catch (IllegalArgumentException exception) {
            throw new PredictionsException(
                    "Loaded dictionary does not contain required keywords.", exception,
                    "Verify the validity of the dictionary asset."
            );
        }

        // Make sure that the reserved words are there
        final String message;
        if (dictionary.get(PAD) == null) {
            message = "Reserved word for padding \"<PAD>\" not found.";
        } else if (dictionary.get(START) == null) {
            message = "Reserved word for start indicator \"<START>\" not found.";
        } else if (dictionary.get(UNKNOWN) == null) {
            message = "Reserved word for unknown word \"<UNKNOWN>\" not found.";
        } else {
            return;
        }
        throw new PredictionsException(message, "Verify the validity of the dictionary asset.");
    }
}
