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

import com.amplifyframework.predictions.PredictionsCategory;
import com.amplifyframework.predictions.PredictionsException;
import com.amplifyframework.predictions.aws.test.R;
import com.amplifyframework.predictions.models.IdentifyActionType;
import com.amplifyframework.predictions.models.Label;
import com.amplifyframework.predictions.models.LabelType;
import com.amplifyframework.predictions.result.IdentifyLabelsResult;
import com.amplifyframework.testutils.Assets;
import com.amplifyframework.testutils.sync.SynchronousMobileClient;
import com.amplifyframework.testutils.sync.SynchronousPredictions;
import com.amplifyframework.util.CollectionUtils;

import org.junit.Before;
import org.junit.Test;

import io.reactivex.Observable;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests that Predictions identify delivers a non-null
 * label detection result for valid input.
 */
public final class AWSPredictionsIdentifyLabelsTest {

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
     * If {@link IdentifyActionType#DETECT_LABELS} is supplied instead of
     * {@link LabelType} as the action type for identify, then the operation
     * triggers error callback.
     * @throws Exception if prediction fails
     */
    @Test(expected = PredictionsException.class)
    public void testUnspecificLabelDetectionTypeFails() throws Exception {
        IdentifyActionType type = IdentifyActionType.DETECT_LABELS;
        final Bitmap image = Assets.readAsBitmap("jeff_bezos.jpg");
        predictions.identify(type, image);
    }

    /**
     * Assert label detection works.
     * @throws Exception if prediction fails
     */
    @Test
    public void testIdentifyLabels() throws Exception {
        final Bitmap image = Assets.readAsBitmap("jeff_bezos.jpg");

        // Identify the labels inside given image and assert non-null result.
        IdentifyLabelsResult result = (IdentifyLabelsResult) predictions.identify(LabelType.ALL, image);
        assertNotNull(result);

        // Assert that Jeff's portrait doesn't flag moderation :)
        assertFalse(result.isUnsafeContent());

        // Assert at least one label is detected as "Person"
        assertFalse(CollectionUtils.isNullOrEmpty(result.getLabels()));
        assertTrue(Observable.fromIterable(result.getLabels())
                .map(Label::getName)
                .toList()
                .blockingGet()
                .contains("Person"));
    }
}
