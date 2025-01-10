/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.util

import aws.smithy.kotlin.runtime.http.config.HttpClientConfig
import aws.smithy.kotlin.runtime.http.engine.okhttp4.OkHttp4Engine
import com.amplifyframework.annotations.InternalAmplifyApi
import com.amplifyframework.core.Amplify

internal object AmplifyHttp {

    enum class Version {
        OkHttp4,
        OkHttp5
    }

    private val logger = Amplify.Logging.logger("HttpEngine")

    val availableVersion: Version by lazy {
        // Check to see if OkHttp4 Engine is available on the runtime classpath. If it is then the customer has
        // explicitly added it, so we can use that. Otherwise, use the OkHttp5 engine.
        try {
            Class.forName("aws.smithy.kotlin.runtime.http.engine.okhttp4.OkHttp4Engine")
            logger.info("Using OkHttp4 Engine")
            Version.OkHttp4
        } catch (e: ClassNotFoundException) {
            logger.info("Using default OkHttp5 Engine")
            Version.OkHttp5
        }
    }
}

/**
 * This function is used to determine, at runtime, whether we should use the OkHttp4Engine instead of the
 * default OkHttp5Engine with Kotlin SDK. This allows customers that cannot use OkHttp5 (which is currently an alpha
 * release) to use OkHttp4 throughout Amplify by adding a dependency on aws.smithy.kotlin:http-client-engine-okhttp4
 * to their runtime classpath.
 * This must be called when instantiating any Client instance from the Kotlin SDK.
 */
@InternalAmplifyApi
fun HttpClientConfig.Builder.setHttpEngine() {
    // The default engine is OkHttp5. If we should use OkHttp4 instead then override it here.
    if (AmplifyHttp.availableVersion == AmplifyHttp.Version.OkHttp4) {
        this.httpClient = OkHttp4Engine()
    }
}
