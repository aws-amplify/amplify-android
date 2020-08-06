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

package com.amplifyframework;

/**
 * Enum to represent various platforms that use Amplify library for
 * tracking usage metrics.
 */
public enum Platform {
    /**
     * Represents the Android platform.
     */
    ANDROID("amplify-android"),

    /**
     * Represents the Flutter platform.
     */
    FLUTTER("amplify-flutter");

    private final String libraryName;

    Platform(String libraryName) {
        this.libraryName = libraryName;
    }

    /**
     * Gets the library name to be used by the user agent.
     * @return the library name for a given platform
     */
    public String getLibraryName() {
        return libraryName;
    }
}
