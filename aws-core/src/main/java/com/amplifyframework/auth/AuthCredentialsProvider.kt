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

import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import com.amplifyframework.annotations.InternalApiWarning
import com.amplifyframework.core.Consumer

@InternalApiWarning
interface AuthCredentialsProvider : CredentialsProvider {
    /**
     * Get the identity ID of the currently logged in user if they are registered in identity pools.
     * @return identity id
     */
    suspend fun getIdentityId(): String

    fun getAccessToken(onResult: Consumer<String>, onFailure: Consumer<Exception>)
}
