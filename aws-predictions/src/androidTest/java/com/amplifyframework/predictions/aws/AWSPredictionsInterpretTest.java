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

import com.amplifyframework.predictions.PredictionsException;
import com.amplifyframework.predictions.models.Entity;
import com.amplifyframework.predictions.models.KeyPhrase;
import com.amplifyframework.predictions.models.Language;
import com.amplifyframework.predictions.models.LanguageType;
import com.amplifyframework.predictions.models.Sentiment;
import com.amplifyframework.predictions.models.SentimentType;
import com.amplifyframework.predictions.models.SpeechType;
import com.amplifyframework.predictions.models.Syntax;
import com.amplifyframework.predictions.options.InterpretOptions;
import com.amplifyframework.predictions.result.InterpretResult;
import com.amplifyframework.testutils.Assets;
import com.amplifyframework.testutils.sync.SynchronousPredictions;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * Tests that Predictions interpret delivers a non-null result
 * for valid input.
 */
public final class AWSPredictionsInterpretTest {

    private static SynchronousPredictions predictions;

    /**
     * Configure Amplify and Predictions plugin before the tests.
     * @throws Exception if configuration fails
     */
    @BeforeClass
    public static void setUpOnce() throws Exception {
        TestConfiguration.configureIfNotConfigured();
        predictions = SynchronousPredictions.singleton();
    }

    /**
     * Assert that interpret fails for blank input.
     * AWS Comprehension will not accept a blank input, so
     * an exception is thrown.
     * @throws Exception if prediction fails
     */
    @Test(expected = PredictionsException.class)
    public void testInterpretFailsForNullInput() throws Exception {
        predictions.interpret("", InterpretOptions.defaultInstance());
    }

    /**
     * Assert that english text is detected as English.
     * @throws Exception if prediction fails
     */
    @Test
    public void testEnglishLanguageDetection() throws Exception {
        // Interpret english text and assert non-null result
        InterpretResult result = predictions.interpret(
                Assets.readAsString("sample-text-en.txt"),
                InterpretOptions.defaultInstance()
        );
        assertNotNull(result);

        // Assert detected language is English
        Language language = result.getLanguage();
        assertNotNull(language);
        assertEquals(LanguageType.ENGLISH, language.getValue());
    }

    /**
     * Assert that english text is detected as French.
     * @throws Exception if prediction fails
     */
    @Test
    public void testFrenchLanguageDetection() throws Exception {
        // Interpret french text and assert non-null result
        InterpretResult result = predictions.interpret(
                Assets.readAsString("sample-text-fr.txt"),
                InterpretOptions.defaultInstance()
        );
        assertNotNull(result);

        // Assert detected language is French
        Language language = result.getLanguage();
        assertNotNull(language);
        assertEquals(LanguageType.FRENCH, language.getValue());
    }

    /**
     * Assert that happy review is detected as positive.
     * @throws Exception if prediction fails
     */
    @Test
    public void testPositiveSentimentDetection() throws Exception {
        // Interpret positive text and assert non-null result
        InterpretResult result = predictions.interpret(
                Assets.readAsString("positive-review.txt"),
                InterpretOptions.defaultInstance()
        );
        assertNotNull(result);

        // Assert detected sentiment is positive
        Sentiment sentiment = result.getSentiment();
        assertNotNull(sentiment);
        assertEquals(SentimentType.POSITIVE, sentiment.getValue());
    }

    /**
     * Assert that unhappy review is detected as negative.
     * @throws Exception if prediction fails
     */
    @Test
    public void testNegativeSentimentDetection() throws Exception {
        // Interpret negative text and assert non-null result
        InterpretResult result = predictions.interpret(
                Assets.readAsString("negative-review.txt"),
                InterpretOptions.defaultInstance()
        );
        assertNotNull(result);

        // Assert detected sentiment is negative
        Sentiment sentiment = result.getSentiment();
        assertNotNull(sentiment);
        assertEquals(SentimentType.NEGATIVE, sentiment.getValue());
    }

    /**
     * Assert that key phrases are detected.
     * @throws Exception if prediction fails
     */
    @Test
    public void testKeyPhraseDetection() throws Exception {
        // Interpret sample text and assert non-null result
        InterpretResult result = predictions.interpret(
                Assets.readAsString("sample-text-en.txt"),
                InterpretOptions.defaultInstance()
        );
        assertNotNull(result);

        // Assert detected key phrases are not empty
        List<KeyPhrase> keyPhrases = result.getKeyPhrases();
        assertNotNull(keyPhrases);
        assertFalse(keyPhrases.isEmpty());
    }

    /**
     * Assert that entities are detected.
     * @throws Exception if prediction fails
     */
    @Test
    public void testEntityDetection() throws Exception {
        // Interpret sample text and assert non-null result
        InterpretResult result = predictions.interpret(
                Assets.readAsString("sample-text-en.txt"),
                InterpretOptions.defaultInstance()
        );
        assertNotNull(result);

        // Assert detected entities are not empty
        List<Entity> entities = result.getEntities();
        assertNotNull(entities);
        assertFalse(entities.isEmpty());
    }

    /**
     * Assert that interpret correctly labels syntax.
     * @throws Exception if prediction fails
     */
    @Test
    @SuppressWarnings("MagicNumber") // TODO: What's a better way to compare results?
    public void testSyntaxDetection() throws Exception {
        final String sampleText = "I am inevitable.";

        // Interpret sample text and assert non-null result
        InterpretResult result = predictions.interpret(
                sampleText,
                InterpretOptions.defaultInstance()
        );
        assertNotNull(result);

        // Assert syntax detection
        List<Syntax> syntax = result.getSyntax();
        assertNotNull(syntax);
        assertEquals(4, syntax.size());

        // Assert that evaluation matches
        assertEquals(SpeechType.PRONOUN, syntax.get(0).getValue());     // I
        assertEquals(SpeechType.VERB, syntax.get(1).getValue());        // am
        assertEquals(SpeechType.ADJECTIVE, syntax.get(2).getValue());   // inevitable
        assertEquals(SpeechType.PUNCTUATION, syntax.get(3).getValue()); // .
    }
}
