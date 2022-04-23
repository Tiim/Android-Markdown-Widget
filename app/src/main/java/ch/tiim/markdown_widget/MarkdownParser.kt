package ch.tiim.markdown_widget

import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.ext.wikilink.WikiLink
import com.vladsch.flexmark.ext.wikilink.WikiLinkExtension
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Node

import com.vladsch.flexmark.util.data.MutableDataSet
import com.vladsch.flexmark.util.misc.Extension
import org.jetbrains.annotations.NotNull
import java.util.*

class MarkdownParser(private val theme:String) {

    val parser: Parser
    val renderer: HtmlRenderer
    init {
        val options = MutableDataSet()

        // uncomment to set optional extensions
        options.set(Parser.EXTENSIONS,
            Arrays.asList(
                TablesExtension.create(),
                StrikethroughExtension.create(),
                StrikethroughExtension.create(),
                TaskListExtension.create(),
                WikiLinkExtension.create(),
                YamlFrontMatterExtension.create(),
                ) as @NotNull Collection<Extension>
        );

        // uncomment to convert soft-breaks to hard breaks
        options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");

        parser = Parser.builder(options).build()
        renderer = HtmlRenderer.builder(options).build()
    }

    fun parse(md: String): String {


        val document: Node = parser.parse(md)
        val html = renderer.render(document)
        return """
            <!DOCTYPE html>
            <html>
                <head>
                    <styles>
                        ${theme}
                    </styles>
                </head>
                <body>
                    ${html}
                </body>
            </html>"""
    }
}