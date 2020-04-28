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

package com.amplifyframework.datastore.appsync;

import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.util.Immutable;

import java.util.Map;

/**
 * Model representing a AppSync specific extensions map.  This data may be included on a
 * GraphQLResponse.Error object, and is represented as a Map&lt;String, Object&gt;.  This class can be
 * used to create a strongly typed model by passing the Map&lt;String, Object&gt; to the constructor.
 */
public final class AppSyncExtensions {
    private static final String ERROR_TYPE_KEY = "errorType";
    private static final String ERROR_INFO_KEY = "errorInfo";
    private static final String DATA_KEY = "data";

    private final String errorType;
    private final String errorInfo;
    private final Map<String, Object> data;

    @SuppressWarnings("unchecked")
    public AppSyncExtensions(Map<String, Object> extensions) {
        this.errorType = (String) extensions.get(ERROR_TYPE_KEY);
        this.errorInfo = (String) extensions.get(ERROR_INFO_KEY);
        this.data = (Map<String, Object>) extensions.get(DATA_KEY);
    }

    public AppSyncExtensions(String errorType, String errorInfo, Map<String, Object> data) {
        this.errorType = errorType;
        this.errorInfo = errorInfo;
        this.data = data;
    }

    /**
     * Returns the specific AppSync error type, often related to handling conflicts.
     * https://docs.aws.amazon.com/appsync/latest/devguide/conflict-detection-and-sync.html#errors
     *
     * @return errorType
     */
    @Nullable
    public String getErrorType() {
        return errorType;
    }

    /**
     * Returns more info about the error.
     *
     * @return errorInfo
     */
    @Nullable
    public String getErrorInfo() {
        return errorInfo;
    }

    /**
     * For conflict unhandled errors, returns a map containing the same fields as the model type.
     *
     * @return data
     */
    @Nullable
    public Map<String, Object> getData() {
        return Immutable.of(data);
    }

    @Override
    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        AppSyncExtensions extensions = (AppSyncExtensions) thatObject;

        return ObjectsCompat.equals(errorType, extensions.errorType) &&
                ObjectsCompat.equals(errorInfo, extensions.errorInfo) &&
                ObjectsCompat.equals(data, extensions.data);
    }

    @Override
    public int hashCode() {
        int result = errorType.hashCode();
        result = 31 * result + (errorInfo != null ? errorInfo.hashCode() : 0);
        result = 31 * result + (data != null ? data.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "AppSyncExtensions{" +
                "errorType=\'" + errorType + "\'" +
                ", errorInfo=\'" + errorInfo + "\'" +
                ", data=\'" + data + "\'" +
                '}';
    }
}
