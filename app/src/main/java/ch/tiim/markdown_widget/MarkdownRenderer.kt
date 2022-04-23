package ch.tiim.markdown_widget

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlin.math.max


private const val TAG = "MarkdownRenderer"
class MarkdownRenderer(private val context: Context, private val width: Int, private val height: Int, private val data: String, private val onReady: ((Bitmap) -> Unit) = {}) {

    private val mdParser = MarkdownParser("")
    val webView = WebView(context);
    private val bitmap = Bitmap.createBitmap(max(width, 100), max(height, 100), Bitmap.Config.ARGB_8888)
    private val canvas = Canvas(bitmap)
    private var ready = false

    init {
        val html = getHtml(data);
        prepareWebView(html)
    }

    private fun prepareWebView(
        html: String
    ) {
        webView.webViewClient = object: WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {

                val handler = Handler(Looper.getMainLooper())
                handler.postDelayed(Runnable {
                    try {
                        webView.draw(canvas);
                        ready = true
                        onReady(bitmap);
                    } catch (e: PendingIntent.CanceledException) {
                        e.printStackTrace()
                    }
                }, 50)
            }
        }
        webView.layout(0, 0, width, height)
        webView.setBackgroundColor(Color.WHITE)
        //webView.setBackgroundColor(Color.MAGENTA)
        val encodedHtml = Base64.encodeToString(html.toByteArray(), Base64.DEFAULT)
        webView.loadData(encodedHtml, "text/html", "base64")
        webView.isDrawingCacheEnabled = true
        webView.buildDrawingCache()
    }

    fun isReady():Boolean {
        return ready && webView.contentHeight != 0
    }

    fun getBitmap(): Bitmap {
        if (!ready) {
            Log.e(TAG, "WebView is not ready yet!")
        }
        return bitmap
    }

    private fun getHtml(data: String):String {
        return mdParser.parse(data);
    }

    fun needsUpdate(width: Int, height: Int, s: String):Boolean {
        return this.width != width || this.height != height || this.data != s
    }
}