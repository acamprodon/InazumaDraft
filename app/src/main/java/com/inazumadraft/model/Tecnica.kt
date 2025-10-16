package com.inazumadraft.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Tecnica(
    val name: String,
    val type: String,
    val power: Int,
    val element: Int,
    val players: List<String>,
    val combined: Boolean = false

) : Parcelable

