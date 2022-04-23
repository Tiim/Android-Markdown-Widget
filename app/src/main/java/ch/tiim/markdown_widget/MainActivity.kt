package ch.tiim.markdown_widget

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity


private const val TAG = "MainActivity"
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
            
            * this is a list
            * list entry 2
        """.trimIndent()

        val debugLayout = findViewById<LinearLayout>(R.id.debugLayout)
        //val bitmap = Markdown(applicationContext).getBitmap(testTxt, debugLayout.width)
        val img = ImageView(applicationContext)
        //img.setImageBitmap(bitmap)
        debugLayout.addView(img)

        Markdown(applicationContext, 800, 500, testTxt) { bitmap ->
            img.setImageBitmap(bitmap)
        }
    }
}