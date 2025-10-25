package com.inazumadraft.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Player(
    val name: String,
    val nickname: String,
    val position: String,          // PT, DF, MC, DL (principal)
    val element: Int,
    val kick: Int,
    val speed: Int,
    val control: Int,
    val defense: Int,
    val image: PlayerImage,
    val season: List<String> = emptyList(),
    val secondaryPositions: List<String> = emptyList() ,
    val id: Long
) : Parcelable
@Parcelize
data class PlayerImage(
    val resourceId: Int = 0,
    val url: String? = null
) : Parcelable {
    val hasResource: Boolean get() = resourceId != 0
    val hasRemoteUrl: Boolean get() = !url.isNullOrBlank()
}
// Helper para multiposición
fun Player.canPlay(role: String): Boolean {
    val code = role.trim().uppercase()
    return position.equals(code, ignoreCase = true) ||
            secondaryPositions.any { it.equals(code, ignoreCase = true) }
}
