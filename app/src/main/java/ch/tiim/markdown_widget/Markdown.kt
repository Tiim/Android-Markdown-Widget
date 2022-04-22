package ch.tiim.markdown_widget

import android.content.Context
import android.text.Spanned
import android.text.style.ReplacementSpan
import android.util.Log
import io.noties.markwon.*
import io.noties.markwon.core.spans.EmphasisSpan
import io.noties.markwon.core.spans.LinkSpan
import io.noties.markwon.ext.latex.JLatexMathPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.inlineparser.MarkwonInlineParserPlugin
import io.noties.markwon.simple.ext.SimpleExtPlugin

private const val TAG = "Markdown"
class Markdown(private val context: Context) {
    fun makeMarkwon() : Markwon {
        return Markwon.builder(context)
            .usePlugin(TablePlugin.create(context))
            .usePlugin(TaskListPlugin.create(context))
            .usePlugin(MarkwonInlineParserPlugin.create())
            .usePlugin(JLatexMathPlugin.create(14F) {
                it.inlinesEnabled(true)
                it.errorHandler { latex, error ->
                    Log.e(TAG, "Error rendering latex", error)
                    null
                }
            })
            .usePlugin(SimpleExtPlugin.create {
                it.addExtension(1, '[', ']', wikilinks)
            })
            .build()
    }

    private val wikilinks = SpanFactory { configuration, props -> {
        Log.d(TAG, "Parsed Wikilinks")
        EmphasisSpan()
    } }

    fun getFormatted(data: String): Spanned {

        val markwon = makeMarkwon()
        val spanned = markwon.toMarkdown(data)

        return spanned
    }
}