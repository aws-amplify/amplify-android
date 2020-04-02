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
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.amplifyframework.core.Action;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.predictions.PredictionsException;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Loads the pre-trained text classification model into
 * a TensorFlow Lite interpreter instance.
 */
public final class TextClassificationModel implements Loadable<Interpreter, PredictionsException> {

    private static final String MODEL_PATH = "text_classification.tflite";

    private final AssetManager assets;

    private Interpreter interpreter;
    private Consumer<Interpreter> onLoaded;
    private Consumer<PredictionsException> onError;
    private Action onUnloaded;
    private boolean loaded;

    /**
     * Constructs a loader for text classification interpreter.
     * @param context the Android context
     */
    public TextClassificationModel(@NonNull Context context) {
        this.assets = context.getAssets();
    }

    /**
     * Executes classification using TensorFlow Lite interpreter with
     * the given input and writes the result onto given output.
     * @param input the input for {@link Interpreter#run(Object, Object)}
     * @param output the output for {@link Interpreter#run(Object, Object)}
     * @throws PredictionsException if the model was not loaded yet
     */
    public void run(Object input, Object output) throws PredictionsException {
        if (interpreter == null) {
            // Blocking logic should prevent this from ever happening
            throw new PredictionsException(
                    "The model is not loaded yet.",
                    "Please wait until the plugin is fully initialized.");
        }
        interpreter.run(input, output);
    }

    /**
     * Loads the pre-trained text classification model into
     * TensorFlow Lite interpreter.
     */
    @WorkerThread
    @Override
    public synchronized void load() {
        // No-op if loaded already
        if (loaded) {
            return;
        }

        try {
            ByteBuffer buffer = loadModelFile();
            interpreter = new Interpreter(buffer);

            if (onLoaded != null) {
                onLoaded.accept(interpreter);
            }
            loaded = true;
        } catch (PredictionsException exception) {
            if (onError != null) {
                onError.accept(exception);
            }
        }
    }

    /**
     * Closes the TensorFlow Lite interpreter.
     */
    @Override
    public synchronized void unload() {
        // No-op if not loaded yet
        if (!loaded) {
            return;
        }

        // Close the interpreter
        interpreter.close();

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
    public TextClassificationModel onLoaded(Consumer<Interpreter> onLoaded) {
        this.onLoaded = onLoaded;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TextClassificationModel onError(Consumer<PredictionsException> onError) {
        this.onError = onError;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TextClassificationModel onUnloaded(Action onUnloaded) {
        this.onUnloaded = onUnloaded;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    @Override
    public Interpreter value() {
        return interpreter;
    }

    // This code comes from the official TensorFlow Lite sample app
    // https://github.com/tensorflow/examples/tree/master/lite/examples/text_classification/android
    private synchronized MappedByteBuffer loadModelFile() throws PredictionsException {
        try (
                AssetFileDescriptor fileDescriptor = assets.openFd(MODEL_PATH);
                FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor())
        ) {
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        } catch (IOException exception) {
            throw new PredictionsException(
                    "Error encountered while loading models.", exception,
                    "Verify that " + MODEL_PATH + " is present inside the assets directory."
            );
        }
    }
}
