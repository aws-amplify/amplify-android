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

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import com.amplifyframework.auth.cognito.activities.CustomTabsManagerActivity
import com.amplifyframework.auth.cognito.activities.WebViewActivity
import com.amplifyframework.auth.cognito.exceptions.service.CodeValidationException
import com.amplifyframework.auth.cognito.helpers.BrowserHelper
import com.amplifyframework.auth.cognito.helpers.HostedUIHttpHelper
import com.amplifyframework.auth.cognito.helpers.PkceHelper
import com.amplifyframework.auth.cognito.helpers.userPoolProviderName
import com.amplifyframework.logging.Logger
import com.amplifyframework.statemachine.codegen.data.CognitoUserPoolTokens
import com.amplifyframework.statemachine.codegen.data.HostedUIOptions
import com.amplifyframework.statemachine.codegen.data.OauthConfiguration
import java.net.URL

@SuppressLint("QueryPermissionsNeeded")
internal class HostedUIClient private constructor(
    private val context: Context,
    private val configuration: OauthConfiguration,
    private val logger: Logger
) :
    CustomTabsServiceConnection() {

    private val proofKey = PkceHelper.generateRandom()
    private val proofKeyHash = PkceHelper.generateHash(proofKey)
    private val state = PkceHelper.generateRandom()
    private var client: CustomTabsClient? = null
    private var session: CustomTabsSession? = null
    private val defaultCustomTabsPackage: String?

    init {
        defaultCustomTabsPackage = BrowserHelper.getDefaultCustomTabPackage(context)?.also {
            preWarmCustomTabs(context, it)
        }
    }

    @Throws(RuntimeException::class)
    fun launchCustomTabsSignIn(hostedUIOptions: HostedUIOptions) {
        launchCustomTabs(
            uri = createAuthorizeUri(hostedUIOptions),
            activity = hostedUIOptions.callingActivity,
            customBrowserPackage = hostedUIOptions.browserPackage
        )
    }

    @Throws(RuntimeException::class)
    fun launchCustomTabsSignOut(browserPackage: String?) {
        launchCustomTabs(
            uri = createSignOutUri(),
            customBrowserPackage = browserPackage
        )
    }

    @Throws(RuntimeException::class)
    fun launchWebViewSignIn(hostedUIOptions: HostedUIOptions) {
        launchWebView(
            uri = createAuthorizeUri(hostedUIOptions),
            activity = hostedUIOptions.callingActivity
        )
    }

    @Throws(RuntimeException::class)
    fun launchWebViewSignOut() {
        launchWebView(createSignOutUri())
    }

    private fun launchCustomTabs(uri: Uri, activity: Activity? = null, customBrowserPackage: String?) {
        if (!BrowserHelper.isBrowserInstalled(context)) {
            throw RuntimeException("No browsers installed")
        }

        val browserPackage = customBrowserPackage ?: defaultCustomTabsPackage

        val customTabsIntent = CustomTabsIntent.Builder(session).build().apply {
            browserPackage?.let { intent.`package` = it }
            intent.data = uri
        }

        val customTabIntent = CustomTabsManagerActivity.createStartIntent(context, customTabsIntent.intent)

        if (activity != null) {
            activity.startActivityForResult(customTabIntent, CUSTOM_TABS_ACTIVITY_CODE)
        } else {
            customTabIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(customTabIntent)
        }
    }

    private fun launchWebView(uri: Uri, activity: Activity? = null) {
        val webViewIntent = WebViewActivity.createStartIntent(uri, context)

        if (activity != null) {
            activity.startActivityForResult(webViewIntent, WEB_VIEW_ACTIVITY_CODE)
        } else {
            webViewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(webViewIntent)
        }
    }

    fun fetchToken(uri: Uri): CognitoUserPoolTokens {
        val errorState = uri.getQueryParameter("error")
        val callbackState = uri.getQueryParameter("state")
        val code = uri.getQueryParameter("code")

        if (errorState != null) {
            val errorDescription = uri.getQueryParameter("error_description")?.trim()
            val message = if (errorDescription?.isNotEmpty() == true) "$errorState: $errorDescription" else errorState
            throw CodeValidationException(message)
        } else if (callbackState == null || code == null) {
            throw CodeValidationException()
        }

        val body = mapOf(
            "grant_type" to "authorization_code",
            "client_id" to configuration.appClient,
            "redirect_uri" to configuration.signInRedirectURI,
            "code_verifier" to proofKey,
            "code" to code
        )

        return HostedUIHttpHelper.fetchTokens(createFetchTokenUrl(), createFetchTokenHeaders(), body)
    }

    private fun createAuthorizeUri(hostedUIOptions: HostedUIOptions): Uri {
        // Build the complete web domain to launch the login screen
        val builder = Uri.Builder()
            .scheme("https")
            .authority(configuration.domain)
            .appendPath("oauth2")
            .appendPath("authorize")
            .appendQueryParameter("client_id", configuration.appClient)
            .appendQueryParameter("redirect_uri", configuration.signInRedirectURI)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("code_challenge", proofKeyHash)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("state", state)

        // check if identity provider set as param.
        hostedUIOptions.providerInfo.authProvider?.userPoolProviderName?.takeIf { it.isNotEmpty() }?.let {
            builder.appendQueryParameter("identity_provider", it)
        }

        // check if idp identifier set as param.
        hostedUIOptions.providerInfo.idpIdentifier?.takeIf { it.isNotEmpty() }?.let {
            builder.appendQueryParameter("idp_identifier", it)
        }

        // use scopes passed in options or fallback to configuration scopes
        val scopes = hostedUIOptions.scopes?.ifEmpty {
            configuration.scopes.toList()
        }

        // Convert scopes into a string of space separated values.
        scopes?.joinToString(" ")?.let {
            builder.appendQueryParameter("scope", it)
        }

        return builder.build()
    }

    private fun createFetchTokenUrl() = URL(
        Uri.Builder()
            .scheme("https")
            .authority(configuration.domain)
            .appendPath("oauth2")
            .appendPath("token")
            .build().toString()
    )

    private fun createFetchTokenHeaders(): Map<String, String> =
        mutableMapOf("Content-Type" to "application/x-www-form-urlencoded").apply {
            if (configuration.appSecret != null) {
                put(
                    "Authorization",
                    "Basic ${PkceHelper.encodeBase64("${configuration.appClient}:${configuration.appSecret}")}"
                )
            }
        }

    internal fun createSignOutUri(): Uri = Uri.Builder()
        .scheme("https")
        .authority(configuration.domain).appendPath("logout")
        .appendQueryParameter("client_id", configuration.appClient)
        .appendQueryParameter("logout_uri", configuration.signOutRedirectURI)
        .build()

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
        const val WEB_VIEW_ACTIVITY_CODE = 49282

        fun create(context: Context, configuration: OauthConfiguration?, logger: Logger) = if (configuration != null) {
            HostedUIClient(context, configuration, logger)
        } else {
            null
        }
    }
}
