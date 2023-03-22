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

package com.amplifyframework.statemachine.codegen.data

import com.amplifyframework.statemachine.codegen.data.serializer.DateSerializer
import java.util.Date
import kotlinx.serialization.Serializable

@Serializable
internal data class SignedInData(
    val userId: String,
    val username: String,
    @Serializable(DateSerializer::class)
    val signedInDate: Date,
    val signInMethod: SignInMethod,
    val cognitoUserPoolTokens: CognitoUserPoolTokens
) {
    override fun equals(other: Any?): Boolean {
        return if (super.equals(other)) {
            true
        } else if (other == null || javaClass != other.javaClass || other !is SignedInData) {
            false
        } else {
            userId == other.userId && username == other.username &&
                signInMethod == other.signInMethod &&
                cognitoUserPoolTokens == other.cognitoUserPoolTokens
        }
    }
}
