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

package com.amplifyframework.testutils;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.RawRes;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

/**
 * A little utility to load test content from src/test/resources.
 */
public final class Resources {
    private static final String RESOURCE_BASE = "src/test/resources";

    private Resources() {}

    /**
     * Gets a test resource as a string, given its path relative to the
     * resources directory. For example, the file at
     * ${PROJECT_ROOT}/src/test/resources/foo/bar.ext would have a
     * relative path value of "foo/bar.ext".
     * @param path Relative path of the test resource
     * @return Contents of the test resource as a string, if it is
     *         available
     * @throws RuntimeException If resource at path does not exist or cannot be read
     */
    public static String readAsString(final String path) throws RuntimeException {
        try {
            final String pathWithResourceBase = RESOURCE_BASE + "/" + path;
            final InputStream inputStream = new FileInputStream(pathWithResourceBase);
            return stringFromStream(inputStream);
        } catch (final IOException ioException) {
            throw new RuntimeException("Failed to load resource " + path, ioException);
        }
    }

    private static String stringFromStream(InputStream inputStream) {
        try {
            final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            final StringBuilder stringBuilder = new StringBuilder();

            String line = bufferedReader.readLine();
            while (line != null) {
                stringBuilder.append(line).append("\n");
                line = bufferedReader.readLine();
            }

            return stringBuilder.toString();
        } catch (IOException ioException) {
            throw new RuntimeException(ioException);
        }
    }

    /**
     * Obtains the raw resource ID for given resource identifier.
     * @param context An Android Context
     * @param identifier Name of a raw resource
     * @return ID of the raw resource
     * @throws RuntimeException if the specified raw resource does not exist
     */
    @RawRes
    public static int getRawResourceId(Context context, String identifier) {
        try {
            return context.getResources().getIdentifier(identifier, "raw", context.getPackageName());
        } catch (Exception exception) {
            throw new RuntimeException("Failed to locate " + identifier, exception);
        }
    }

    /**
     * Reads the content of a raw resource, as a string.
     * @param context An Android Context
     * @param rawResourceId ID of a raw resource
     * @return String content of the raw resource
     */
    public static String readAsString(Context context, @RawRes int rawResourceId) {
        return stringFromStream(context.getResources().openRawResource(rawResourceId));
    }

    /**
     * Reads the content of a raw resource, as a {@link JSONObject}.
     * @param context An Android Context
     * @param rawResourceId ID of a raw resource
     * @return JSON Object equivalent of the raw resource
     * @throws RuntimeException If resource with given ID does not exist or cannot be read
     */
    public static JSONObject readAsJson(Context context, @RawRes int rawResourceId) {
        try {
            return new JSONObject(readAsString(context, rawResourceId));
        } catch (JSONException jsonException) {
            throw new RuntimeException(jsonException);
        }
    }

    /**
     * Gets a test resource as a JSONObject, given its path relative to the
     * resources directory. For example, the file at
     * ${PROJECT_ROOT}/src/test/resources/foo/bar.json would have a
     * relative path value of "foo/bar.json".
     * @param path Relative path of the test resource
     * @return Contents of the test resource as a JSONObject, if it is
     *         available
     * @throws RuntimeException If resource at path does not exist or cannot be read
     */
    @NonNull
    public static JSONObject readAsJson(String path) {
        try {
            return new JSONObject(readAsString(path));
        } catch (JSONException jsonException) {
            throw new RuntimeException(jsonException);
        }
    }

    /**
     * Read lines of test from a resource. Each sequential line is a new item in the list.
     * @param path A path relative to the resources directory. For example, the file at
     *             ${PROJECT_ROOT}/src/test/resources/foo/bar.lines would have a relative
     *             path value of "foo/bar.lines".
     * @return A list of strings, one for each line of text found in the resource file
     *         that exists at the the provided path.
     */
    public static List<String> readLines(String path) {
        return Arrays.asList(readAsString(path).split("\\r?\\n"));
    }
}
