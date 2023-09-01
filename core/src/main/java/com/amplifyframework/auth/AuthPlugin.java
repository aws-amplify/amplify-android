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

package com.amplifyframework.auth;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.auth.options.AuthVerifyTOTPSetupOptions;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.plugin.Plugin;

/**
 * Abstract class that a plugin implementation of the Auth Category
 * would extend. This includes the client behavior dictated by
 * {@link AuthCategoryBehavior} and {@link Plugin}.
 * @param <E> The class type of the escape hatch provided by the plugin
 */
public abstract class AuthPlugin<E> implements AuthCategoryBehavior, Plugin<E> {
    @NonNull
    @Override
    public final CategoryType getCategoryType() {
        return CategoryType.AUTH;
    }

    @WorkerThread
    @Override
    public void initialize(@NonNull Context context) throws AmplifyException {}

    /**
     * Default implementation that throws UnsupportedOperationException.
     * @param onSuccess Success callback
     * @param onError Error callback
     */
    @Override
    public void setUpTOTP(@NonNull Consumer<TOTPSetupDetails> onSuccess, @NonNull Consumer<AuthException> onError) {
        throw new UnsupportedOperationException("TOTP is not implemented in this plugin");
    }

    /**
     * Default implementation that throws UnsupportedOperationException.
     * @param code TOTP code to verify TOTP setup
     * @param onSuccess Success callback
     * @param onError Error callback
     */
    @Override
    public void verifyTOTPSetup(
        @NonNull String code,
        @NonNull Action onSuccess,
        @NonNull Consumer<AuthException> onError
    ) {
        throw new UnsupportedOperationException("TOTP is not implemented in this plugin");
    }

    /**
     * Default implementation that throws UnsupportedOperationException.
     * @param code TOTP code to verify TOTP setup
     * @param options additional options to verify totp setup
     * @param onSuccess Success callback
     * @param onError Error callback
     */
    @Override
    public void verifyTOTPSetup(
        @NonNull String code,
        @NonNull AuthVerifyTOTPSetupOptions options,
        @NonNull Action onSuccess,
        @NonNull Consumer<AuthException> onError
    ) {
        throw new UnsupportedOperationException("TOTP is not implemented in this plugin");
    }
}
