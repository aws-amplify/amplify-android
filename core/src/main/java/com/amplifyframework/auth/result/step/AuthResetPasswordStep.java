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

package com.amplifyframework.auth.result.step;

import com.amplifyframework.core.Action;
import com.amplifyframework.core.Consumer;

public enum AuthResetPasswordStep {
    /**
     * A code was sent to enable the user to change their password. Submit this code using
     * {@link com.amplifyframework.auth.AuthCategoryBehavior#confirmResetPassword(String, String, Action, Consumer)}
     * with the user's chosen new password.
     */
    CONFIRM_RESET_PASSWORD_WITH_CODE,

    /**
     * The flow is completed and no further steps are needed.
     */
    DONE;
}
