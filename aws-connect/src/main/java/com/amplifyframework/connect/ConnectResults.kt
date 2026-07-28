/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.connect

import com.amplifyframework.annotations.ExperimentalAmplifyApi
import com.amplifyframework.foundation.result.Result

/**
 * Data returned by a successful [AmplifyConnectClient.identifyUser] call. It
 * currently carries no fields and exists so the operation can grow its result
 * shape without a breaking change.
 */
@ExperimentalAmplifyApi
class IdentifyUserData internal constructor()

/** Result of [AmplifyConnectClient.identifyUser]. */
@ExperimentalAmplifyApi
typealias IdentifyUserResult = Result<IdentifyUserData, AmplifyConnectException>

/**
 * Data returned by a successful [AmplifyConnectClient.registerDevice] call. It
 * currently carries no fields and exists so the operation can grow its result
 * shape without a breaking change.
 */
@ExperimentalAmplifyApi
class RegisterDeviceData internal constructor()

/** Result of [AmplifyConnectClient.registerDevice]. */
@ExperimentalAmplifyApi
typealias RegisterDeviceResult = Result<RegisterDeviceData, AmplifyConnectException>

/**
 * Data returned by a successful [AmplifyConnectClient.removeDevice] call. It
 * currently carries no fields and exists so the operation can grow its result
 * shape without a breaking change.
 */
@ExperimentalAmplifyApi
class RemoveDeviceData internal constructor()

/** Result of [AmplifyConnectClient.removeDevice]. */
@ExperimentalAmplifyApi
typealias RemoveDeviceResult = Result<RemoveDeviceData, AmplifyConnectException>
