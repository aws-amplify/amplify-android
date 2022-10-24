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
