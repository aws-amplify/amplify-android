/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test

class OauthConfigurationTest {

    @Test
    fun `fromJson with single redirect URI`() {
        val json = JSONObject().apply {
            put("AppClientId", "testClientId")
            put("WebDomain", "testDomain")
            put("Scopes", JSONArray(listOf("openid")))
            put("SignInRedirectURI", "https://example.com/signin")
            put("SignOutRedirectURI", "https://example.com/signout")
        }

        val config = OauthConfiguration.fromJson(json)
        assertNotNull(config)
        assertEquals("https://example.com/signin", config.signInRedirectURI)
        assertEquals("https://example.com/signout", config.signOutRedirectURI)
    }

    @Test
    fun `fromJson with multiple redirect URIs prefers non-HTTP scheme`() {
        val json = JSONObject().apply {
            put("AppClientId", "testClientId")
            put("WebDomain", "testDomain")
            put("Scopes", JSONArray(listOf("openid")))
            put("SignInRedirectURI", "https://example.com/signin,myapp://signin")
            put("SignOutRedirectURI", "myapp://signout,https://example.com/signout")
        }

        val config = OauthConfiguration.fromJson(json)
        assertNotNull(config)
        assertEquals("myapp://signin", config.signInRedirectURI)
        assertEquals("myapp://signout", config.signOutRedirectURI)
    }

    @Test
    fun `fromJson with multiple HTTP URIs uses first one`() {
        val json = JSONObject().apply {
            put("AppClientId", "testClientId")
            put("WebDomain", "testDomain")
            put("Scopes", JSONArray(listOf("openid")))
            put("SignInRedirectURI", "https://example.com/signin,http://localhost/callback")
            put("SignOutRedirectURI", "https://example.com/signout,http://localhost/logout")
        }

        val config = OauthConfiguration.fromJson(json)
        assertNotNull(config)
        assertEquals("https://example.com/signin", config.signInRedirectURI)
        assertEquals("https://example.com/signout", config.signOutRedirectURI)
    }

    @Test
    fun `fromJson with missing redirect URIs returns null`() {
        val json = JSONObject().apply {
            put("AppClientId", "testClientId")
            put("WebDomain", "testDomain")
            put("Scopes", JSONArray(listOf("openid")))
            // No redirect URIs
        }

        val config = OauthConfiguration.fromJson(json)
        assertNull(config)
    }
}
