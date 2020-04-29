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
import androidx.core.util.Pair;

import com.amplifyframework.predictions.PredictionsCategory;
import com.amplifyframework.predictions.PredictionsException;
import com.amplifyframework.predictions.aws.test.R;
import com.amplifyframework.predictions.models.BoundedKeyValue;
import com.amplifyframework.predictions.models.Cell;
import com.amplifyframework.predictions.models.IdentifyActionType;
import com.amplifyframework.predictions.models.Table;
import com.amplifyframework.predictions.models.TextFormatType;
import com.amplifyframework.predictions.result.IdentifyDocumentTextResult;
import com.amplifyframework.predictions.result.IdentifyTextResult;
import com.amplifyframework.testutils.Assets;
import com.amplifyframework.testutils.FeatureAssert;
import com.amplifyframework.testutils.sync.SynchronousMobileClient;
import com.amplifyframework.testutils.sync.SynchronousPredictions;
import com.amplifyframework.util.CollectionUtils;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * Tests that Predictions identify delivers a non-null
 * text detection result for valid input.
 */
public final class AWSPredictionsIdentifyTextTest {

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
     * If {@link IdentifyActionType#DETECT_TEXT} is supplied instead of
     * {@link TextFormatType} as the action type for identify, then the operation
     * triggers error callback.
     * @throws Exception if prediction fails
     */
    @Test(expected = PredictionsException.class)
    public void testUnspecificTextFormatDetectionTypeFails() throws Exception {
        IdentifyActionType type = IdentifyActionType.DETECT_TEXT;
        final Bitmap image = Assets.readAsBitmap("sample-table.png");
        predictions.identify(type, image);
    }

    /**
     * Assert plain text detection works.
     * @throws Exception if prediction fails
     */
    @Test
    public void testIdentifyPlainText() throws Exception {
        final Bitmap image = Assets.readAsBitmap("sample-table.png");

        // Identify the text inside given image and assert non-null result.
        IdentifyTextResult result =
                (IdentifyTextResult) predictions.identify(TextFormatType.PLAIN, image);
        assertNotNull(result);
        assertNotNull(result.getFullText());
    }

    /**
     * Assert table detection works.
     * @throws Exception if prediction fails
     */
    @Test
    public void testIdentifyTables() throws Exception {
        final Bitmap image = Assets.readAsBitmap("sample-table.png");

        // Identify the text inside given image and assert non-null result.
        IdentifyDocumentTextResult result =
                (IdentifyDocumentTextResult) predictions.identify(TextFormatType.TABLE, image);
        assertNotNull(result);

        // Assert that one table is detected.
        List<Table> tables = result.getTables();
        assertFalse(CollectionUtils.isNullOrEmpty(tables));
        assertEquals(1, tables.size());

        // Assert that table has correct dimensions.
        Table table = tables.get(0);
        assertEquals(2, table.getRowSize());
        assertEquals(2, table.getColumnSize());

        // Assert that table has correct cells.
        List<Cell> cells = table.getCells();
        assertEquals(4, cells.size());
        FeatureAssert.assertContains("Name", cells);
        FeatureAssert.assertContains("Address", cells);
        FeatureAssert.assertContains("Ana Carolina", cells);
        FeatureAssert.assertContains("123 Any Town", cells);
    }

    /**
     * Assert form detection works.
     * @throws Exception if prediction fails
     */
    @Test
    public void testIdentifyForms() throws Exception {
        final Bitmap image = Assets.readAsBitmap("sample-form.png");

        // Identify the text inside given image and assert non-null result.
        IdentifyDocumentTextResult result =
                (IdentifyDocumentTextResult) predictions.identify(TextFormatType.FORM, image);

        // Assert that four key-values are detected.
        List<BoundedKeyValue> keyValues = result.getKeyValues();
        assertFalse(CollectionUtils.isNullOrEmpty(keyValues));
        assertEquals(4, keyValues.size());

        // Assert that key-value pairs have correct values.
        FeatureAssert.assertContains(new Pair<>("Name:", "Jane Doe"), keyValues);
        FeatureAssert.assertContains(new Pair<>("Address:", "123 Any Street, Anytown, USA"), keyValues);
        FeatureAssert.assertContains(new Pair<>("Birth date:", "12-26-1980"), keyValues);
        // FeatureAssert.assertContains(new Pair<>("Male:", "true"), keyValues); // Selection not supported by key-value
    }
}
