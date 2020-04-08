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
import com.amplifyframework.predictions.models.EntityType;
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

import java.util.Arrays;
import java.util.List;

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
        predictions = SynchronousPredictions.delegatingToAmplify();
    }

    /**
     * Assert that interpret fails for blank input.
     * AWS Comprehension will not accept a blank input, so
     * an exception is thrown.
     * @throws Exception if prediction fails
     */
    @Test(expected = PredictionsException.class)
    public void testInterpretFailsForNullInput() throws Exception {
        predictions.interpret("", InterpretOptions.defaults());
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
                InterpretOptions.defaults()
        );
        assertNotNull(result);

        // Assert detected language is English
        Language language = result.getLanguage();
        FeatureAssert.assertMatches(LanguageType.ENGLISH, language);
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
                InterpretOptions.defaults()
        );
        assertNotNull(result);

        // Assert detected language is French
        Language language = result.getLanguage();
        FeatureAssert.assertMatches(LanguageType.FRENCH, language);
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
                InterpretOptions.defaults()
        );
        assertNotNull(result);

        // Assert detected sentiment is positive
        Sentiment sentiment = result.getSentiment();
        FeatureAssert.assertMatches(SentimentType.POSITIVE, sentiment);
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
                InterpretOptions.defaults()
        );
        assertNotNull(result);

        // Assert detected sentiment is negative
        Sentiment sentiment = result.getSentiment();
        FeatureAssert.assertMatches(SentimentType.NEGATIVE, sentiment);
    }

    /**
     * Assert that key phrases are detected.
     * @throws Exception if prediction fails
     */
    @Test
    public void testKeyPhraseDetection() throws Exception {
        final String sampleText = "My mama always said life was like a box of chocolates.";

        // Interpret sample text and assert non-null result
        InterpretResult result = predictions.interpret(
                sampleText,
                InterpretOptions.defaults()
        );
        assertNotNull(result);

        // Assert key phrase detection
        List<KeyPhrase> actual = result.getKeyPhrases();
        List<String> expected = Arrays.asList(
                "My mama",
                "life",
                "a box",
                "chocolates"
        );
        FeatureAssert.assertMatches(expected, actual);
    }

    /**
     * Assert that entities are detected.
     * @throws Exception if prediction fails
     */
    @Test
    public void testEntityDetection() throws Exception {
        final String sampleText = "Toto, I've a feeling we're not in Kansas anymore.";

        // Interpret sample text and assert non-null result
        InterpretResult result = predictions.interpret(
                sampleText,
                InterpretOptions.defaults()
        );
        assertNotNull(result);

        // Assert entities detection
        List<Entity> actual = result.getEntities();
        List<EntityType> expected = Arrays.asList(
                EntityType.PERSON,  // Toto (it's a dog, but close enough)
                EntityType.LOCATION // Kansas
        );
        FeatureAssert.assertMatches(expected, actual);
    }

    /**
     * Assert that interpret correctly labels syntax.
     * @throws Exception if prediction fails
     */
    @Test
    public void testSyntaxDetection() throws Exception {
        final String sampleText = "I am inevitable.";

        // Interpret sample text and assert non-null result
        InterpretResult result = predictions.interpret(
                sampleText,
                InterpretOptions.defaults()
        );
        assertNotNull(result);

        // Assert syntax detection
        List<Syntax> actual = result.getSyntax();
        List<SpeechType> expected = Arrays.asList(
                SpeechType.PRONOUN,     // I
                SpeechType.VERB,        // am
                SpeechType.ADJECTIVE,   // inevitable
                SpeechType.PUNCTUATION  // .
        );
        FeatureAssert.assertMatches(expected, actual);
    }
}
