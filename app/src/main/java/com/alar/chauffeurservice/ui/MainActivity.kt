package com.alar.chauffeurservice.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.*
import android.widget.ProgressBar
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.alar.chauffeurservice.AppConfig
import com.alar.chauffeurservice.R

class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private lateinit var progress: ProgressBar
    private lateinit var refresher: SwipeRefreshLayout
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        progress = findViewById(R.id.progress)
        refresher = findViewById(R.id.refresher)

        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.databaseEnabled = true
        webSettings.setSupportMultipleWindows(true)
        webSettings.javaScriptCanOpenWindowsAutomatically = true
        webSettings.userAgentString = webSettings.userAgentString + " AlarApp/1.0"
        webSettings.setGeolocationEnabled(true)
        webSettings.allowFileAccess = true
        webSettings.allowContentAccess = true
        webSettings.mediaPlaybackRequiresUserGesture = false
        webSettings.cacheMode = WebSettings.LOAD_DEFAULT

        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url ?: return false
                val host = url.host ?: return false
                if (AppConfig.ALLOWED_HOSTS.contains(host)) return false
                val scheme = url.scheme ?: "https"
                return try {
                    when (scheme) {
                        "tel", "mailto", "sms", "geo", "market", "intent", "whatsapp" -> {
                            startActivity(Intent(Intent.ACTION_VIEW, url)); true
                        }
                        else -> { startActivity(Intent(Intent.ACTION_VIEW, url)); true }
                    }
                } catch (_: ActivityNotFoundException) { false }
            }
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                progress.visibility = View.VISIBLE
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                progress.visibility = View.GONE
                refresher.isRefreshing = false
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                runOnUiThread { request.grant(request.resources) }
            }
            override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback?) {
                val fine = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION)
                val coarse = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION)
                callback?.invoke(origin, fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED, false)
            }
            override fun onShowFileChooser(
                webView: WebView?, filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                this@MainActivity.filePathCallback = filePathCallback
                return try {
                    startActivityForResult(fileChooserParams?.createIntent(), 101); true
                } catch (e: Exception) {
                    this@MainActivity.filePathCallback = null; false
                }
            }
        }

        refresher.setOnRefreshListener { webView.reload() }

        val deep = intent?.data
        if (deep != null) webView.loadUrl(deep.toString()) else webView.loadUrl(AppConfig.YOUR_DOMAIN)
    }

    override fun onBackPressed() {
        if (this::webView.isInitialized && webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101) {
            val result = when { data?.data != null -> arrayOf(data.data!!) else -> null }
            filePathCallback?.onReceiveValue(result)
            filePathCallback = null
        }
    }
}