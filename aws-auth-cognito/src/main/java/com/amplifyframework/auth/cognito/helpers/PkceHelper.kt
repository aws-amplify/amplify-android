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

import android.util.Base64
import java.lang.Exception
import java.nio.charset.Charset
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.Throws

/**
 * Utility class to generate random string, hash, and encoding.
 */
object PkceHelper {

    /**
     * Generates a unique string.
     * @return the generated unique string.
     */
    fun generateRandom(): String {
        val randBytes = ByteArray(32)
        SecureRandom().nextBytes(randBytes)
        return Base64.encodeToString(
            randBytes,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
    }

    /**
     * Converts string into SHA-256 hash.
     * @param data Required: The [String] to hash.
     * @return the hash as a [String].
     */
    @Throws(Exception::class)
    fun generateHash(data: String): String {
        return try {
            val bytes = data.toByteArray(charset("US-ASCII"))
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(bytes, 0, bytes.size)
            val digestBytes = digest.digest()
            Base64.encodeToString(
                digestBytes,
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            )
        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * Encodes a string in Base-64.
     * @param str Required: String to encode.
     * @return Base-64 encoded string.
     */
    fun encodeBase64(str: String): String {
        val data = str.toByteArray(Charset.forName("ISO-8859-1"))
        return Base64.encodeToString(data, Base64.NO_PADDING or Base64.NO_WRAP)
    }
}
