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
import com.amplifyframework.auth.AuthException
import kotlin.text.Charsets.UTF_8
import org.json.JSONObject

/**
 * Utility class for all operations on JWT.
 */
object JWTParser {
    private const val HEADER = 0
    private const val PAYLOAD = 1
    private const val SIGNATURE = 2
    private const val JWT_PARTS = 3

    /**
     * Returns header for a JWT as a JSON object.
     *
     * @param jwt valid JSON Web Token as String.
     * @return header as a JSONObject.
     */
    fun getHeader(jwt: String): JSONObject {
        return try {
            validateJWT(jwt)
            val sectionDecoded =
                Base64.decode(jwt.split(".").toTypedArray()[HEADER], Base64.URL_SAFE)
            val jwtSection = String(sectionDecoded, UTF_8)
            JSONObject(jwtSection)
        } catch (e: Exception) {
            throw AuthException(e.localizedMessage, "error in parsing JSON")
        }
    }

    /**
     * Returns payload of a JWT as a JSON object.
     *
     * @param jwt valid JSON Web Token as String.
     * @return payload as a JSONObject.
     */
    fun getPayload(jwt: String): JSONObject {
        return try {
            validateJWT(jwt)
            val payload = jwt.split(".").toTypedArray()[PAYLOAD]
            val sectionDecoded = Base64.decode(payload, Base64.URL_SAFE)
            val jwtSection = String(sectionDecoded, UTF_8)
            JSONObject(jwtSection)
        } catch (e: Exception) {
            throw AuthException(e.localizedMessage, "error in parsing JSON")
        }
    }

    /**
     * Returns signature of a JWT as a String.
     *
     * @param jwt valid JSON Web Token as String.
     * @return signature as a String.
     */
    fun getSignature(jwt: String): String {
        return try {
            validateJWT(jwt)
            val sectionDecoded =
                Base64.decode(jwt.split(".").toTypedArray()[SIGNATURE], Base64.URL_SAFE)
            String(sectionDecoded, UTF_8)
        } catch (e: Exception) {
            throw AuthException(e.localizedMessage, "error in parsing JSON")
        }
    }

    /**
     * Returns a claim, from the `JWT`s' payload, as a String.
     *
     * @param jwt valid JSON Web Token as String.
     * @param claim claim name as String.
     * @return claim from the JWT as a String.
     */
    fun getClaim(jwt: String, claim: String?): String? {
        if (jwt.isEmpty()) {
            return jwt
        }
        return try {
            val payload = getPayload(jwt)
            val claimValue = claim?.let { payload[claim] }
            claimValue.toString()
        } catch (e: Exception) {
            throw AuthException(e.localizedMessage, "invalid token")
        }
    }

    /**
     * Checks if a JWT token contains a claim.
     * @param jwt A string, possibly not event a JWT
     * @param key Key for a claim, e.g., "jti" or "aud"
     * @return True if JWT is a valid JWT and contains the requested claim, false otherwise
     */
    fun hasClaim(jwt: String, key: String?): Boolean {
        return try {
            getPayload(jwt).has(key)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if `JWT` is a valid JSON Web Token.
     *
     * @param jwt The JWT as a [String].
     */
    fun validateJWT(jwt: String) {
        // Check if the the JWT has the three parts
        val jwtParts = jwt.split(".").toTypedArray()
        if (jwtParts.size != JWT_PARTS) {
            throw AuthException("not a JSON web token", "error in parsing JSON")
        }
    }
}
