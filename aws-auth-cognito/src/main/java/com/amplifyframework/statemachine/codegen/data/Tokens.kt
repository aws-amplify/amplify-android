/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.statemachine.codegen.data

import android.util.Base64
import com.amplifyframework.auth.exceptions.UnknownException
import com.amplifyframework.statemachine.util.mask
import java.time.Instant
import kotlin.text.Charsets.UTF_8
import kotlinx.serialization.Serializable
import org.json.JSONObject
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

internal abstract class Jwt {
    abstract val tokenValue: String

    override fun toString() = tokenValue.mask()
    override fun hashCode() = tokenValue.hashCode()
    override fun equals(other: Any?) = other is Jwt && this.tokenValue == other.tokenValue

    private val parts by lazy {
        tokenValue.split(".").also {
            if (it.size != JWT_PARTS) {
                throw UnknownException("Not a JSON web token. Error in parsing JSON")
            }
        }
    }

    private val payload by lazy {
        try {
            val payload = parts[PAYLOAD]
            val sectionDecoded = Base64.decode(payload, Base64.URL_SAFE)
            val jwtSection = String(sectionDecoded, UTF_8)
            JSONObject(jwtSection)
        } catch (e: Exception) {
            throw UnknownException("${e.localizedMessage ?: ""}, error in parsing JSON")
        }
    }

    protected fun getClaim(claim: Claim): String? = try {
        if (payload.has(claim.key)) payload[claim.key].toString() else null
    } catch (e: Exception) {
        throw UnknownException("${e.localizedMessage ?: ""}, Invalid token")
    }

    internal companion object {
        private const val HEADER = 0
        private const val PAYLOAD = 1
        private const val SIGNATURE = 2
        private const val JWT_PARTS = 3
    }

    enum class Claim(val key: String) {
        Expiration("exp"),
        UserSub("sub"),
        Username("username"), // username claim in the access token
        TokenRevocationId("origin_jti"),
        CognitoUsername("cognito:username") // username claim in the identity token
    }
}

// See https://docs.aws.amazon.com/cognito/latest/developerguide/amazon-cognito-user-pools-using-the-id-token.html
@Serializable(with = IdTokenAsStringSerializer::class)
internal class IdToken(override val tokenValue: String) : Jwt() {
    val userSub: String?
        get() = getClaim(Claim.UserSub)
    val username: String?
        get() = getClaim(Claim.CognitoUsername)
    val expiration: Instant? by lazy {
        getClaim(Claim.Expiration)?.let { Instant.ofEpochSecond(it.toLong()) }
    }
}

// See https://docs.aws.amazon.com/cognito/latest/developerguide/amazon-cognito-user-pools-using-the-access-token.html
@Serializable(with = AccessTokenAsStringSerializer::class)
internal class AccessToken(override val tokenValue: String) : Jwt() {
    val tokenRevocationId: String?
        get() = getClaim(Claim.TokenRevocationId)
    val userSub: String?
        get() = getClaim(Claim.UserSub)
    val username: String?
        get() = getClaim(Claim.Username)
    val expiration: Instant? by lazy {
        getClaim(Claim.Expiration)?.let { Instant.ofEpochSecond(it.toLong()) }
    }
}

// Refresh token is just an opaque base64 string
@Serializable(with = RefreshTokenAsStringSerializer::class)
@JvmInline
internal value class RefreshToken(val tokenValue: String) {
    override fun toString() = tokenValue.mask()
}

internal fun String?.asIdToken() = this?.let { IdToken(it) }
internal fun String?.asAccessToken() = this?.let { AccessToken(it) }
internal fun String?.asRefreshToken() = this?.let { RefreshToken(it) }

/**
 * Contains cognito user pool JWT tokens
 * @param idToken User Pool id token
 * @param accessToken User Pool access token
 * @param refreshToken User Pool refresh token
 * @param expiration Auth result expiration but not token expiration
 */
@Serializable
internal data class CognitoUserPoolTokens(
    val idToken: IdToken?,
    val accessToken: AccessToken?,
    val refreshToken: RefreshToken?,
    val expiration: Long?
) {
    constructor(
        idToken: String?,
        accessToken: String?,
        refreshToken: String?,
        expiration: Long?
    ) : this(
        idToken = idToken.asIdToken(),
        accessToken = accessToken.asAccessToken(),
        refreshToken = refreshToken.asRefreshToken(),
        expiration = expiration
    )

    override fun equals(other: Any?): Boolean = if (super.equals(other)) {
        true
    } else if (other == null || javaClass != other.javaClass || other !is CognitoUserPoolTokens) {
        false
    } else {
        idToken == other.idToken && accessToken == other.accessToken && refreshToken == other.refreshToken
    }
}

/**
 * Helper function to extract token value from either flat or nested format
 */
private fun extractTokenValue(decoder: Decoder, tokenType: String): String {
    return if (decoder is JsonDecoder) {
        when (val element = decoder.decodeJsonElement()) {
            is JsonPrimitive -> element.content // Flat format: "token": "value"
            is JsonObject -> element["tokenValue"]?.jsonPrimitive?.content 
                ?: throw SerializationException("Missing tokenValue in nested $tokenType")
            else -> throw SerializationException("Expected string or object for $tokenType")
        }
    } else {
        decoder.decodeString() // Fallback for non-JSON decoders
    }
}

/**
 * Serializer for IdToken that maintains string serialization format
 */
internal object IdTokenAsStringSerializer : KSerializer<IdToken> {
    override val descriptor = String.serializer().descriptor
    override fun serialize(encoder: Encoder, value: IdToken) = encoder.encodeString(value.tokenValue)
    override fun deserialize(decoder: Decoder) = IdToken(extractTokenValue(decoder, "IdToken"))
}

/**
 * Serializer for AccessToken that maintains string serialization format
 */
internal object AccessTokenAsStringSerializer : KSerializer<AccessToken> {
    override val descriptor = String.serializer().descriptor
    override fun serialize(encoder: Encoder, value: AccessToken) = encoder.encodeString(value.tokenValue)
    override fun deserialize(decoder: Decoder) = AccessToken(extractTokenValue(decoder, "AccessToken"))
}

/**
 * Serializer for RefreshToken that maintains string serialization format
 */
internal object RefreshTokenAsStringSerializer : KSerializer<RefreshToken> {
    override val descriptor = String.serializer().descriptor
    override fun serialize(encoder: Encoder, value: RefreshToken) = encoder.encodeString(value.tokenValue)
    override fun deserialize(decoder: Decoder) = RefreshToken(extractTokenValue(decoder, "RefreshToken"))
}