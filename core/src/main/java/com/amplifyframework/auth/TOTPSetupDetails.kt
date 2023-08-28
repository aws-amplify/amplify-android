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
package com.amplifyframework.auth

import android.net.Uri

/**
 * Details of TOTP Setup that help launch into TOTP manager.
 *
 * @param sharedSecret Secret code returned by the service to help setting up TOTP
 * @param username username that will be used to construct the URI
 */
data class TOTPSetupDetails(
    val sharedSecret: String,
    val username: String
) {

    /**
     * Returns a TOTP setup URI that can help avoid barcode scanning and use native password manager
     * to handle TOTP association.
     *
     * @param appName of TOTP manager
     * @param accountName for TOTP manager. Defaults to stored username value.
     */
    @JvmOverloads
    fun getSetupURI(
        appName: String,
        accountName: String = username
    ): Uri {
        return Uri.parse("otpauth://totp/$appName:$accountName?secret=$sharedSecret&issuer=$appName")
    }
}
