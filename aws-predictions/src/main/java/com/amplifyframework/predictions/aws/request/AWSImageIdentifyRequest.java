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

package com.amplifyframework.predictions.aws.request;

import android.graphics.Bitmap;
import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Simple request instance for image identification operation.
 */
public final class AWSImageIdentifyRequest {

    private static final int COMPRESS_QUALITY_PERCENT = 100;

    private final ByteBuffer buffer;

    private AWSImageIdentifyRequest(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    /**
     * Constructs an instance of {@link AWSImageIdentifyRequest}.
     * @param image the input image to analyze
     * @return a request for Amazon Rekognition and Amazon Textract services
     */
    @NonNull
    public static AWSImageIdentifyRequest fromBitmap(@NonNull Bitmap image) {
        Objects.requireNonNull(image);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, COMPRESS_QUALITY_PERCENT, stream);
        ByteBuffer buffer = ByteBuffer.wrap(stream.toByteArray());
        return new AWSImageIdentifyRequest(buffer);
    }

    /**
     * Gets the byte data of input image.
     * @return the byte buffer of image
     */
    @NonNull
    public ByteBuffer getBuffer() {
        return buffer;
    }
}
