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

package com.amplifyframework.auth

/**
 * Wraps the various Cognito User Pool tokens.
 */
data class AWSCognitoUserPoolTokens constructor(
    /**
     * Returns the access JWT token in its encoded string form.
     * @return the access JWT token in its encoded string form.
     */
    val accessToken: String?,

    /**
     * Returns the ID JWT token in its encoded string form.
     * @return the ID JWT token in its encoded string form.
     */
    val idToken: String?,

    /**
     * Returns the refresh JWT token in its encoded string form.
     * @return the refresh JWT token in its encoded string form.
     */
    val refreshToken: String?
)
