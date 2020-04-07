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

import com.amplifyframework.predictions.models.Sentiment;
import com.amplifyframework.predictions.models.SentimentType;
import com.amplifyframework.predictions.tensorflow.asset.TextClassificationDictionary;
import com.amplifyframework.predictions.tensorflow.asset.TextClassificationLabels;
import com.amplifyframework.predictions.tensorflow.asset.TextClassificationModel;
import com.amplifyframework.testutils.random.RandomString;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test that sentiment detection using TensorFlow Lite interpreter
 * to output Amplify text interpretation result works.
 */
@RunWith(JUnit4.class)
public final class TextClassificationTest {

    private TextClassificationModel mockInterpreter;
    private TextClassificationDictionary mockDictionary;
    private TextClassificationLabels mockLabels;

    private TensorFlowTextClassificationService service;

    /**
     * Set up mock behavior before each test.
     */
    @Before
    public void setUp() {
        // Mock asset loaders
        mockInterpreter = mock(TextClassificationModel.class);
        mockDictionary = mock(TextClassificationDictionary.class);
        mockLabels = mock(TextClassificationLabels.class);

        // Create text classifier with mock asset loaders
        service = new TensorFlowTextClassificationService(
                mockInterpreter,
                mockDictionary,
                mockLabels
        );
    }

    /**
     * Test that fetchSentiment() can do the following.
     *
     *  - Choose the output with highest score,
     *  - obtain the column index,
     *  - obtain the label located at that index,
     *  - convert the label into {@link SentimentType}, and
     *  - return appropriate {@link Sentiment}
     *
     * @throws Exception if sentiment fetch fails
     */
    @Test
    @SuppressWarnings("MagicNumber") // Double comparison delta epsilon
    public void testFetchSentiment() throws Exception {
        // List of labels to expect
        final List<String> labels = Arrays.asList(
                "negative",
                "positive",
                "unknown1",
                "unknown2"
        );
        // Mock random confidence score
        final float confidenceScore = new Random().nextFloat();

        // Make mock interpreter set confidence score for
        // "positive" label into pre-determined random value
        doAnswer(invocation -> {
            float[][] output = invocation.getArgument(1, float[][].class);
            output[0][labels.indexOf("positive")] = confidenceScore;
            return null;
        }).when(mockInterpreter).run(any(), any(float[][].class));

        // Make mock dictionary return any valid input format
        when(mockDictionary.tokenizeInputText(anyString()))
                .thenReturn(new float[0][]);

        // Make mock labels emulate an actual list of labels
        when(mockLabels.size())
                .thenReturn(labels.size());
        when(mockLabels.get(anyInt()))
                .thenAnswer(invocation -> labels.get(invocation.getArgument(0)));

        // Assert that detected sentiment is positive
        Sentiment sentiment = service.fetchSentiment(RandomString.string());
        assertEquals(SentimentType.POSITIVE, sentiment.getValue());
        assertEquals(confidenceScore * 100, sentiment.getConfidence(), 1E-5);
    }
}
