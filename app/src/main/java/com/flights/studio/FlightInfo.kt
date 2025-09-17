@file:Suppress("DEPRECATION")

package com.flights.studio

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.net.toUri

/* ───── 1️⃣  Model ─────────────────────────────────────────── */

internal sealed class Payload {
    data class Phone(val number: String) : Payload()
    data class Email(val address: String) : Payload()
    data class Maps(val query: String) : Payload()

    fun toIntent() = when (this) {
        is Phone -> Intent(Intent.ACTION_DIAL, "tel:$number".toUri())
        is Email -> Intent(Intent.ACTION_SENDTO, "mailto:$address".toUri())
        is Maps  -> Intent(
            Intent.ACTION_VIEW,
            "geo:0,0?q=${Uri.encode(query)}".toUri()
        ).setPackage("com.google.android.apps.maps")
    }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

/*  Row type must be visible because rows is public  */
internal data class Row(val label: String, val payload: Payload)

/* ───── 2️⃣  Widget provider ───────────────────────────────── */

class FlightInfo : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        manager: AppWidgetManager,
        ids: IntArray,
    ) = ids.forEach { updateAppWidget(context, manager, it) }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != CLICK_ACTION) return

        val pos = intent.getIntExtra(KEY_POS, -1).takeIf { it in rows.indices } ?: return
        Log.d("FlightWidget", "row $pos clicked")

        context.startActivity(rows[pos].payload.toIntent())
    }

    companion object {
        const val CLICK_ACTION = "com.flights.studio.CLICK_ACTION"
        const val KEY_POS = "ITEM_POSITION"

        /** labels + actions, 1-to-1 */
        internal val rows = listOf(
            Row("307 733 7682",                 Payload.Phone("3077337682")),
            Row("General E-mail",              Payload.Email("info@jhairport.org")),
            Row("Jackson Hole Airport",        Payload.Maps("Jackson Hole Airport N1250 East Airport Road")),
            Row("Airport Operations",          Payload.Email("operations@jhairport.org")),
            Row("Lost & Found",                Payload.Email("info@jhairport.org")),
            Row("Human Resources",             Payload.Email("hr@jhairport.org")),
            Row("Communications / CX",         Payload.Email("megan.jenkins@jhairport.org"))
        )
    }
}

/* ───── 3️⃣  RemoteViews service & factory ─────────────────── */

class FlightInfoRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        object : RemoteViewsFactory {

            override fun getCount() = FlightInfo.rows.size
            override fun onCreate() {}
            override fun onDestroy() {}
            override fun onDataSetChanged() {}
            override fun hasStableIds() = true
            override fun getViewTypeCount() = 1
            override fun getItemId(pos: Int) = pos.toLong()
            override fun getLoadingView(): RemoteViews? = null

            override fun getViewAt(pos: Int): RemoteViews {
                val ctx = applicationContext
                val rv  = RemoteViews(ctx.packageName, R.layout.list_item_flight_info)

                val row = FlightInfo.rows[pos]
                rv.setTextViewText(R.id.text_flight_info, row.label)

                when (row.payload) {
                    is Payload.Phone -> {
                        rv.setImageViewResource(R.id.icon, R.drawable.baseline_local_phone_24)
                        rv.setInt(R.id.strip, "setBackgroundResource", R.drawable.bg_strip_gradient)
                    }
                    is Payload.Email -> {
                        rv.setImageViewResource(R.id.icon, R.drawable.baseline_email_24)
                        rv.setInt(R.id.strip, "setBackgroundResource", R.drawable.bg_strip_gradient)
                    }
                    is Payload.Maps -> {
                        rv.setImageViewResource(R.id.icon, R.drawable.baseline_map_24)
                        rv.setInt(R.id.strip, "setBackgroundResource", R.drawable.bg_strip_gradient)
                    }
                }

                // attach extras so the row click is routed back
                val fillIn = Intent().putExtra(FlightInfo.KEY_POS, pos)
                rv.setOnClickFillInIntent(R.id.list_item_container, fillIn)

                return rv
            }

        }
}

/* ───── 4️⃣  Build the widget ─────────────────────────────── */

internal fun updateAppWidget(
    context: Context,
    manager: AppWidgetManager,
    id: Int,
) {
    val views = RemoteViews(context.packageName, R.layout.flight_info).apply {
        setRemoteAdapter(
            R.id.appwidget_list,
            Intent(context, FlightInfoRemoteViewsService::class.java)
        )

        val template = Intent(context, FlightInfo::class.java)
            .setAction(FlightInfo.CLICK_ACTION)

        val pi = PendingIntent.getBroadcast(
            context, 0, template,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        setPendingIntentTemplate(R.id.appwidget_list, pi)
    }
    manager.updateAppWidget(id, views)
}
