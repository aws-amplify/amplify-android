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
import com.amplifyframework.testutils.Await;

import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test that input conversion to TensorFlow Lite tokens
 * works as intended.
 */
public final class InputTokenizerTest {
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
        final String inputText = "Where is the bathroom?";
        final InputStream stream = new FileInputStream("src/test/resources/word-tokens.txt");

        // Make mock context return mock asset manager
        when(mockContext.getAssets()).thenReturn(mockAssets);

        // Make mock asset manager return pre-determined input stream
        when(mockAssets.open(anyString())).thenReturn(stream);

        // Load!! (from mock assets)
        TextClassificationDictionary dictionary = new TextClassificationDictionary(mockContext);
        Map<String, Integer> tokens = Await.<Map<String, Integer>, PredictionsException>result(
            (onResult, onError) -> {
                dictionary.onLoaded(onResult, onError);
                dictionary.load();
            }
        );

        // Assert that load was successful
        assertEquals(tokens, dictionary.getValue());

        // Tokenize input
        float[][] input = dictionary.tokenizeInputText(inputText);
        float[][] expected = new float[1][256]; // 256 = Max sentence size
        expected[0][0] = 1; // <START>
        expected[0][1] = 2; // Where (<UNKNOWN>)
        expected[0][2] = 9; // is
        expected[0][3] = 4; // the
        expected[0][4] = 2; // bathroom (<UNKNOWN>)
        // Followed by 0's (<PAD>)

        assertArrayEquals(expected, input);
    }
}
