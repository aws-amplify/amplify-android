package com.amplifyframework.auth

/**
 * Wraps the various Cognito User Pool tokens.
 */
data class AWSCognitoUserPoolTokens constructor(
    /**
     * Returns the access JWT token in its encoded string form.
     * @return the access JWT token in its encoded string form.
     */
    val accessToken: String?,

    /**
     * Returns the ID JWT token in its encoded string form.
     * @return the ID JWT token in its encoded string form.
     */
    val idToken: String?,

    /**
     * Returns the refresh JWT token in its encoded string form.
     * @return the refresh JWT token in its encoded string form.
     */
    val refreshToken: String?
)
