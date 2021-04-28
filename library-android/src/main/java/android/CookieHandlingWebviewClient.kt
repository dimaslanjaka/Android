package android

import android.annotation.TargetApi
import android.os.Build
import android.webkit.*
import android.webkit.CookieManager
import java.io.IOException
import java.net.*


class CookieHandlingWebviewClient() : WebViewClient() {
    var reloading = false
    val allCookies = mutableMapOf<URI, HttpCookie>()

    /**
     * <a href="https://stackoverflow.com/a/41121660">https://stackoverflow.com/a/41121660</a>
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
        if (request != null && request.url != null) {
            if (request.method.equals("get", ignoreCase = true)) {
                val scheme = request.url.scheme!!.trim { it <= ' ' }
                if (scheme.equals("http", ignoreCase = true) || scheme.equals("https", ignoreCase = true)) {
                    return executeRequest(request.url.toString())
                }
            }
        }
        return null
    }

    /**
     * <a href="https://stackoverflow.com/a/41121660">https://stackoverflow.com/a/41121660</a>
     */
    override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? {
        return url?.let { executeRequest(it) }
    }

    /**
     * <a href="https://stackoverflow.com/a/41121660">https://stackoverflow.com/a/41121660</a>
     */
    private fun executeRequest(url: String): WebResourceResponse? {
        try {
            val connection: URLConnection = URL(url).openConnection()
            connection.getHeaderField("Set-Cookie")?.let {
                parseCookie(connection, it, "Set-Cookie")
            }
            connection.getHeaderField("Set-Cookie2")?.let {
                parseCookie(connection, it, "Set-Cookie2")
            }
            return null
            //return new WebResourceResponse(connection.getContentType(), connection.getHeaderField("encoding"), connection.getInputStream());
        } catch (e: MalformedURLException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    fun parseCookie(connection: URLConnection, cookie: String, prefix: String = "Cookie Header") {
        println(prefix, cookie)
        val parse = HttpCookie.parse("Set-Cookie: $cookie")
        parse.forEach {
            allCookies[connection.url.toURI()] = it
        }
        //println(parse.size, gson.toJson(parse))
    }

    fun println(vararg msg: Any?) {
        if (msg.isNotEmpty()) {
            var i = 0
            msg.forEach { any ->
                i++
                android.Log.d(i.toString(), any.toString())
            }
        }
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        return super.shouldOverrideUrlLoading(view, request)
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError
    ) {
        super.onReceivedError(view, request, error)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            println("ERROR", request.url.toString())
        }
        // TODO: auto refresh webview
        if (!reloading) {
            view.reload();
        }
    }

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        CookieSyncManager.getInstance().sync()
        val cookies = CookieManager.getInstance().getCookie(url)
        //println("COOKIE", cookies ?: "NULL")
        //CookieHandling.instance.saveCookie(URI(url), cookies ?: "NULL=NULL")
        println(allCookies)
        reloading = false
    }
}