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
        put(AppClientId, appClient)
        appSecret?.let { put(AppClientSecret, it) }
        put(WebDomain, domain)
        put(Scopes, JSONArray(scopes))
        put(SignInRedirectURI, signInRedirectURI)
        put(SignOutRedirectURI, signOutRedirectURI)
    }

    internal companion object {

        private const val AppClientId = "AppClientId"
        private const val AppClientSecret = "AppClientSecret"
        private const val WebDomain = "WebDomain"
        private const val Scopes = "Scopes"
        private const val SignInRedirectURI = "SignInRedirectURI"
        private const val SignOutRedirectURI = "SignOutRedirectURI"

        fun fromJson(jsonObject: JSONObject?): OauthConfiguration? {
            return jsonObject?.run {
                val appClient = optString(AppClientId).takeUnless { it.isNullOrEmpty() }
                val appSecret = optString(AppClientSecret, null).takeUnless { it.isNullOrEmpty() }
                val domain = optString(WebDomain).takeUnless { it.isNullOrEmpty() }
                val scopes = optJSONArray(Scopes)?.let { scopesArray ->
                    val scopesSet = mutableSetOf<String>()
                    for (i in 0 until scopesArray.length()) {
                        scopesArray.optString(i)?.let { scopesSet.add(it) }
                    }
                    scopesSet
                }
                val signInRedirectURI = optString(SignInRedirectURI).takeUnless { it.isNullOrEmpty() }
                val signOutRedirectURI = optString(SignOutRedirectURI).takeUnless { it.isNullOrEmpty() }

                return if (appClient != null && domain != null && scopes != null && signInRedirectURI != null &&
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
