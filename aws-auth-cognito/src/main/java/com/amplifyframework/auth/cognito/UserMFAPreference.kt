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
package com.amplifyframework.auth.cognito

import com.amplifyframework.auth.MFAType

/**
 * Output for fetching MFA preference.
 *
 * @param enabled MFA types
 * @param preferred MFA type. null if not set
 */
data class UserMFAPreference(
    val enabled: Set<MFAType>?,
    val preferred: MFAType?
)

/**
 * Input for updating the MFA preference for a MFA Type
 */
enum class MFAPreference(
    internal val mfaEnabled: Boolean,
    internal val mfaPreferred: Boolean? = null
) {
    /**
     * MFA not enabled
     */
    DISABLED(false),

    /**
     * MFA enabled
     */
    ENABLED(true),

    /**
     * MFA enabled and preferred
     */
    PREFERRED(true, true),

    /**
     * MFA enabled and not preferred
     */
    NOT_PREFERRED(true, false)
}
