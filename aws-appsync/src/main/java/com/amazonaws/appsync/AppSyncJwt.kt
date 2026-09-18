/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.appsync

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okio.ByteString.Companion.decodeBase64

/**
 * Reads claims out of a JWT payload.
 *
 * The signature is deliberately **not** verified. These claims are used only to populate a variable
 * the service then authorizes for itself, so the token is being read for its content rather than
 * trusted — AppSync validates it independently.
 *
 * Decodes with okio, which arrives alongside OkHttp and handles base64url and absent padding. Nothing
 * here needs an Android runtime, so it can be exercised on a plain JVM.
 */
internal object AppSyncJwt {

    private const val EXPECTED_PARTS = 3
    private const val PAYLOAD_INDEX = 1

    /**
     * Decodes the payload of [token].
     *
     * @throws AppSyncTokenParsingException if the token is not a well-formed JWT.
     */
    fun payload(token: String): JsonObject {
        val parts = token.split('.')
        if (parts.size != EXPECTED_PARTS) {
            throw AppSyncTokenParsingException(
                message = "The token is not a JSON Web Token: expected $EXPECTED_PARTS " +
                    "dot-separated parts but found ${parts.size}."
            )
        }

        // JWT uses the base64url alphabet and omits padding; okio accepts both alphabets and tolerates
        // the missing padding, so no pre-processing is needed.
        val decoded = parts[PAYLOAD_INDEX].decodeBase64()?.utf8()
            ?: throw AppSyncTokenParsingException(
                message = "The token's payload is not valid base64."
            )

        return try {
            JsonParser.parseString(decoded).asJsonObject
        } catch (error: Exception) {
            throw AppSyncTokenParsingException(
                message = "The token's payload is not a JSON object.",
                cause = error
            )
        }
    }

    /**
     * Reads a string claim.
     *
     * @throws AppSyncAuthorizationClaimException if the claim is absent or is not a string. This is
     *   distinct from a parsing failure: the token was readable, it simply does not carry what the
     *   model's `@auth` rule requires.
     */
    fun stringClaim(payload: JsonObject, claim: String): String {
        val value = payload.get(claim)?.takeIf { it.isJsonPrimitive }
            ?: throw AppSyncAuthorizationClaimException(
                message = "The token does not contain the '$claim' claim, which the model's @auth rule " +
                    "requires to identify the owner."
            )
        return value.asString
    }

    /**
     * Reads a claim holding a list of group names. Returns empty when the claim is absent, since a user
     * simply belonging to no groups is normal rather than an error.
     */
    fun groupsClaim(payload: JsonObject, claim: String): List<String> {
        val value = payload.get(claim) ?: return emptyList()
        return when {
            value.isJsonArray -> value.asJsonArray.mapNotNull { it.takeIf { e -> e.isJsonPrimitive }?.asString }
            // A single group is sometimes emitted unwrapped.
            value.isJsonPrimitive -> listOf(value.asString)
            else -> emptyList()
        }
    }
}
