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

package com.amplifyframework.api;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * A little utility to load test content from src/test/resources.
 */
public final class Resources {

    private static final String RESOURCE_BASE = "src/test/resources";

    /**
     * Dis-allows instantiation of this test utility.
     */
    private Resources() {
    }

    /**
     * Gets a test resource as a string, given its path relative to the
     * resources directory. For example, the file at
     * ${PROJECT_ROOT}/src/test/resources/foo/bar.ext would have a
     * relative path value of "foo/bar.ext".
     * @param path Relative path of the test resource
     * @return Contents of the test resource as a string, if it is
     *         available
     */
    public static String readAsString(final String path) {
        try {
            final String pathWithResourceBase = RESOURCE_BASE + "/" + path;
            final InputStream inputStream = new FileInputStream(pathWithResourceBase);
            final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            final StringBuilder stringBuilder = new StringBuilder();

            String line = bufferedReader.readLine();
            while (line != null) {
                stringBuilder.append(line).append("\n");
                line = bufferedReader.readLine();
            }

            return stringBuilder.toString();

        } catch (final IOException ioException) {
            throw new RuntimeException("Failed to load resource " + path, ioException);
        }
    }
}

