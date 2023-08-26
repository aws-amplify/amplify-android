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
package com.amplifyframework

import android.net.Uri

data class TOTPSetupDetails(
    val sharedSecret: String,
    val username: String
) {
    fun getSetupURI(
        issuer: String,
        accountName: String = username
    ): Uri {
        return Uri.parse("otpauth://totp/$issuer:$accountName?secret=$sharedSecret&issuer=$issuer")
    }
}
