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

import com.amplifyframework.statemachine.codegen.data.AuthConfiguration
import com.amplifyframework.statemachine.codegen.data.IdentityPoolConfiguration
import com.amplifyframework.statemachine.codegen.data.UserPoolConfiguration
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AWSCognitoAuthServiceBehaviorTest {

    // Robolectric needed due to internals of AWSCognitoAuthServiceBehavior initialization
    @Test
    fun verifyFromConfiguration() {
        val expectedIdentityPoolConfigRegion = "identity-pool-region"
        val expectedUserPoolRegion = "user-pool-region"
        val config = AuthConfiguration(
            identityPool = IdentityPoolConfiguration.builder()
                .poolId("pool-a")
                .region(expectedIdentityPoolConfigRegion)
                .build(),
            userPool = UserPoolConfiguration.Builder()
                .poolId("pool-b")
                .region(expectedUserPoolRegion)
                .build(),
            oauth = null
        )

        val testObject = AWSCognitoAuthServiceBehavior.fromConfiguration(config)

        assertEquals(expectedIdentityPoolConfigRegion, testObject.cognitoIdentityClient!!.config.region)
        assertEquals(expectedUserPoolRegion, testObject.cognitoIdentityProviderClient!!.config.region)
    }
}
