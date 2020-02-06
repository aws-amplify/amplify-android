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

package com.amplifyframework.storage.s3;

import android.content.Context;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.storage.StorageAccessLevel;
import com.amplifyframework.storage.StorageException;
import com.amplifyframework.storage.options.StorageUploadFileOptions;
import com.amplifyframework.storage.result.StorageUploadFileResult;
import com.amplifyframework.testutils.Await;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.Date;

import static org.junit.Assert.assertEquals;

public final class AWSS3StoragePluginInstrumentationTest {
    private static final String TAG = AWSS3StoragePluginInstrumentationTest.class.getSimpleName();

    private final String filename = "test-" + new Date().getTime();

    /**
     * Setup the Android application context.
     * @throws AmplifyException from Amplify configuration
     */
    @BeforeClass
    public static void configureAmplify() throws AmplifyException {
        TestConfiguration.configureIfNotConfigured();
    }

    @Test
    public void testUploadPublicAccess() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();

        // Write a random file to cache directory
        File sampleFile = new File(context.getCacheDir(), filename);
        sampleFile.createNewFile();
        if (sampleFile.exists() && sampleFile.isFile()) {
            Log.i(TAG, "filename: " + sampleFile.getName() + " exists!");
        }

        StorageUploadFileResult result = Await.<StorageUploadFileResult, StorageException>result(
                (onResult, onError) ->
                Amplify.Storage.uploadFile(
                        sampleFile.getName(),
                        sampleFile.getAbsolutePath(),
                        onResult,
                        onError
                )
        );

        assertEquals(filename, result.getKey());
    }

    @Test
    public void testUploadPrivateAccess() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();

        // Write a random file to cache directory
        File sampleFile = new File(context.getCacheDir(), filename);
        sampleFile.createNewFile();
        if (sampleFile.exists() && sampleFile.isFile()) {
            Log.i(TAG, "filename: " + sampleFile.getName() + " exists!");
        }

        StorageUploadFileOptions options = StorageUploadFileOptions.builder()
                .accessLevel(StorageAccessLevel.PRIVATE)
                .build();

        Amplify.Storage.uploadFile(
                sampleFile.getName(),
                sampleFile.getAbsolutePath(),
                options,
                result -> Log.i(TAG, "Successfully uploaded: " + result.getKey()),
                error -> Log.e(TAG, "Error: " + error.getMessage())
        );
    }
}

