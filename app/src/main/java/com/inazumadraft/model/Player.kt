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
    val image: Int,
    // 👇 NUEVO: posiciones adicionales (opcional, por defecto vacío)
    val secondaryPositions: List<String> = emptyList()
) : Parcelable

// Helper para multiposición
fun Player.canPlay(role: String): Boolean {
    val code = role.trim().uppercase()
    return position.equals(code, ignoreCase = true) ||
            secondaryPositions.any { it.equals(code, ignoreCase = true) }
}
