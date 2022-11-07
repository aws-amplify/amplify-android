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

import com.amplifyframework.auth.AuthCategory
import com.amplifyframework.hub.HubCategory
import com.amplifyframework.hub.HubChannel

/**
 * An enumeration of the names of AWS Cognito specific events relating the [AuthCategory],
 * that are published via [HubCategory.publish] on the
 * [HubChannel.AUTH] channel.
 */
enum class AWSCognitoAuthChannelEventName {
    /**
     * Federation to Identity Pool has succeeded.
     */
    FEDERATED_TO_IDENTITY_POOL,

    /**
     * Federation to Identity Pool has been cleared
     */
    FEDERATION_TO_IDENTITY_POOL_CLEARED
}
