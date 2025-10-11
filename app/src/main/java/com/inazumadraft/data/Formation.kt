package com.inazumadraft.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Formation(
    val name: String,
    val positions: List<String> // PT, DF, MC, DL
) : Parcelable

// Formaciones de fútbol 7 (1 PT + 6 de campo)
val formations = listOf(
    // 3 DFs, 2 medios, 1 DL
    Formation(
        name = "3-2-1",
        positions = listOf(
            "PT",
            "DF", "DF", "DF",
            "MC", "MC",
            "DL"
        )
    ),
    // 2 DFs, 3 medios, 1 DL
    Formation(
        name = "2-3-1",
        positions = listOf(
            "PT",
            "DF", "DF",
            "MC", "MC", "MC",
            "DL"
        )
    ),
    // 3 DFs, 1 medio, 2 DLs
    Formation(
        name = "3-1-2",
        positions = listOf(
            "PT",
            "DF", "DF", "DF",
            "MC",
            "DL", "DL"
        )
    ),
    // 2 DFs, 2 medios, 2 DLs
    Formation(
        name = "2-2-2",
        positions = listOf(
            "PT",
            "DF", "DF",
            "MC", "MC",
            "DL", "DL"
        )
    )
)

/**
 * Coordenadas relativas (x, y) en el campo [0..1].
 * y=0.92 PT; líneas: DFs ~0.72, medios ~0.50, DLs ~0.15.
 * El orden de cada lista coincide con 'positions' de la formación.
 */
val formationCoordinates: Map<String, List<Pair<Float, Float>>> = mapOf(
    // 3-2-1: 3 DF en línea, 2 MC abiertos, 1 DL centrado
    "3-2-1" to listOf(
        0.50f to 0.92f, // PT
        0.20f to 0.72f, // DF izq
        0.50f to 0.72f, // DF centro
        0.80f to 0.72f, // DF der
        0.35f to 0.50f, // MC izq
        0.65f to 0.50f, // MC der
        0.50f to 0.15f  // DL centro
    ),

    // 2-3-1: 2 DF abiertos, 3 MC (izq-centro-der), 1 DL centrado
    "2-3-1" to listOf(
        0.50f to 0.92f, // PT
        0.30f to 0.72f, // DF izq
        0.70f to 0.72f, // DF der
        0.20f to 0.50f, // MC izq
        0.50f to 0.50f, // MC centro
        0.80f to 0.50f, // MC der
        0.50f to 0.15f  // DL centro
    ),

    // 3-1-2: 3 DF en línea, 1 MC centrado, 2 DL abiertos
    "3-1-2" to listOf(
        0.50f to 0.92f, // PT
        0.20f to 0.72f, // DF izq
        0.50f to 0.72f, // DF centro
        0.80f to 0.72f, // DF der
        0.50f to 0.50f, // MC centro
        0.35f to 0.15f, // DL izq
        0.65f to 0.15f  // DL der
    ),

    // 2-2-2: 2 DF, 2 MC, 2 DL (simétricos)
    "2-2-2" to listOf(
        0.50f to 0.92f, // PT
        0.30f to 0.72f, // DF izq
        0.70f to 0.72f, // DF der
        0.35f to 0.50f, // MC izq
        0.65f to 0.50f, // MC der
        0.35f to 0.15f, // DL izq
        0.65f to 0.15f  // DL der
    )
)
