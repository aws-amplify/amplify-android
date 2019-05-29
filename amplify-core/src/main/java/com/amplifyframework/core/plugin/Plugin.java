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

package com.amplifyframework.core.plugin;

import android.support.annotation.NonNull;

import org.json.JSONObject;

public interface Plugin {
    String getPluginKey();

    void configure(@NonNull JSONObject jsonObject);

    void configure(@NonNull JSONObject jsonObject, @NonNull String key);

    void reset();

    Plugin initWithConfiguration(@NonNull JSONObject jsonObject);

    Plugin initWithConfiguration(@NonNull JSONObject jsonObject, @NonNull String key);
}
