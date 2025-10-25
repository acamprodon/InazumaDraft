package com.inazumadraft.ui

import android.widget.ImageView
import coil.load
import com.inazumadraft.model.PlayerImage

fun ImageView.loadPlayerImage(image: PlayerImage?) {
    when {
        image == null -> setImageDrawable(null)
        image.resourceId != 0 -> setImageResource(image.resourceId)
        !image.url.isNullOrBlank() -> load(image.url) {
            crossfade(true)
        }
        else -> setImageDrawable(null)
    }
}