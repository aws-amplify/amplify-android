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

package com.amplifyframework.auth.cognito

import aws.sdk.kotlin.services.cognitoidentityprovider.model.WebAuthnCredentialDescription
import aws.smithy.kotlin.runtime.time.Instant
import com.amplifyframework.auth.AuthCodeDeliveryDetails
import com.amplifyframework.auth.AuthFactorType
import com.amplifyframework.auth.MFAType
import com.amplifyframework.auth.TOTPSetupDetails
import com.amplifyframework.auth.result.step.AuthNextSignInStep
import com.amplifyframework.auth.result.step.AuthSignInStep
import com.amplifyframework.statemachine.codegen.data.CognitoUserPoolTokens
import com.amplifyframework.statemachine.codegen.data.SignInMethod
import com.amplifyframework.statemachine.codegen.data.SignedInData
import java.util.Date

fun mockWebAuthnCredentialDescription(
    credentialId: String = "id",
    friendlyName: String = "name",
    relyingParty: String = "relyingParty",
    createdAt: Instant = Instant.now()
) = WebAuthnCredentialDescription {
    this.credentialId = credentialId
    this.createdAt = createdAt
    this.relyingPartyId = relyingParty
    friendlyCredentialName = friendlyName
    authenticatorTransports = emptyList()
}

fun mockAuthNextSignInStep(
    authSignInStep: AuthSignInStep = AuthSignInStep.DONE,
    additionalInfo: Map<String, String> = emptyMap(),
    authCodeDeliveryDetails: AuthCodeDeliveryDetails? = null,
    totpSetupDetails: TOTPSetupDetails? = null,
    allowedMFATypes: Set<MFAType>? = null,
    availableFactors: Set<AuthFactorType>? = null
) = AuthNextSignInStep(
    authSignInStep,
    additionalInfo,
    authCodeDeliveryDetails,
    totpSetupDetails,
    allowedMFATypes,
    availableFactors
)

internal fun mockSignedInData(
    userId: String = "userid",
    username: String = "username",
    signedInDate: Date = Date(),
    signInMethod: SignInMethod = SignInMethod.ApiBased(SignInMethod.ApiBased.AuthType.USER_SRP_AUTH),
    cognitoUserPoolTokens: CognitoUserPoolTokens =
        CognitoUserPoolTokens(idToken = null as String?, accessToken = null, refreshToken = null, expiration = null)
) = SignedInData(
    userId = userId,
    username = username,
    signedInDate = signedInDate,
    signInMethod = signInMethod,
    cognitoUserPoolTokens = cognitoUserPoolTokens
)
