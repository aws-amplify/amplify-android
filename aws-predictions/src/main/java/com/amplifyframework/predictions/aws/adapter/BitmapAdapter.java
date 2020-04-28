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

package com.amplifyframework.predictions.aws.adapter;

import android.graphics.Bitmap;
import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Utility class to extract Android's {@link Bitmap} data
 * to be sent to AWS services as input.
 */
public final class BitmapAdapter {

    private static final int COMPRESS_QUALITY_PERCENT = 100;

    private BitmapAdapter() {}

    /**
     * Compresses given image bitmap to JPEG format and then
     * writes the byte content to a {@link ByteBuffer}.
     * @param image the input image bitmap
     * @return the image byte content
     */
    public static ByteBuffer fromBitmap(@NonNull Bitmap image) {
        Objects.requireNonNull(image);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, COMPRESS_QUALITY_PERCENT, stream);
        return ByteBuffer.wrap(stream.toByteArray());
    }
}
