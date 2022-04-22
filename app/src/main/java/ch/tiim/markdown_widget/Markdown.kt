package ch.tiim.markdown_widget

import android.content.Context

import android.view.View
import android.webkit.WebView
import org.commonmark.node.Node
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer




private const val TAG = "Markdown"
class Markdown(private val context: Context) {
    fun getView(data: String): WebView {

        val parser: Parser = Parser.builder().build()
        val document: Node = parser.parse(data)
        val renderer = HtmlRenderer.builder().build()
        val html = renderer.render(document)

        val wv = WebView(context)
        val header = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
        wv.loadData(html, "text/html; charset=utf-8", "UTF-8")
        return wv
    }
}