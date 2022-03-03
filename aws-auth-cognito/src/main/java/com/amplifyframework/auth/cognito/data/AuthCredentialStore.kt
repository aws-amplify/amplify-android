package com.amplifyframework.auth.cognito.data

interface AuthCredentialStore {
    fun saveCredential(credential: AmplifyCredential)

    fun retrieveCredential(): AmplifyCredential?

    /**
     * This function helps in storing partial values into the credential store by copying existing values and updating only the ones that have changed.
     * @param cognitoUserPoolTokens userPoolTokens of a user
     * @param identityId the identityID of a user
     * @param awsCredentials the AWS Credentials of a user
     * */
    fun savePartialCredential(cognitoUserPoolTokens: CognitoUserPoolTokens? = null, identityId: String? = null, awsCredentials: AWSCredentials? = null)

    fun deleteCredential()
}

