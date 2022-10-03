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

package com.amplifyframework.auth.cognito.asf

import android.util.Base64
import android.util.Log
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Creates the signature for context data. It create HMAC_SHA256 for the
 * stringified JSON payload data and then encodes it in Base64. Payload contains
 * username, userPoolId and timestamp so these are already factored in the
 * generated signature.
 */
class SignatureGenerator {
    companion object {
        private val TAG = SignatureGenerator::class.java.simpleName
        private const val HMAC_SHA_256 = "HmacSHA256"

        /**
         * Generates the signature for the JSON data payload.
         * @param data JSON payload for contextData.
         * @param secret secret key used for generating the signature.
         * @param version version name of the data provider.
         * @return signature string for the payload.
         */
        @JvmStatic
        fun getSignature(data: String, secret: String, version: String) = try {
            val mac = Mac.getInstance(HMAC_SHA_256)
            val secretKey = SecretKeySpec(secret.toByteArray(), HMAC_SHA_256)
            mac.init(secretKey)
            mac.update(version.toByteArray())
            val signature = mac.doFinal(data.toByteArray())
            String(Base64.encode(signature, Base64.NO_WRAP))
        } catch (e: Exception) {
            Log.w(TAG, "Exception while completing context data signature", e)
            ""
        }
    }
}
