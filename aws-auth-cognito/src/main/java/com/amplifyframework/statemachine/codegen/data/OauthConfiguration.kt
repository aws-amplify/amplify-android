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

package com.amplifyframework.statemachine.codegen.data

import com.amplifyframework.annotations.InternalAmplifyApi
import com.amplifyframework.auth.cognito.helpers.HostedUIHelper
import org.json.JSONArray
import org.json.JSONObject

@InternalAmplifyApi
data class OauthConfiguration internal constructor(
    val appClient: String,
    val appSecret: String?,
    val domain: String,
    val scopes: Set<String>,
    val signInRedirectURI: String,
    val signOutRedirectURI: String
) {
    internal fun toGen1Json() = JSONObject().apply {
        put(APP_CLIENT_ID, appClient)
        appSecret?.let { put(APP_CLIENT_SECRET, it) }
        put(WEB_DOMAIN, domain)
        put(SCOPES, JSONArray(scopes))
        put(SIGN_IN_REDIRECT_URI, signInRedirectURI)
        put(SIGN_OUT_REDIRECT_URI, signOutRedirectURI)
    }

    internal companion object {

        private const val APP_CLIENT_ID = "AppClientId"
        private const val APP_CLIENT_SECRET = "AppClientSecret"
        private const val WEB_DOMAIN = "WebDomain"
        private const val SCOPES = "Scopes"
        private const val SIGN_IN_REDIRECT_URI = "SignInRedirectURI"
        private const val SIGN_OUT_REDIRECT_URI = "SignOutRedirectURI"

        fun fromJson(jsonObject: JSONObject?): OauthConfiguration? {
            return jsonObject?.run {
                val appClient = optString(APP_CLIENT_ID).takeUnless { it.isNullOrEmpty() }
                val appSecret = optString(APP_CLIENT_SECRET, null).takeUnless { it.isNullOrEmpty() }
                val domain = optString(WEB_DOMAIN).takeUnless { it.isNullOrEmpty() }
                val scopes = optJSONArray(SCOPES)?.let { scopesArray ->
                    val scopesSet = mutableSetOf<String>()
                    for (i in 0 until scopesArray.length()) {
                        scopesArray.optString(i)?.let { scopesSet.add(it) }
                    }
                    scopesSet
                }
                // Get redirect URIs and split by comma if multiple URIs are provided
                val signInRedirectURIs =
                    optString(SIGN_IN_REDIRECT_URI).takeUnless { it.isNullOrEmpty() }?.split(",") ?: emptyList()
                val signOutRedirectURIs =
                    optString(SIGN_OUT_REDIRECT_URI).takeUnless { it.isNullOrEmpty() }?.split(",") ?: emptyList()

                // Select appropriate redirect URIs (prefer non-HTTP/HTTPS URIs for mobile)
                val signInRedirectURI = HostedUIHelper.selectRedirectUri(signInRedirectURIs)
                val signOutRedirectURI = HostedUIHelper.selectRedirectUri(signOutRedirectURIs)

                return if (appClient != null &&
                    domain != null &&
                    scopes != null &&
                    signInRedirectURI != null &&
                    signOutRedirectURI != null
                ) {
                    OauthConfiguration(appClient, appSecret, domain, scopes, signInRedirectURI, signOutRedirectURI)
                } else {
                    return null
                }
            }
        }
    }
}
