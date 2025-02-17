package com.flights.studio

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService

class FlightInfoRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {
    private val textValues = listOf(
        context.getString(R.string._307_733_7682),
        context.getString(R.string.info_jhairport_org),
        context.getString(R.string.jackson_hole_airport_n1250_east_airport_road),
        context.getString(R.string.PO_Box_n159Jackson_WY_n83001),
        context.getString(R.string.airport_operations),
        context.getString(R.string.lost_and_found),
        context.getString(R.string.human_resources),
        context.getString(R.string.communications_customer_experience)
    )

    override fun onCreate() {}

    override fun onDestroy() {}

    override fun getCount(): Int {
        return textValues.size
    }

    override fun getViewAt(position: Int): RemoteViews {
        if (position < 0 || position >= textValues.size) {
            throw IndexOutOfBoundsException("Invalid position: $position")
        }

        val views = RemoteViews(context.packageName, R.layout.list_item_flight_info)
        views.setTextViewText(R.id.text_flight_info, textValues[position])

        // Create an Intent for the click action
        val fillInIntent = Intent(context, YourWidgetReceiver::class.java)
        fillInIntent.action = "YOUR_WIDGET_CLICK_ACTION" // Define the action string for the receiver
        fillInIntent.putExtra("ITEM_POSITION", position) // Pass the item position
        views.setOnClickFillInIntent(R.id.list_item_container, fillInIntent) // Set the fill-in intent for the view

        return views
    }

    override fun getLoadingView(): RemoteViews? {
        return null
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    override fun onDataSetChanged() {}
}
