package com.amplifyframework.auth.cognito

import com.amplifyframework.auth.AuthException

class RevokeTokenException(exception: Exception) : AuthException(
    "Failed to revoke token",
    exception,
    "See attached exception for more details. RevokeToken can be retried using the CognitoIdentityProviderClient " +
        "accessible from the escape hatch."
)

class GlobalSignOutException(exception: Exception) : AuthException(
    "Failed to sign out globally",
    exception,
    "See attached exception for more details. GlobalSignOut can be retried using the CognitoIdentityProviderClient " +
        "accessible from the escape hatch."
)
