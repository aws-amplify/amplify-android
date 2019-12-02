/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.api.aws;

import androidx.annotation.NonNull;

import com.amplifyframework.api.ApiCategoryBehavior;
import com.amplifyframework.api.rest.RestResponse;
import com.amplifyframework.core.ResultListener;
import com.amplifyframework.testutils.LatchedResultListener;

import org.junit.Assert;

/**
 * An {@link ResultListener} that can await results.
 * This implementation expected the result type to be the {@link RestResponse} as
 * found in the template argument in several callbacks of the {@link ApiCategoryBehavior}.
 */
public final class LatchedRestResponseListener implements ResultListener<RestResponse> {

    private static final long DEFAULT_TIMEOUT_MS = 5_000 /* ms */;

    private final LatchedResultListener<RestResponse> latchedResultListener;

    /**
     * Constructs a new LatchedRestResponseListener with a provided latch timeout.
     * @param waitTimeMs Latch will timeout after this many milliseconds
     */
    public LatchedRestResponseListener(long waitTimeMs) {
        this.latchedResultListener = LatchedResultListener.waitFor(waitTimeMs);
    }

    /**
     * Constructs a new LatchedRestResponseListener using a default latch timeout of 5 seconds.
     */
    public LatchedRestResponseListener() {
        this(DEFAULT_TIMEOUT_MS);
    }

    @Override
    public void onResult(RestResponse result) {
        latchedResultListener.onResult(result);
    }

    @Override
    public void onError(Throwable error) {
        latchedResultListener.onError(error);
    }

    /**
     * Await a terminal event, either {@link #onResult(RestResponse)} is called,
     * or {@link #onError(Throwable)} is called.
     * @return Current {@link LatchedRestResponseListener} instance for fluent call chaining
     */
    @NonNull
    public LatchedRestResponseListener awaitTerminalEvent() {
        latchedResultListener.awaitTerminalEvent();
        return this;
    }

    /**
     * Awaits a RestResponse, and then validates that that response
     * had no RestResponse.Code, and did contain non-null data.
     * @return The non-null data in a successful GraphQLResponse
     * @throws AssertionError In all other circumstances
     */
    @NonNull
    public RestResponse awaitSuccessResponse() {
        final RestResponse response = latchedResultListener.awaitResult();
        Assert.assertTrue("Should return successful response", response.getCode().isSucessful());
        Assert.assertNotNull("No data in RestResponse", response.getData());
        return response;
    }

    /**
     * Await a RestResponse which contains (a) error(s).
     * @return The errors
     */
    @NonNull
    public RestResponse awaitErrors() {
        final RestResponse response = latchedResultListener.awaitResult();
        Assert.assertFalse("Should not return successful response", response.getCode().isSucessful());
        return response;
    }
}
