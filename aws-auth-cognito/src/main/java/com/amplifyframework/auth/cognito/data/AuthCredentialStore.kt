package com.amplifyframework.auth.cognito.data

interface AuthCredentialStore {
    fun saveCredential(credential: AmplifyCredential)

    fun retrieveCredential(): AmplifyCredential?

    fun deleteCredential()
}

