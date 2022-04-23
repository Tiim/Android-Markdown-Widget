package ch.tiim.markdown_widget

import android.app.PendingIntent
import android.app.PendingIntent.CanceledException
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.content.res.Resources
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.util.Log
import android.util.SparseArray
import android.webkit.WebView
import android.widget.RemoteViews
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.InputStreamReader


private const val TAG = "MarkdownFileWidget"

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in [MarkdownFileWidgetConfigureActivity]
 */
class MarkdownFileWidget : AppWidgetProvider() {
    companion object {
        private val cachedMarkdown: SparseArray<Markdown> = SparseArray()
    }

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


    fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        WebView.enableSlowWholeDocumentDraw()
        val size = WidgetSizeProvider(context)
        val (width, height) = size.getWidgetsSize(appWidgetId)

        val tapBehavior = loadPref(context, appWidgetId, PREF_BEHAVIOUR, TAP_BEHAVIOUR_DEFAULT_APP)
        val fileUri = Uri.parse(loadPref(context, appWidgetId, PREF_FILE, ""))

        val s = loadMarkdown(context, fileUri)

        if (cachedMarkdown[appWidgetId] == null || cachedMarkdown[appWidgetId].needsUpdate(
                width,
                height,
                s
            )
        ) {
            cachedMarkdown.put(appWidgetId, Markdown(context, width, height, s))
        }
        val md = cachedMarkdown[appWidgetId]

        if (!md.isReady() || width == 0 || height == 0) {
            Log.d(TAG, "not ready!")
            val pendingUpdate = getUpdatePendingIntent(context, appWidgetId)
            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed(Runnable {
                try {
                    pendingUpdate.send()
                } catch (e: CanceledException) {
                    e.printStackTrace()
                }
            }, 300)
            return
        }

        Log.d(TAG, "is ready! :D :D")

        // Render textview to bitmap
        val views = RemoteViews(context.packageName, R.layout.markdown_file_widget)
        views.setImageViewBitmap(R.id.renderImg, md.getBitmap())

        //views.setImageViewBitmap(R.id.renderImg, bitmap)
        if (tapBehavior != TAP_BEHAVIOUR_NONE) {
            views.setOnClickPendingIntent(
                R.id.renderImg,
                getIntent(context, fileUri, tapBehavior, context.contentResolver)
            )
        }

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}

internal fun getUpdatePendingIntent(context: Context, appWidgetId: Int): PendingIntent {
    val intentUpdate = Intent(context, MarkdownFileWidget::class.java)
    intentUpdate.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
    val idArray = intArrayOf(appWidgetId)
    intentUpdate.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, idArray)
    val pendingUpdate = PendingIntent.getBroadcast(
        context,
        appWidgetId,
        intentUpdate,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    return pendingUpdate
}

fun getIntent(context: Context, uri: Uri, tapBehavior: String, c: ContentResolver): PendingIntent {
    val intent = Intent(Intent.ACTION_EDIT)
    if (tapBehavior == TAP_BEHAVIOUR_DEFAULT_APP) {
        intent.setDataAndType(uri.normalizeScheme(), "text/plain")
        //intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.flags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    } else if (tapBehavior == TAP_BEHAVIOUR_OBSIDIAN) {
        intent.data = Uri.parse("obsidian://open?file=" + Uri.encode(getFileName(uri, c)))
    }
    return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
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

fun loadMarkdown(context: Context, uri: Uri): String {
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
        val manager = AppWidgetManager.getInstance(context)
        val isPortrait = context.resources.configuration.orientation == ORIENTATION_PORTRAIT
        val (width, height) = listOf(
            if (isPortrait) AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH
            else AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,
            if (isPortrait) AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT
            else AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT
        ).map { manager.getAppWidgetOptions(widgetId).getInt(it, 0).dp.toInt() }

        Log.d(TAG, "Device size: $width $height")
        return width to height
    }

    private val Number.dp: Float get() = this.toFloat() * Resources.getSystem().displayMetrics.density
}