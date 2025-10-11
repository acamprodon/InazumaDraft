package com.inazumadraft.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Formation(
    val name: String,
    val positions: List<String> // 
) : Parcelable

// Lista de formaciones disponibles (el orden de 'positions' define el orden en el campo)
val formations = listOf(
    Formation(
        name = "1-2-1",
        positions = listOf(
            "PT",
            "DF",
            "MC",
            "MC",
            "DL"
        )
    ),
    Formation(
        name = "2-1-1",
        positions = listOf(
            "pt",
            "DF",
            "DF",
            "MC",
            "DL"
        )
    ),
    Formation(
        name = "1-1-2",
        positions = listOf(
            "PT",
            "DF",
            "MC",
            "DL",
            "DL"
        )
    )
)

/**
 * Coordenadas relativas (x, y) dentro del campo [0..1].
 * El índice de cada par debe coincidir con el orden en 'positions' de la formación.
 */
val formationCoordinates: Map<String, List<Pair<Float, Float>>> = mapOf(
    // 1 DF centrado, 2 MC abiertos, 1 DL centrado
    "1-2-1" to listOf(
        0.5f to 0.90f, // PT
        0.5f to 0.65f, // DF (centrado)
        0.30f to 0.45f, // MC (izquierda)
        0.70f to 0.45f, // MC (derecha)
        0.5f to 0.10f  // DL (centrado)
    ),

    // 2 DF abiertos, 1 MC centrado, 1 DL centrado
    "2-1-1" to listOf(
        0.5f to 0.90f, // PT
        0.30f to 0.70f, // DF (izquierda)
        0.70f to 0.70f, // DF (derecha)
        0.5f to 0.40f, // MC
        0.5f to 0.10f  // DL
    ),

    // 1 DF, 1 MC, 2 DL abiertos
    "1-1-2" to listOf(
        0.5f to 0.90f, // PT
        0.5f to 0.60f, // DF
        0.5f to 0.40f, // MC
        0.30f to 0.10f, // DL (izquierda)
        0.70f to 0.10f  // DL (derecha)
    )
)
