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

package com.amplifyframework.geo.maplibre

import androidx.test.core.app.ApplicationProvider
import com.amplifyframework.auth.AuthCategory
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.geo.GeoCategory
import com.amplifyframework.geo.location.AWSLocationGeoPlugin
import com.amplifyframework.testutils.sync.SynchronousAuth
import com.amplifyframework.testutils.sync.TestCategory

/**
 * Amplify can only be initialized once. This object singleton is used to create Amplify on the first run,
 * then use existing singleton for additional runs
 */
object AmplifyWrapper {

    private val awsCognitoAuthPlugin = AWSCognitoAuthPlugin()
    private val authCategory = TestCategory.forPlugin(awsCognitoAuthPlugin) as AuthCategory

    val auth: SynchronousAuth = SynchronousAuth.delegatingToCognito(
        ApplicationProvider.getApplicationContext(),
        awsCognitoAuthPlugin
    )
    val geo = TestCategory.forPlugin(AWSLocationGeoPlugin(authCategory = authCategory)) as GeoCategory
}