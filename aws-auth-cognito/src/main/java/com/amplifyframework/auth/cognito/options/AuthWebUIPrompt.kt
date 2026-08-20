package com.amplifyframework.auth.cognito.options

/**
 * An OIDC parameter that controls authentication behavior for existing sessions.
 */
public enum class AuthWebUIPrompt(val value: String) {
    /**
     * Amazon Cognito silently continues authentication for users who have a valid authenticated session.
     * With this prompt, users can silently authenticate between different app clients in your user pool.
     * If the user is not already authenticated, the authorization server returns a login_required error.
     */
    NONE(value = "none"),

    /**
     * Amazon Cognito requires users to re-authenticate even if they have an existing session. Send this
     * value when you want to verify the user's identity again. Authenticated users who have an existing
     * session can return to sign-in without invalidating that session. When a user who has an existing
     * session signs in again, Amazon Cognito assigns them a new session cookie. This parameter can also
     * be forwarded to your IdPs. IdPs that accept this parameter also request a new authentication
     * attempt from the user.
     */
    LOGIN(value = "login"),

    /**
     * This value has no effect on local sign-in and must be submitted in requests that redirect to IdPs.
     * When included in your authorization request, this parameter adds prompt=select_account to the URL
     * path for the IdP redirect destination. When IdPs support this parameter, they request that users
     * select the account that they want to log in with.
     */
    SELECT_ACCOUNT(value = "select_account"),

    /**
     * This value has no effect on local sign-in and must be submitted in requests that redirect to IdPs.
     * When included in your authorization request, this parameter adds prompt=consent to the URL path for
     * the IdP redirect destination. When IdPs support this parameter, they request user consent before
     * they redirect back to your user pool.
     */
    CONSENT(value = "consent")
}
