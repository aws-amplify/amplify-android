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

public data class UserMFAPreference(
    val enabled: Set<MFAType>?,
    val preferred: MFAType?
)

public sealed class MFAPreference {
    abstract val mfaEnabled: Boolean
    abstract val mfaPreferred: Boolean

    object Disabled : MFAPreference() {
        override val mfaEnabled: Boolean
            get() = false
        override val mfaPreferred: Boolean
            get() = false
    }

    object Enabled : MFAPreference() {
        override val mfaEnabled: Boolean
            get() = true
        override val mfaPreferred: Boolean
            get() = false
    }

    object Preferred : MFAPreference() {
        override val mfaEnabled: Boolean
            get() = true
        override val mfaPreferred: Boolean
            get() = true
    }

    object NotPreferred : MFAPreference() {
        override val mfaEnabled: Boolean
            get() = true
        override val mfaPreferred: Boolean
            get() = false
    }
}
