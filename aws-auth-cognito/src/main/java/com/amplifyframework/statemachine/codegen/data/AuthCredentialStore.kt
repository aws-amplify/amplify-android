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

interface AuthCredentialStore {
    fun saveCredential(credential: AmplifyCredential)

    fun retrieveCredential(): AmplifyCredential?

    /**
     * This function helps in storing partial values into the credential store by copying existing values and updating only the ones that have changed.
     * @param cognitoUserPoolTokens userPoolTokens of a user
     * @param identityId the identityID of a user
     * @param awsCredentials the AWS Credentials of a user
     * */
    fun savePartialCredential(
        cognitoUserPoolTokens: CognitoUserPoolTokens? = null,
        identityId: String? = null,
        awsCredentials: AWSCredentials? = null
    )

    fun deleteCredential()
}
