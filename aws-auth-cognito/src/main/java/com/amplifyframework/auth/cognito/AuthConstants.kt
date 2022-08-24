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

package com.amplifyframework.auth.cognito

object AuthConstants {
     const val KEY_SECRET_HASH = "SECRET_HASH"
     const val KEY_USERNAME = "USERNAME"
     const val VALUE_SMS_MFA = "SMS_MFA_CODE"
     const val VALUE_NEW_PASSWORD = "NEW_PASSWORD"
     const val VALUE_ANSWER= "ANSWER"
     const val KEY_PASSWORD_CLAIM_SECRET_BLOCK = "PASSWORD_CLAIM_SECRET_BLOCK"
     const val KEY_PASSWORD_CLAIM_SIGNATURE = "PASSWORD_CLAIM_SIGNATURE"
     const val KEY_TIMESTAMP = "TIMESTAMP"
     const val KEY_SALT = "SALT"
     const val KEY_SECRET_BLOCK = "SECRET_BLOCK"
    const val KEY_SRP_A = "SRP_A"
     const val KEY_SRP_B = "SRP_B"
     const val KEY_USER_ID_FOR_SRP = "USER_ID_FOR_SRP"
}