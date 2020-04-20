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

package com.amplifyframework.api.graphql.error;

import java.util.Map;

public class AppSyncExtensions {
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

    /**
     * Returns the specific AppSync error type, often related to handling conflicts.
     * https://docs.aws.amazon.com/appsync/latest/devguide/conflict-detection-and-sync.html#errors
     *
     * @return errorType
     */
    public String getErrorType() {
        return errorType;
    }

    /**
     * Returns more info about the error.
     *
     * @return errorInfo
     */
    public String getErrorInfo() {
        return errorInfo;
    }

    /**
     * For conflict unhandled errors, this returns a map containing a version number, and the model
     * fields corresponding to the current server state.
     *
     * @return data
     */
    public Map<String, Object> getData() {
        return data;
    }
}
