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

import com.amplifyframework.auth.AuthUserAttributeKey;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Consumer;

/**
 * Represents the various common steps a user could be in for the user attribute update flow.
 */
public enum AuthUpdateAttributeStep {
    /**
     * A code was sent to enable the user to update their user attribute. Submit this code using
     * {@link com.amplifyframework.auth.AuthCategoryBehavior#confirmUserAttribute(AuthUserAttributeKey,
     * String, Action, Consumer)}
     * with the user's chosen new password.
     */
    CONFIRM_ATTRIBUTE_WITH_CODE,

    /**
     * The flow is completed and no further steps are needed.
     */
    DONE;
}
