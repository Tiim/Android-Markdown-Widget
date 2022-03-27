package ch.tiim.markdown_widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.RemoteViews
import android.widget.TextView
import androidx.core.net.toFile
import io.noties.markwon.Markwon
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.InputStreamReader


/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in [MarkdownFileWidgetConfigureActivity]
 */
class MarkdownFileWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        if (context != null && appWidgetManager != null) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        // When the user deletes the widget, delete the preference associated with it.
        for (appWidgetId in appWidgetIds) {
            deletePrefs(context, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {

    val r = context.resources

    val size = WidgetSizeProvider(context)

    val (width, height) = size.getWidgetsSize(appWidgetId)

    val tapBehavior = loadPref(context, appWidgetId, PREF_BEHAVIOUR, TAP_BEHAVIOUR_DEFAULT_APP)
    val fileUri = Uri.parse(loadPref(context, appWidgetId, PREF_FILE, ""))

    val s = loadMarkdown(context, fileUri)
    val markwon = Markwon.create(context);
    val spanned = markwon.toMarkdown(s)

    // Create textview
    val text = TextView(context)
    text.text = spanned
    text.layout(0,0, width.toInt(),height.toInt())

    // Render textview to bitmap
    val bitmap = Bitmap.createBitmap(width.toInt(),height.toInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    text.draw(canvas)
    val views = RemoteViews(context.packageName, R.layout.markdown_file_widget)
    views.setImageViewBitmap(R.id.renderImg, bitmap)
    if (tapBehavior != TAP_BEHAVIOUR_NONE) {
        views.setOnClickPendingIntent(
            R.id.renderImg,
            getIntent(context, fileUri, tapBehavior, context.contentResolver)
        )
    }
    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
}

fun getIntent(context: Context, uri: Uri, tapBehavior: String, c: ContentResolver): PendingIntent {
    val intent = Intent(Intent.ACTION_VIEW)
    if (tapBehavior == TAP_BEHAVIOUR_DEFAULT_APP) {
        intent.setDataAndType(uri.normalizeScheme(), "text/*")
        //intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
    } else if (tapBehavior == TAP_BEHAVIOUR_OBSIDIAN) {
        intent.data = Uri.parse("obsidian://open?file=" + Uri.encode(getFileName(uri, c)) )
    }
    return PendingIntent.getActivity(context, 0, intent, 0)
}

fun getFileName(uri: Uri, c: ContentResolver): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor: Cursor? = c.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val i = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                result = cursor.getString(i)
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result!!.lastIndexOf('/')
        if (cut != -1) {
            result = result.substring(cut + 1)
        }
    }
    return result
}

fun loadMarkdown(context: Context, uri : Uri): String {
    try {
        val ins: InputStream = context.contentResolver.openInputStream(uri)!!
        val reader = BufferedReader(InputStreamReader(ins, "utf-8"))
        val data = reader.lines().reduce { s, t -> s + "\n" + t }
        return data.get()
    } catch (err: FileNotFoundException) {
        return ""
    }
}


class WidgetSizeProvider(
    private val context: Context // Do not pass Application context
) {

    private val appWidgetManager = AppWidgetManager.getInstance(context)

    fun getWidgetsSize(widgetId: Int): Pair<Int, Int> {
        val isPortrait = context.resources.configuration.orientation == ORIENTATION_PORTRAIT
        val width = getWidgetWidth(isPortrait, widgetId)
        val height = getWidgetHeight(isPortrait, widgetId)
        val widthInPx = context.dip(width)
        val heightInPx = context.dip(height)
        return widthInPx to heightInPx
    }

    private fun getWidgetWidth(isPortrait: Boolean, widgetId: Int): Int =
        if (isPortrait) {
            getWidgetSizeInDp(widgetId, AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        } else {
            getWidgetSizeInDp(widgetId, AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
        }

    private fun getWidgetHeight(isPortrait: Boolean, widgetId: Int): Int =
        if (isPortrait) {
            getWidgetSizeInDp(widgetId, AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        } else {
            getWidgetSizeInDp(widgetId, AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        }

    private fun getWidgetSizeInDp(widgetId: Int, key: String): Int =
        appWidgetManager.getAppWidgetOptions(widgetId).getInt(key, 0)

    private fun Context.dip(value: Int): Int = (value * resources.displayMetrics.density).toInt()

}