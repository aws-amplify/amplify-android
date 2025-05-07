package com.amazonaws.sdk.appsync.events.utils

import android.content.Context
import com.amplifyframework.core.Resources

/**
 * Utility to load Cognito user credentials from a json document.
 * The document must be formatted as following:
 *
 * <pre>
 * {
 *   "username": "username123",
 *   "password": "kool@pass22"
 * }
 * </pre>
 */
object Credentials {
    private const val DEFAULT_ID = "credentials" // credentials.json
    private var credentials: Pair<String, String>? = null

    fun load(context: Context, identifier: String = DEFAULT_ID): Pair<String, String> {
        if (credentials == null) {
            credentials = synchronized(this) {
                val json = Resources.readJsonResource(context, identifier)
                val username = json.getString("username")
                val password = json.getString("password")
                username to password
            }
        }
        return credentials!!
    }
}
