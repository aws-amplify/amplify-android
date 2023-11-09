/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.predictions.aws.models.liveness

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class LivenessResponseStream(
    @SerialName("ServerSessionInformationEvent") val serverSessionInformationEvent:
        ServerSessionInformationEvent? = null,
    @SerialName("DisconnectionEvent") val disconnectionEvent: DisconnectionEvent? = null,
    @SerialName("ValidationException") val validationException: ValidationException? = null,
    @SerialName("InternalServerException") val internalServerException: InternalServerException? = null,
    @SerialName("ThrottlingException") val throttlingException: ThrottlingException? = null,
    @SerialName("ServiceQuotaExceededException") val serviceQuotaExceededException:
        ServiceQuotaExceededException? = null,
    @SerialName("ServiceUnavailableException") val serviceUnavailableException: ServiceUnavailableException? = null,
    @SerialName("SessionNotFoundException") val sessionNotFoundException: SessionNotFoundException? = null,
    @SerialName("AccessDeniedException") val accessDeniedException: AccessDeniedException? = null
)
