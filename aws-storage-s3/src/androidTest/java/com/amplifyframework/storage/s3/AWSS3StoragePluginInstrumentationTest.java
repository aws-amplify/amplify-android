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
import com.amplifyframework.storage.options.StorageUploadFileOptions;
import com.amplifyframework.testutils.RandomString;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public final class AWSS3StoragePluginInstrumentationTest {
    private static final String TAG = AWSS3StoragePluginInstrumentationTest.class.getSimpleName();

    /**
     * Setup the Android application context.
     * @throws AmplifyException from Amplify configuration
     */
    @BeforeClass
    public static void configureAmplify() throws AmplifyException {
        TestConfiguration.configureIfNotConfigured();
    }

    @Test
    public void testUploadPublicAccess() {
        Context context = ApplicationProvider.getApplicationContext();

        // Write a random file to cache directory
        final String message = RandomString.string();
        File sampleFile = new File(context.getCacheDir(), "sample.txt");
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(sampleFile));
            writer.append(message);
            writer.close();
        } catch(IOException error) {
            Log.e(TAG, error.getMessage());
        }

        Amplify.Storage.uploadFile(
                "test-key",
                sampleFile.getAbsolutePath(),
                onSuccess -> Log.i(TAG, "Successfully uploaded: " + onSuccess.getKey()),
                onError -> {
                    onError.printStackTrace();
                    throw new RuntimeException();
                }
        );
    }

    @Test
    public void testUploadPrivateAccess() {
        Context context = ApplicationProvider.getApplicationContext();

        // Write a random file to cache directory
        final String message = RandomString.string();
        File sampleFile = new File(context.getCacheDir(), "sample.txt");
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(sampleFile));
            writer.append(message);
            writer.close();
        } catch(IOException error) {
            Log.e(TAG, error.getMessage());
        }

        StorageUploadFileOptions options = StorageUploadFileOptions.builder()
                .accessLevel(StorageAccessLevel.PRIVATE)
                .build();

        Amplify.Storage.uploadFile(
                "test-key",
                sampleFile.getAbsolutePath(),
                options,
                onSuccess -> Log.i(TAG, "Successfully uploaded: " + onSuccess.getKey()),
                onError -> {
                    onError.printStackTrace();
                    throw new RuntimeException();
                }
        );
    }
}

