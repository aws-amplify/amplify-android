package com.amplifyframework.auth.cognito.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.core.net.toUri
import androidx.core.os.BundleCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.amplifyframework.auth.cognito.R
import com.amplifyframework.core.Amplify
import com.google.android.material.appbar.MaterialToolbar

internal class WebViewActivity : AppCompatActivity(R.layout.activity_auth_webview) {

    private lateinit var webView: WebView
    private lateinit var startUri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.startUri = resolveStartUri(savedInstanceState) ?: run {
            cancel()
            return
        }

        setupSystemBars()
        setupToolbar()
        setupWebView()

        if (savedInstanceState != null) {
            this.webView.restoreState(savedInstanceState)
        } else {
            this.webView.loadUrl(this.startUri.toString())
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (webView.canGoBack()) webView.goBack() else cancel()
                }
            }
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.let { complete(it) }
    }

    override fun onPause() {
        super.onPause()
        CookieManager.getInstance().flush()
    }

    override fun onResume() {
        super.onResume()
        CookieManager.getInstance().flush()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        this.webView.saveState(outState)
        outState.putParcelable(EXTRA_START_URI, this.startUri)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_UI_HIDDEN) CookieManager.getInstance().flush()
    }

    private fun resolveStartUri(savedInstanceState: Bundle?): Uri? {
        val savedUri = savedInstanceState?.let {
            BundleCompat.getParcelable(it, EXTRA_START_URI, Uri::class.java)
        }

        return savedUri ?: IntentCompat.getParcelableExtra(intent, EXTRA_START_URI, Uri::class.java)
    }

    private fun setupSystemBars() {
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
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { cancel() }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView = findViewById<WebView>(R.id.authWebView).apply {
            settings.apply {
                cacheMode = WebSettings.LOAD_DEFAULT
                javaScriptEnabled = true
                domStorageEnabled = true
                builtInZoomControls = false
                displayZoomControls = false
                userAgentString = "$userAgentString AppWebView"
            }
            val cm = CookieManager.getInstance()
            cm.setCookie(
                "https://connect.om.fr",
                "X-Requested-With=WebView; Path=/; Domain=connect.om.fr; Secure; SameSite=None"
            )
            cm.setCookie(
                "https://connect.athena.om.fr",
                "X-Requested-With=WebView; Path=/; Domain=connect.athena.om.fr; Secure; SameSite=None"
            )
            cm.flush()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest): Boolean =
                    handleUri(request.url)

                @Deprecated("For pre-N")
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean = handleUri(url.toUri())

                override fun onPageFinished(view: WebView?, url: String?) {
                    CookieManager.getInstance().flush()
                }

                override fun onPageCommitVisible(view: WebView?, url: String?) {
                    CookieManager.getInstance().flush()
                }
            }
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
        }
    }

    private fun handleUri(uri: Uri): Boolean {
        if (isAllowedRedirectScheme(uri)) {
            complete(uri)
            return true
        }

        if (isSuccessHttps(uri)) {
            CookieManager.getInstance().flush()
            setResult(RESULT_OK, Intent().setData(uri))
            finish()
            return true
        }

        return when (uri.scheme) {
            "http", "https" -> false
            else -> {
                runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                true
            }
        }
    }

    private fun isAllowedRedirectScheme(uri: Uri): Boolean {
        val allowed = intent.getStringArrayExtra(EXTRA_REDIRECT_SCHEMES)?.toSet().orEmpty()
        return uri.scheme != null && uri.scheme in allowed
    }

    private fun isSuccessHttps(uri: Uri): Boolean {
        if (uri.scheme != "http" && uri.scheme != "https") return false
        val prefixes = intent.getStringArrayExtra(EXTRA_SUCCESS_URL_PREFIXES).orEmpty()
        return prefixes.any { uri.toString().startsWith(it) }
    }

    private fun complete(callbackUri: Uri) {
        CookieManager.getInstance().flush()
        setResult(RESULT_OK, Intent().setData(callbackUri))
        finish()
    }

    private fun cancel() {
        Amplify.Auth.handleWebUISignInResponse(null)
        setResult(RESULT_CANCELED)
        finish()
    }

    companion object {
        private const val EXTRA_START_URI = "extra_start_uri"
        private const val EXTRA_REDIRECT_SCHEMES = "extra_redirect_schemes"
        private const val EXTRA_SUCCESS_URL_PREFIXES = "extra_success_url_prefixes"
        fun createStartIntent(context: Context, startUri: Uri): Intent {
            val intent = Intent(context, WebViewActivity::class.java).apply {
                putExtra(EXTRA_START_URI, startUri)
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
