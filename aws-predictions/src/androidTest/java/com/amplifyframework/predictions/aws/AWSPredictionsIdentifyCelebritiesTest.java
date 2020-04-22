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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.amplifyframework.predictions.PredictionsCategory;
import com.amplifyframework.predictions.PredictionsException;
import com.amplifyframework.predictions.aws.test.R;
import com.amplifyframework.predictions.models.CelebrityDetails;
import com.amplifyframework.predictions.options.IdentifyOptions;
import com.amplifyframework.predictions.result.IdentifyCelebritiesResult;
import com.amplifyframework.testutils.sync.SynchronousMobileClient;
import com.amplifyframework.testutils.sync.SynchronousPredictions;

import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests that Predictions identify delivers a non-null
 * celebrity detection result for valid input.
 */
public final class AWSPredictionsIdentifyCelebritiesTest {

    private static final int MINIMUM_IMAGE_SIZE = 80;

    private SynchronousPredictions predictions;

    /**
     * Configure Predictions category before each test.
     * @throws Exception if mobile client initialization fails
     */
    @Before
    public void setUp() throws Exception {
        Context context = getApplicationContext();

        // Set up Auth
        SynchronousMobileClient.instance().initialize();

        // Delegate to Predictions category
        PredictionsCategory asyncDelegate =
                TestPredictionsCategory.create(context, R.raw.amplifyconfiguration);
        predictions = SynchronousPredictions.delegatingTo(asyncDelegate);
    }

    /**
     * Assert that identify fails for invalid image.
     * An image with less than 80x80 size limit will throw
     * validation error from the service.
     * @throws Exception if prediction fails
     */
    @Test(expected = PredictionsException.class)
    public void testIdentifyFailsForSmallImage() throws Exception {
        Bitmap image = Bitmap.createBitmap(MINIMUM_IMAGE_SIZE - 1,
                MINIMUM_IMAGE_SIZE - 1, Bitmap.Config.ARGB_8888);
        predictions.identifyCelebrities(image, IdentifyOptions.defaults());
    }

    /**
     * Assert that identify "passes" for blank image.
     * @throws Exception if prediction fails
     */
    @Test
    public void testIdentifyPassesForBlankImage() throws Exception {
        Bitmap image = Bitmap.createBitmap(MINIMUM_IMAGE_SIZE,
                MINIMUM_IMAGE_SIZE, Bitmap.Config.ARGB_8888);

        // Identify the celebrity inside given image and assert non-null result.
        IdentifyCelebritiesResult result = predictions.identifyCelebrities(image, IdentifyOptions.defaults());
        assertNotNull(result);

        // Assert nobody is detected
        List<CelebrityDetails> celebs = result.getCelebrities();
        assertTrue(celebs.isEmpty());
    }

    /**
     * Assert celebrity detection works.
     * @throws Exception if prediction fails
     */
    @Test
    public void testIdentifyCelebrities() throws Exception {
        InputStream bezosJpeg = getApplicationContext()
                .getAssets().open("jeff_bezos.jpg");
        final Bitmap image = BitmapFactory.decodeStream(bezosJpeg);

        // Identify the celebrity inside given image and assert non-null result.
        IdentifyCelebritiesResult result = predictions.identifyCelebrities(image, IdentifyOptions.defaults());
        assertNotNull(result);

        // Assert that Jeff Bezos is detected
        List<CelebrityDetails> celebs = result.getCelebrities();
        assertNotNull(celebs);
        assertFalse(celebs.isEmpty());
        assertTrue(celebs.stream().anyMatch(celeb -> "Jeff Bezos".equals(celeb.getCelebrity().getName())));
    }
}
