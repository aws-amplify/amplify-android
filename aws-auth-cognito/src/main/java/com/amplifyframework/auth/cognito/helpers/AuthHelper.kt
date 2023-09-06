/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.auth.cognito.helpers

import com.amplifyframework.auth.cognito.exceptions.service.InvalidParameterException
import com.amplifyframework.auth.exceptions.UnknownException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

internal open class AuthHelper {

    companion object {
        val HMAC_SHA_256 = "HmacSHA256"

        /**
         * Generates secret hash. Uses HMAC SHA256.
         *
         * @param userId User ID
         * @param clientId Client ID
         * @param clientSecret Client secret
         * @return secret hash as a `String`, `null ` if `clientSecret` is `null`
         */
        fun getSecretHash(userId: String?, clientId: String?, clientSecret: String?): String? {
            return when {
                userId == null -> throw InvalidParameterException(
                    cause = Exception("user ID cannot be null")
                )
                clientId == null -> throw InvalidParameterException(
                    cause = Exception("client ID cannot be null")
                )
                clientSecret.isNullOrEmpty() -> null
                else ->
                    try {
                        val mac = Mac.getInstance(HMAC_SHA_256)
                        val keySpec = SecretKeySpec(clientSecret.toByteArray(), HMAC_SHA_256)
                        mac.init(keySpec)
                        mac.update(userId.toByteArray())
                        val raw = mac.doFinal(clientId.toByteArray())
                        String(android.util.Base64.encode(raw, android.util.Base64.NO_WRAP))
                    } catch (e: Exception) {
                        throw UnknownException(cause = Exception("errors in HMAC calculation"))
                    }
            }
        }
    }
}
