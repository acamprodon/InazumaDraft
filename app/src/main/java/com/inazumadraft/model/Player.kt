package com.inazumadraft.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Player(
    val name: String,
    val nickname: String,
    val position: String,
    val element: Int,
    val kick: Int,
    val speed: Int,
    val control: Int,
    val defense: Int,
    val image: Int
) : Parcelable
