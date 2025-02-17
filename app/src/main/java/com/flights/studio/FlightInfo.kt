@file:Suppress("DEPRECATION")

package com.flights.studio

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import android.widget.Toast

class FlightInfo : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {}

    override fun onDisabled(context: Context) {}

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == CLICK_ACTION) {
            val position = intent.getIntExtra("ITEM_POSITION", -1)
            if (position != -1) {
                val widgetText = context.getString(R.string.clicked_item, position)
                Toast.makeText(context, widgetText, Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val CLICK_ACTION = "com.flights.studio.CLICK_ACTION"
    }
}

class FlightInfoRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return FlightInfoRemoteViewsFactory(applicationContext)
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val intent = Intent(context, FlightInfoRemoteViewsService::class.java)
    val views = RemoteViews(context.packageName, R.layout.flight_info)
    views.setRemoteAdapter(R.id.appwidget_list, intent)

    val clickIntent = Intent(context, FlightInfo::class.java)
    clickIntent.action = FlightInfo.CLICK_ACTION

    val clickPendingIntent = PendingIntent.getBroadcast(
        context, 0, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setPendingIntentTemplate(R.id.appwidget_list, clickPendingIntent)

    appWidgetManager.updateAppWidget(appWidgetId, views)
}
