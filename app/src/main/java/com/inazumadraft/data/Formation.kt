package com.inazumadraft.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Formation(
    val name: String,
    val positions: List<String>
) : Parcelable

val formations = listOf(
    Formation(
        "1-2-1",
        listOf("PT", "DF", "MC", "MC", "DL")
    ),
    Formation(
        "2-1-1",
        listOf("PT", "DF", "DF", "MC", "DL")
    ),
    Formation(
        "1-1-2",
        listOf("PT", "DF", "MC", "DL", "DL")
    )
)

val formationCoordinates = mapOf(
    // PT al fondo, DF medio, mediocampistas y DL al frente
    "1-2-1" to listOf(
        Pair(0.5f, 0.9f), // PT
        Pair(0.5f, 0.65f), // DF
        Pair(0.35f, 0.4f), // MC izq
        Pair(0.65f, 0.4f), // MC der
        Pair(0.5f, 0.15f)  // DL
    ),
    "2-1-1" to listOf(
        Pair(0.5f, 0.9f), // PT
        Pair(0.35f, 0.7f), // DF izq
        Pair(0.65f, 0.7f), // DF der
        Pair(0.5f, 0.45f), // MC
        Pair(0.5f, 0.15f)  // DL
    ),
    "1-1-2" to listOf(
        Pair(0.5f, 0.9f), // PT
        Pair(0.5f, 0.65f), // DF
        Pair(0.5f, 0.4f), // MC
        Pair(0.35f, 0.15f), // DL izq
        Pair(0.65f, 0.15f)  // DL der
    )
)
