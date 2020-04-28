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

import com.amplifyframework.predictions.aws.adapter.BitmapAdapter;

import com.amazonaws.services.textract.model.Document;

import java.nio.ByteBuffer;

/**
 * Simple request instance for image identification operation.
 */
public final class AWSTextractRequest {

    private final Document document;

    private AWSTextractRequest(Document document) {
        this.document = document;
    }

    /**
     * Constructs an instance of {@link AWSTextractRequest}.
     * @param image the input image to extract text from
     * @return a request for Amazon Textract service
     */
    @NonNull
    public static AWSTextractRequest fromBitmap(@NonNull Bitmap image) {
        ByteBuffer buffer = BitmapAdapter.fromBitmap(image);
        Document document = new Document().withBytes(buffer);
        return new AWSTextractRequest(document);
    }

    /**
     * Gets the Textract input document.
     * @return the Textract document
     */
    @NonNull
    public Document getDocument() {
        return document;
    }
}
