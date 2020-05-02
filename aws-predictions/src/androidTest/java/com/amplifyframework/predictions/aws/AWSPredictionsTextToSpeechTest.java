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
import android.media.MediaDataSource;
import android.media.MediaPlayer;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.amplifyframework.predictions.PredictionsCategory;
import com.amplifyframework.predictions.PredictionsException;
import com.amplifyframework.predictions.aws.test.R;
import com.amplifyframework.predictions.result.TextToSpeechResult;
import com.amplifyframework.testutils.Assets;
import com.amplifyframework.testutils.sync.SynchronousMobileClient;
import com.amplifyframework.testutils.sync.SynchronousPredictions;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertNotNull;

/**
 * Tests that Predictions text-to-speech conversion
 * delivers a non-null result for valid input.
 */
public final class AWSPredictionsTextToSpeechTest {

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

    @Test
    public void testTextToSpeechReturnsNonNullResult() throws PredictionsException {
        // Interpret english text and assert non-null result
        final String englishText = Assets.readAsString("sample-text-en.txt");
        TextToSpeechResult result = predictions.convertTextToSpeech(englishText);

        // Assert non-null audio data
        InputStream audioData = result.getAudioData();
        assertNotNull(audioData);

        // MediaPlayer mp = new MediaPlayer();
        // mp.setDataSource(new InputStreamMediaDataSource(audioData));
        // mp.start();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public static final class InputStreamMediaDataSource extends MediaDataSource {
        private InputStream stream;

        private InputStreamMediaDataSource(InputStream stream) {
            this.stream = stream;
        }

        @Override
        public synchronized void close() throws IOException {
            stream.close();
        }

        @Override
        public synchronized int readAt(long position, byte[] buffer, int offset, int size) throws IOException {
            return stream.read(buffer, offset, size);
        }

        @Override
        public synchronized long getSize() throws IOException {
            return stream.available();
        }
    }
}
