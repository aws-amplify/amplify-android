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

package com.amplifyframework.statemachine.codegen.actions

import com.amplifyframework.statemachine.Action
import com.amplifyframework.statemachine.codegen.data.AWSCredentials
import com.amplifyframework.statemachine.codegen.data.AmplifyCredential
import com.amplifyframework.statemachine.codegen.data.LoginsMapProvider
import com.amplifyframework.statemachine.codegen.data.SignedInData

internal interface FetchAuthSessionActions {
    fun refreshUserPoolTokensAction(signedInData: SignedInData): Action
    fun refreshAuthSessionAction(logins: LoginsMapProvider): Action
    fun fetchIdentityAction(loginsMap: LoginsMapProvider): Action
    fun fetchAWSCredentialsAction(identityId: String, loginsMap: LoginsMapProvider): Action
    fun notifySessionEstablishedAction(identityId: String, awsCredentials: AWSCredentials): Action
    fun notifySessionRefreshedAction(amplifyCredential: AmplifyCredential): Action
}
