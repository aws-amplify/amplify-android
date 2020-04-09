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

import android.content.Context;

import com.amplifyframework.predictions.PredictionsCategory;
import com.amplifyframework.predictions.PredictionsException;
import com.amplifyframework.predictions.aws.test.R;
import com.amplifyframework.predictions.models.Entity;
import com.amplifyframework.predictions.models.KeyPhrase;
import com.amplifyframework.predictions.models.Language;
import com.amplifyframework.predictions.models.LanguageType;
import com.amplifyframework.predictions.models.Sentiment;
import com.amplifyframework.predictions.models.SentimentType;
import com.amplifyframework.predictions.models.SpeechType;
import com.amplifyframework.predictions.models.Syntax;
import com.amplifyframework.predictions.options.InterpretOptions;
import com.amplifyframework.predictions.options.TranslateTextOptions;
import com.amplifyframework.predictions.result.InterpretResult;
import com.amplifyframework.predictions.result.TranslateTextResult;
import com.amplifyframework.testutils.Assets;
import com.amplifyframework.testutils.sync.SynchronousMobileClient;
import com.amplifyframework.testutils.sync.SynchronousPredictions;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * Tests that Predictions translate delivers a non-null result
 * for valid input.
 */
public final class AWSPredictionsTranslateTest {

    private SynchronousPredictions predictions;

    /**
     * Initialize mobile client singleton.
     * @throws Exception if mobile client initialization fails
     */
    @BeforeClass
    public static void setUpOnce() throws Exception {
        SynchronousMobileClient.instance().initialize();
    }

    /**
     * Configure Predictions category before each test.
     */
    @Before
    public void setUp() {
        Context context = getApplicationContext();

        // Delegate to Predictions category
        PredictionsCategory asyncDelegate =
                TestPredictionsCategory.create(context, R.raw.amplifyconfiguration);
        predictions = SynchronousPredictions.delegatingTo(asyncDelegate);
    }

    /**
     * Assert that translate fails for blank input.
     * AWS Translate will not accept a blank input, so
     * an exception is thrown.
     * @throws Exception if prediction fails
     */
    @Test(expected = PredictionsException.class)
    public void testTranslateFailsForNullInput() throws Exception {
        predictions.translateText("", TranslateTextOptions.defaults());
    }

    /**
     * Assert that category falls back to configured default language.
     * @throws Exception if prediction fails
     */
    @Test
    public void testTranslationWithDefaultLanguage() throws Exception {
        final String sampleText = "Hello world!";

        // Translate english text and assert non-null result.
        // Use configured default languages
        TranslateTextResult result = predictions.translateText(
                sampleText,
                // LanguageType.ENGLISH,
                // LanguageType.SPANISH,
                TranslateTextOptions.defaults()
        );
        assertNotNull(result);

        // Assert translated language is in Spanish
        LanguageType language = result.getTargetLanguage();
        assertEquals(LanguageType.SPANISH, language);

        // Assert translation
        String translation = result.getTranslatedText();
        assertEquals("¡Hola mundo!", translation);
    }

    /**
     * Assert that category falls back to configured default language.
     * @throws Exception if prediction fails
     */
    @Test
    public void testTranslationWithLanguageOverride() throws Exception {
        final String sampleText = "¡Hola mundo!";

        // Translate english text and assert non-null result.
        // Use configured default languages
        TranslateTextResult result = predictions.translateText(
                sampleText,
                 LanguageType.SPANISH,
                 LanguageType.ENGLISH,
                TranslateTextOptions.defaults()
        );
        assertNotNull(result);

        // Assert translated language is in English
        LanguageType language = result.getTargetLanguage();
        assertEquals(LanguageType.ENGLISH, language);

        // Assert translation
        String translation = result.getTranslatedText();
        assertEquals("Hello world!", translation);
    }
}
