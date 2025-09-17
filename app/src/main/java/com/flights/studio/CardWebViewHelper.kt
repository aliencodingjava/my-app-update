package com.flights.studio

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream

object CardWebViewHelper {

    private val adHosts = listOf("doubleclick.net", "googlesyndication.com")

    @SuppressLint("SetJavaScriptEnabled")
    fun setup(
        context: Context,
        cardId: String?,
        webViewContainer: FrameLayout,
        webView: WebView,
        errorImageView: ImageView? = null,
        errorReloadButton: MaterialButton? = null,
    ) {
        when (cardId) {
            "about_us" -> {
                inflateStaticPage(context, R.layout.about_us, webViewContainer) { view ->
                    view.findViewById<TextView>(R.id.hrContactText)?.setOnClickListener {
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SENDTO).apply {
                            data = "mailto:hr@jhairport.org".toUri()
                        }, "Send email via"))
                    }
                    view.findViewById<MaterialButton>(R.id.aboutUsJobApplication)
                        ?.setOnClickListener {
                            showWebDialog(context, context.getString(R.string.job_application_link))
                        }
                }
                return
            }

            "contact_us" -> {
                inflateStaticPage(context, R.layout.contact_us, webViewContainer) { view ->
                    view.findViewById<RecyclerView>(R.id.recyclerView)?.apply {
                        layoutManager = LinearLayoutManager(context)
                        adapter = ContactUsAdapter(
                            ContactUsAdapter.getContactUsDetails(context),
                            context
                        ) { lat, lng, _ ->
                            showWebDialog(
                                context,
                                "https://www.google.com/maps/search/?api=1&query=$lat,$lng"
                            )
                        }
                    }
                }
                return
            }
        }

        val url = when (cardId) {
            "card2" -> "https://www.jacksonholeairport.com/about/news/"
            "card3" -> "https://www.jacksonholeairport.com/flights/"
            "card4" -> "https://www.jacksonholeflightservices.com/"
            else -> "about:blank"
        }

        val activity = context as AppCompatActivity
        val containerView = activity.findViewById<FrameLayout?>(R.id.container)
        val progressBar = context.findViewById<ProgressBar>(R.id.webProgressBar)
        val networkHelper = NetworkConnectivityHelper(context)

        webView.apply {
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            alpha = 0f
            visibility = View.VISIBLE
            setInitialScale(1)
            setBackgroundColor(Color.TRANSPARENT)

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportZoom(false)
                builtInZoomControls = false
                displayZoomControls = false
                blockNetworkImage = false
                textZoom = 75
                cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117 Safari/537.36"
            }

            webViewClient = object : WebViewClient() {

                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    injectHideTriggers(view) // Inject CSS early
                }

                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?,
                ): WebResourceResponse? {
                    val url = request?.url.toString()
                    if (url.contains("scripts.min.js") || url.contains("trigger") || url.contains("footer")) {
                        return WebResourceResponse("application/javascript", "UTF-8", ByteArrayInputStream("".toByteArray()))
                    }

                    val host = request?.url?.host ?: return null
                    return if (adHosts.any { host.contains(it) }) {
                        WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
                    } else null
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    injectHideTriggers(view) // Reinforce hide after load

                    progressBar.animate().alpha(0f).setDuration(200).withEndAction {
                        progressBar.visibility = View.GONE
                        progressBar.alpha = 1f
                    }.start()

                    containerView?.visibility = View.GONE
                    errorImageView?.visibility = View.GONE
                    errorReloadButton?.visibility = View.GONE

                    animate().alpha(1f).setDuration(300).start()
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError,
                ) {
                    if (request.isForMainFrame) {
                        showErrorUi(webView, errorImageView, progressBar, errorReloadButton)
                    }
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    progressBar.visibility = if (newProgress < 100) View.VISIBLE else View.GONE
                    progressBar.progress = newProgress
                }
            }
        }

        CoroutineScope(Dispatchers.Main).launch {
            val hasInternet = networkHelper.isInternetAvailableFast()
            if (hasInternet) {
                errorImageView?.visibility = View.GONE
                errorReloadButton?.visibility = View.GONE
                webView.loadUrl(url)
                webViewContainer.removeAllViews()
                webViewContainer.addView(webView)
                progressBar.visibility = View.VISIBLE
            } else {
                showErrorUi(webView, errorImageView, progressBar, errorReloadButton)
                containerView?.visibility = View.VISIBLE
                UiUtils.showNoInternetDialog(context, errorImageView)
            }
        }
    }

    private fun injectHideTriggers(view: WebView?) {
        view?.evaluateJavascript(
            """
        (function() {
            var style = document.createElement('style');
            style.innerHTML = `
                .fixed-triggers {
                    display: none !important;
                    opacity: 0 !important;
                    height: 0 !important;
                    visibility: hidden !important;
                    position: absolute !important;
                    bottom: -9999px !important;
                    pointer-events: none !important;
                }
            `;
            document.head.appendChild(style);
        })();
        """.trimIndent(),
            null
        )
    }


    private fun inflateStaticPage(
        context: Context,
        layoutRes: Int,
        container: FrameLayout,
        setup: (View) -> Unit,
    ) {
        val view = LayoutInflater.from(context).inflate(layoutRes, container, false)
        container.removeAllViews()
        container.addView(view)
        setup(view)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun showWebDialog(context: Context, url: String) {
        val webView = WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            loadUrl("https://docs.google.com/viewer?embedded=true&url=$url")
        }

        Dialog(context).apply {
            setContentView(webView)
            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            show()
        }
    }

    private fun showErrorUi(
        webView: WebView,
        errorImageView: ImageView?,
        progressBar: ProgressBar,
        errorReloadButton: MaterialButton? = null,
    ) {
        progressBar.visibility = View.GONE
        webView.visibility = View.INVISIBLE
        errorImageView?.apply {
            alpha = 0f
            visibility = View.VISIBLE
            bringToFront()
            animate().alpha(1f).setDuration(500).start()
        }
        errorReloadButton?.visibility = View.VISIBLE
    }
}
