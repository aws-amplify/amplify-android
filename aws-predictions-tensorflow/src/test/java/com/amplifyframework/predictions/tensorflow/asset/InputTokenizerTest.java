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

import com.amplifyframework.predictions.PredictionsException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test that input conversion to TensorFlow Lite tokens
 * works as intended.
 */
@RunWith(JUnit4.class)
public final class InputTokenizerTest {
    private static final long LOAD_TIMEOUT_MS = 100;

    private Context mockContext;
    private AssetManager mockAssets;

    /**
     * Set up mock behavior before each test.
     */
    @Before
    public void setUp() {
        // Mock Android context and asset manager
        mockContext = mock(Context.class);
        mockAssets = mock(AssetManager.class);
    }

    /**
     * Test that text tokenizer converts input text into a 2-D
     * array of equivalent token values and pads the rest with 0's.
     * @throws Exception if dictionary fails to load
     */
    @Test
    @SuppressWarnings("MagicNumber") // word tokens
    public void testInputTextTokenizer() throws Exception {
        final CountDownLatch loaded = new CountDownLatch(1);
        final AtomicReference<Map<String, Integer>> tokens = new AtomicReference<>();
        final AtomicReference<PredictionsException> error = new AtomicReference<>();

        final String inputText = "Where is the bathroom?";
        final InputStream stream = new FileInputStream("src/test/resources/word-tokens.txt");

        // Make mock context return mock asset manager
        when(mockContext.getAssets()).thenReturn(mockAssets);

        // Make mock asset manager return pre-determined input stream
        when(mockAssets.open(anyString())).thenReturn(stream);

        // Load!! (from mock assets)
        TextClassificationDictionary dictionary = new TextClassificationDictionary(mockContext)
                .onLoaded(
                    onLoad -> {
                        loaded.countDown();
                        tokens.set(onLoad);
                    },
                    onError -> {
                        loaded.countDown();
                        error.set(onError);
                    }
                );
        dictionary.load();
        loaded.await(LOAD_TIMEOUT_MS, TimeUnit.MICROSECONDS);
        if (error.get() != null) {
            fail("Failed to load dictionary.");
        }

        // Assert that load was successful
        assertEquals(tokens.get(), dictionary.getValue());

        // Tokenize input
        float[][] input = dictionary.tokenizeInputText(inputText);
        float[][] expected = new float[1][input[0].length];
        expected[0][0] = 1; // <START>
        expected[0][1] = 2; // Where (<UNKNOWN>)
        expected[0][2] = 9; // is
        expected[0][3] = 4; // the
        expected[0][4] = 2; // bathroom (<UNKNOWN>)
        // Followed by 0's (<PAD>)

        assertArrayEquals(expected, input);
    }
}
