/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.core.configuration

import com.amplifyframework.auth.AuthUserAttributeKey
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Deserializes strings like "EMAIL" into [AuthUserAttributeKey] instances with the correct key.
 */
internal object AuthUserAttributeKeySerializer : KSerializer<AuthUserAttributeKey> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("AuthUserAttributeKey", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): AuthUserAttributeKey {
        val keyString = decoder.decodeString().lowercase()
        return AuthUserAttributeKey.custom(keyString)
    }

    override fun serialize(encoder: Encoder, value: AuthUserAttributeKey) {
        encoder.encodeString(value.keyString)
    }
}
