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

package com.amplifyframework.logging;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.plugin.Plugin;

/**
 * Abstract class that a plugin implementation of Logging category
 * would extend. This includes the client behavior dictated by
 * {@link LoggingCategoryBehavior} and {@link Plugin}.
 * @param <E> The class type of the escape hatch used to perform
 *            low-level, implementation-specific logging operations
 */
public abstract class LoggingPlugin<E> implements LoggingCategoryBehavior, Plugin<E> {
    @NonNull
    @Override
    public final CategoryType getCategoryType() {
        return CategoryType.LOGGING;
    }

    @WorkerThread
    @Override
    public void initialize(@NonNull Context context) throws AmplifyException {
    }

    @Override
    public final boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof LoggingPlugin)) {
            return false;
        }
        LoggingPlugin<?> otherPlugin = (LoggingPlugin<?>) object;
        return getPluginKey().equals(otherPlugin.getPluginKey());
    }

    @Override
    public final int hashCode() {
        return getPluginKey().hashCode();
    }
}
