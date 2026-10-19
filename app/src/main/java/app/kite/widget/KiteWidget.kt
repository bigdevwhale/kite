package app.kite.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import app.kite.MainActivity
import app.kite.R
import app.kite.localized

/** "I drifted off" on the home screen: one tap and you're reeling the string in. */
class KiteWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val l = context.localized()
        val views = RemoteViews(context.packageName, R.layout.widget_kite)
        views.setTextViewText(R.id.title, l.getString(R.string.drifted))
        views.setTextViewText(R.id.sub, l.getString(R.string.widget_sub))
        val open = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_OPEN, MainActivity.OPEN_SELF)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        views.setOnClickPendingIntent(R.id.root, PendingIntent.getActivity(context, 2, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
        manager.updateAppWidget(ids, views)
    }
}
