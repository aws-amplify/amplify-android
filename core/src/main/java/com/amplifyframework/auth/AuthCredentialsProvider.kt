package com.amplifyframework.auth

import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider

interface AuthCredentialsProvider : CredentialsProvider {
    /**
     * Get the identity ID of the currently logged in user if they are registered in identity pools.
     * @return identity id
     */
    suspend fun getIdentityId(): String
}