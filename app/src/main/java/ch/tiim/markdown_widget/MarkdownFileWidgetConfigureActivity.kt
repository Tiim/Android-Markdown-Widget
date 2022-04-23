package ch.tiim.markdown_widget

import android.Manifest
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.RadioGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import ch.tiim.markdown_widget.databinding.MarkdownFileWidgetConfigureBinding

/**
 * The configuration screen for the [MarkdownFileWidget] AppWidget.
 */
internal const val TAP_BEHAVIOUR_NONE = "none"
internal const val TAP_BEHAVIOUR_DEFAULT_APP = "default_app"
internal const val TAP_BEHAVIOUR_OBSIDIAN = "obsidian"

private const val ACTIVITY_RESULT_BROWSE = 1

internal const val PREF_FILE = "filepath"
internal const val PREF_BEHAVIOUR = "behaviour"

class MarkdownFileWidgetConfigureActivity : Activity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var inputFilePath: EditText
    private lateinit var radioGroup: RadioGroup
    private val onBrowse = View.OnClickListener {
        // https://developer.android.com/reference/android/content/Intent#ACTION_OPEN_DOCUMENT
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/markdown"
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION.or( Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(Intent.createChooser(intent, "Select a markdown file"), ACTIVITY_RESULT_BROWSE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if( requestCode == ACTIVITY_RESULT_BROWSE && resultCode == RESULT_OK && data?.data != null) {
            val uri: Uri = data.data!!;

            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

            val context = this@MarkdownFileWidgetConfigureActivity
            val text = uri.toString()
            inputFilePath.setText(text.toCharArray(), 0, text.length)

            savePref(context, appWidgetId, PREF_FILE, text)
        }
    }

    private val onAddWidget = View.OnClickListener {
        val context = this@MarkdownFileWidgetConfigureActivity

        // When the button is clicked, store the string locally
        val widgetText = inputFilePath.text.toString()
        savePref(context, appWidgetId, "filepath" , widgetText)

        val rID = radioGroup.checkedRadioButtonId
        val tapBehaviour = when (rID) {
            R.id.radio_noop -> {
                TAP_BEHAVIOUR_NONE
            }
            R.id.radio_obsidian -> {
                TAP_BEHAVIOUR_OBSIDIAN
            }
            else -> {
                TAP_BEHAVIOUR_DEFAULT_APP
            }
        }
        savePref(context, appWidgetId, "behaviour", tapBehaviour)


        // It is the responsibility of the configuration activity to update the app widget
        val appWidgetManager = AppWidgetManager.getInstance(context)

        getUpdatePendingIntent(context, appWidgetId).send()

        // Make sure we pass back the original appWidgetId
        val resultValue = Intent()
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, resultValue)
        finish()
    }
    private lateinit var binding: MarkdownFileWidgetConfigureBinding

    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED)

        binding = MarkdownFileWidgetConfigureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        inputFilePath = binding.inputFile
        radioGroup = binding.radiogroup
        binding.addButton.setOnClickListener(onAddWidget)
        binding.btnBrowse.setOnClickListener(onBrowse)
        binding.radioDefaultApp.isSelected = true


        // Find the widget id from the intent.
        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        // If this activity was started with an intent without an app widget ID, finish with an error.
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

    }

}

private const val PREFS_NAME = "ch.tiim.markdown_widget.MarkdownFileWidget"
private const val PREF_PREFIX_KEY = "appwidget_"

// Write the prefix to the SharedPreferences object for this widget
internal fun savePref(context: Context, appWidgetId: Int, prefName: String, text: String) {
    val prefs = context.getSharedPreferences(PREFS_NAME, 0).edit()
    prefs.putString("$PREF_PREFIX_KEY$appWidgetId--$prefName", text)
    prefs.apply()
}

// Read the prefix from the SharedPreferences object for this widget.
// If there is no preference saved, use default
internal fun loadPref(context: Context, appWidgetId: Int, prefName: String, default: String): String {
    val prefs = context.getSharedPreferences(PREFS_NAME, 0)
    val titleValue = prefs.getString("$PREF_PREFIX_KEY$appWidgetId--$prefName", null)
    return titleValue ?: default
}

internal fun deletePrefs(context: Context, appWidgetId: Int) {
    val prefs = context.getSharedPreferences(PREFS_NAME, 0).edit()
    prefs.remove("$PREF_PREFIX_KEY$appWidgetId--$PREF_BEHAVIOUR")
    prefs.remove("$PREF_PREFIX_KEY$appWidgetId--$PREF_FILE")
    prefs.apply()
}