package com.flights.studio

import android.content.Context
import android.media.MediaPlayer

fun playSheetOpenSound(context: Context, soundRes: Int) {
    val player = MediaPlayer.create(context, soundRes)
    player.setOnCompletionListener {
        it.release() // free resources after playback
    }
    player.start()
}


