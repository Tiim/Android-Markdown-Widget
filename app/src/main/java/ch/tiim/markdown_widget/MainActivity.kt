package ch.tiim.markdown_widget

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.coffee).setOnClickListener(View.OnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.buymeacoffee.com/Tiim"))
            startActivity(browserIntent)
        })

        val testTxt = """
            # Test
            [[Wikilink]]
                
            * List
            * [ ] List2
            * [x] List 3
            
            ${'$'}${'$'}
            m = e\times c
            ${'$'}${'$'}
            
            ${'$'}a = b${'$'}
            
            | Header | Second Header |
            | -------------| --------------------------|
            | Data1 | First Row |
            | Data2 | Second Row |
        """.trimIndent()

        val debugLayout = findViewById<LinearLayout>(R.id.debugLayout)
        val webview = Markdown(applicationContext).getView(testTxt)

        webview.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,  LinearLayout.LayoutParams.MATCH_PARENT)
        debugLayout.addView(webview)
    }
}