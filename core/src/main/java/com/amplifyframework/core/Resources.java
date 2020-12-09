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

package com.amplifyframework.core;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import androidx.annotation.RawRes;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.Scanner;

/**
 * A utility to read resource files.
 */
public final class Resources {
    private Resources() {}

    /**
     * Obtains the raw resource ID for the given resource identifier.
     * @param context An Android Context
     * @param identifier Name of a raw resource
     * @return ID of the raw resource
     * @throws ResourceLoadingException if the specified raw resource does not exist
     */
    @RawRes
    public static int getRawResourceId(Context context, String identifier) throws ResourceLoadingException {
        try {
            return context.getResources().getIdentifier(identifier, "raw", context.getPackageName());
        } catch (Exception lookupError) {
            throw new ResourceLoadingException("No such resource with identifier " + identifier, lookupError);
        }
    }

    /**
     * Reads the content of the resource with the given identifier, as a {@link JSONObject}.
     * @param context An Android Context
     * @param identifier Name of a raw resource
     * @return JSON Object equivalent of the raw resource
     * @throws ResourceLoadingException If resource with given ID does not exist or cannot be read
     */
    public static JSONObject readJsonResource(Context context, String identifier)
            throws ResourceLoadingException {
        return readJsonResourceFromId(context, getRawResourceId(context, identifier));
    }

    /**
     * Reads the content of the resource with the given ID, as a {@link JSONObject}.
     * @param context An Android Context
     * @param resourceId ID of a raw resource
     * @return JSON Object equivalent of the raw resource
     * @throws ResourceLoadingException If resource with given ID does not exist or cannot be read
     */
    public static JSONObject readJsonResourceFromId(Context context, @RawRes int resourceId)
            throws ResourceLoadingException {
        InputStream inputStream;

        try {
            inputStream = context.getResources().openRawResource(resourceId);
        } catch (NotFoundException notFoundError) {
            throw new ResourceLoadingException("No such resource with ID = " + resourceId, notFoundError);
        }

        final Scanner in = new Scanner(inputStream);
        final StringBuilder sb = new StringBuilder();
        while (in.hasNextLine()) {
            sb.append(in.nextLine());
        }
        in.close();

        try {
            return new JSONObject(sb.toString());
        } catch (JSONException badJsonError) {
            throw new ResourceLoadingException(
                "Failed to read the resource with ID " + resourceId + ".", badJsonError
            );
        }
    }

    /**
     * Indicates that a requested resource cannot be loaded.
     */
    public static final class ResourceLoadingException extends Exception {
        private static final long serialVersionUID = 1;

        /**
         * Constructs a new resource loading exception.
         * @param failureRationale A rationale for the failure
         * @param rootCause A root cause for the failure
         */
        public ResourceLoadingException(String failureRationale, Throwable rootCause) {
            super(failureRationale, rootCause);
        }
    }
}
