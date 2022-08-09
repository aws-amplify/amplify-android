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

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import com.amplifyframework.auth.cognito.activities.CustomTabsManagerActivity
import com.amplifyframework.auth.cognito.helpers.BrowserHelper
import com.amplifyframework.auth.cognito.helpers.HostedUIHttpHelper
import com.amplifyframework.auth.cognito.helpers.PkceHelper
import com.amplifyframework.statemachine.codegen.data.CognitoUserPoolTokens
import com.amplifyframework.statemachine.codegen.data.HostedUISignInOptions
import java.net.URL

class HostedUIClient(context: Context) : CustomTabsServiceConnection() {

    val proofKey = PkceHelper.generateRandom()
    val proofKeyHash = PkceHelper.generateHash(proofKey)
    val state = PkceHelper.generateRandom()

    var client: CustomTabsClient? = null
    var session: CustomTabsSession? = null

    private val defaultCustomTabsPackage: String?

    init {
        defaultCustomTabsPackage = BrowserHelper.getDefaultCustomTabPackage(context)?.also {
            preWarmCustomTabs(context, it)
        }
    }

    @Throws(RuntimeException::class)
    fun launchCustomTabs(activity: Activity, hostedUIOptions: HostedUISignInOptions) {
        if (!BrowserHelper.isBrowserInstalled(activity)) {
            throw RuntimeException("No browsers installed")
        }

        val uri = createHostedUIUri(hostedUIOptions)

        val browserPackage = hostedUIOptions.browserPackage ?: defaultCustomTabsPackage

        val customTabsIntent = CustomTabsIntent.Builder(session).build().apply {
            browserPackage?.let { intent.`package` = it }
            intent.data = uri
        }

        activity.startActivityForResult(
            CustomTabsManagerActivity.createStartIntent(activity, customTabsIntent.intent),
            CUSTOM_TABS_ACTIVITY_CODE
        )
    }

    fun fetchToken(uri: Uri, hostedUIOptions: HostedUISignInOptions): CognitoUserPoolTokens {
        val errorState = uri.getQueryParameter("error")
        val callbackState = uri.getQueryParameter("state")
        val code = uri.getQueryParameter("code")

        if (errorState != null) {
            // on error
            throw Exception()
        } else if (callbackState == null || code == null) {
            // on error
            throw Exception()
        }

        val fetchUrl = URL(
            Uri.Builder()
            .scheme("https")
            .authority(hostedUIOptions.domain)
            .appendPath("oauth2")
            .appendPath("token")
            .build().toString()
        )

        val headers = mutableMapOf("Content-Type" to "application/x-www-form-urlencoded").apply {
            if (hostedUIOptions.appSecret != null) {
                put(
                    "Authorization",
                    PkceHelper.encodeBase64("${hostedUIOptions.appClient}:${hostedUIOptions.appSecret}")
                )
            }
        }

        val body = mapOf(
            "grant_type" to "authorization_code",
            "client_id" to hostedUIOptions.appClient,
            "redirect_uri" to hostedUIOptions.signInRedirectURI,
            "code_verifier" to proofKey,
            "code" to code
        )

        return HostedUIHttpHelper.fetchTokens(fetchUrl, headers, body)
    }

    private fun createHostedUIUri(hostedUIOptions: HostedUISignInOptions): Uri {
        // Build the complete web domain to launch the login screen
        val builder = Uri.Builder()
            .scheme("https")
            .authority(hostedUIOptions.domain)
            .appendPath("oauth2")
            .appendPath("authorize")
            .appendQueryParameter("client_id", hostedUIOptions.appClient)
            .appendQueryParameter("redirect_uri", hostedUIOptions.signInRedirectURI)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("code_challenge", proofKeyHash)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("state", state)

        // check if identity provider set as param.
        (hostedUIOptions as? HostedUISignInOptions.SocialWebSignInOptions)
            ?.identityProvider?.takeIf { it.isNotEmpty() }?.let {
                builder.appendQueryParameter("identity_provider", it)
            }

        // check if idp identifier set as param.
        hostedUIOptions.idpIdentifier?.takeIf { it.isNotEmpty() }?.let {
            builder.appendQueryParameter("idp_identifier", it)
        }

        // Convert scopes into a string of space separated values.
        hostedUIOptions.scopes?.joinToString(" ")?.let {
            builder.appendQueryParameter("scope", it)
        }

        return builder.build()
    }

    override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
        this.client = client
        client.warmup(0L)
        session = client.newSession(null)
    }

    override fun onServiceDisconnected(name: ComponentName) {
        client = null
    }

    /**
     * Connects to Custom Tabs Service on the device.
     */
    private fun preWarmCustomTabs(context: Context, packageName: String) {
        CustomTabsClient.bindCustomTabsService(
            context,
            packageName,
            this
        )
    }

    companion object {
        const val CUSTOM_TABS_ACTIVITY_CODE = 49281
    }
}
