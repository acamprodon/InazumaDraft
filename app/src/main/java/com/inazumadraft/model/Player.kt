package com.inazumadraft.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Player(
    val name: String,
    val position: String, // "Portero", "Defensa", "Centrocampista", "Delantero"
    val power: Int,
    val speed: Int,
    val technique: Int,
    val image: Int
) : Parcelable
