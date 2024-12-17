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

package com.amplifyframework.auth.cognito.featuretest.serializers

import com.amplifyframework.auth.result.AuthSignUpResult
import com.google.gson.Gson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * gson based AuthSignUpResult Serializer as part of AuthStatesSerializer.
 * This is needed for java class serialization with kotlinx.serialization library
 */
object AuthSignUpResultSerializer : KSerializer<AuthSignUpResult> {
    private val gson = Gson()

    override val descriptor: SerialDescriptor
        get() = buildClassSerialDescriptor("AuthSignUpResult")

    override fun deserialize(decoder: Decoder): AuthSignUpResult {
        val jsonString = decoder.decodeString()
        return gson.fromJson(jsonString, AuthSignUpResult::class.java)
    }

    override fun serialize(encoder: Encoder, value: AuthSignUpResult) {
        val jsonString = gson.toJson(value)
        encoder.encodeString(jsonString)
    }
}
