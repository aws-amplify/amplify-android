/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.auth.options;

/**
 * The shared options among all Auth plugins.
 * Note: This is currently empty but exists here to support common verify totp setup options.
 */
public class AuthVerifyTOTPSetupOptions {

    /**
     * The builder for this class.
     * @param <T> The type of builder - used to support plugin extensions of this.
     */
    public abstract static class Builder<T extends Builder<T>> {

        /**
         * Build an instance of AuthVerifyTOTPSetupOptions (or one of its subclasses).
         * @return an instance of AuthVerifyTOTPSetupOptions (or one of its subclasses)
         */
        public AuthVerifyTOTPSetupOptions build() {
            return new AuthVerifyTOTPSetupOptions();
        }

    }
}
