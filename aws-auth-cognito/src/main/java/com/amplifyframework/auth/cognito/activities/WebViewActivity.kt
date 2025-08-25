package com.amplifyframework.auth.cognito.activities

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.amplifyframework.auth.cognito.R
import com.amplifyframework.core.Amplify
import com.google.android.material.appbar.MaterialToolbar
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

internal class WebViewActivity: AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var webViewStartUri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            extractState(intent.extras)
            initializeView()
        } else {
            extractState(savedInstanceState)
            initializeView()
            webView.restoreState(savedInstanceState)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    handleAuthorizationCanceled()
                }
            }
        })
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onResume() {
        super.onResume()

        if (intent.data != null) {
            handleAuthorizationComplete()
            finish()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(WEB_VIEW_START_URI_KEY, webViewStartUri)
    }

    private fun initializeView() {
        setContentView(R.layout.activity_auth_webview)

        WindowCompat.setDecorFitsSystemWindows(window, true)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = false
        insetsController.isAppearanceLightNavigationBars = false
        val contentContainer: View = findViewById(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(contentContainer) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            WindowInsetsCompat.CONSUMED
        }

        // Toolbar
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)

        setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
        }

        toolbar.setNavigationOnClickListener {
            handleAuthorizationCanceled()
        }


        // Web view
        webView = findViewById<WebView?>(R.id.authWebView).apply {
            settings.apply {
                cacheMode = WebSettings.LOAD_NO_CACHE
                domStorageEnabled = true
                javaScriptEnabled = true
            }

            webViewClient = object: WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest): Boolean {
                    return handleRedirect(request.url)
                }

                @Deprecated("Deprecated in Java")
                @Suppress("OverridingDeprecatedMember")
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    return handleRedirect(url.toUri())
                }
            }

            loadUrl(webViewStartUri.toString())
        }
    }

    private fun extractState(state: Bundle?) {
        if (state == null) {
            Log.d(TAG, "WebViewActivity was created with a null state.")
            finish()
            return
        }

        webViewStartUri = state.getParcelable(WEB_VIEW_START_URI_KEY)!!
    }

    private fun handleAuthorizationComplete() {
        Log.d(TAG, "Authorization flow completed successfully")
        setResult(RESULT_OK, intent)
    }

    private fun handleAuthorizationCanceled() {
        Log.d(TAG, "Authorization flow canceled by user")

        Amplify.Auth.handleWebUISignInResponse(null)
        setResult(RESULT_CANCELED)
        finish()
    }

    private fun handleRedirect(uri: Uri): Boolean {
        return when (uri.scheme) {
            "http", "https" -> {
                false
            }
            else -> {
                val intent = Intent(Intent.ACTION_VIEW, uri)

                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                }

                true
            }
        }
    }

    companion object {
        private const val TAG = "AuthClient"
        private const val WEB_VIEW_START_URI_KEY = "webViewStartUri"

        /**
         * Creates an intent to start an OAuth2 flow in a Webkit web view.
         * @param context the package context for the app.
         */
        fun createStartIntent(uri: Uri, context: Context): Intent {
            val intent = Intent(context, WebViewActivity::class.java).apply {
                putExtra(WEB_VIEW_START_URI_KEY, uri)
            }
            return intent
        }

        /**
         * Creates an intent to handle the completion of an authorization flow. This restores
         * the original CustomTabsManagerActivity that was created at the start of the flow.
         * @param context the package context for the app.
         * @param responseUri the response URI, which carries the parameters describing the response.
         */
        @JvmStatic
        fun createResponseHandlingIntent(context: Context, responseUri: Uri?): Intent {
            val intent = Intent(context, WebViewActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                data = responseUri
            }
            return intent
        }
    }
}