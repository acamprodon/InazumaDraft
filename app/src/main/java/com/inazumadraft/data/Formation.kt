package com.inazumadraft.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Formation(
    val name: String,
    val positions: List<String> // PT + 10 jugadores
) : Parcelable

val formations = listOf(
    Formation("4-4-2", listOf("PT","DF","DF","DF","DF","MC","MC","MC","MC","DL","DL")),
    Formation("4-3-3", listOf("PT","DF","DF","DF","DF","MC","MC","MC","DL","DL","DL")),
    Formation("5-2-3", listOf("PT","DF","DF","DF","DF","DF","MC","MC","DL","DL","DL")),
    Formation("3-2-3-2", listOf("PT","DF","DF","DF","MC","MC","MC","MC","MC","DL","DL")),
    Formation("3-4-3", listOf("PT","DF","DF","DF","MC","MC","MC","MC","DL","DL","DL")),
    Formation("5-3-2", listOf("PT","DF","DF","DF","DF","DF","MC","MC","MC","DL","DL")),
    Formation("4-2-3-1", listOf("PT","DF","DF","DF","DF","MC","MC","MC","MC","MC","DL")),
    Formation("4-3-2-1", listOf("PT","DF","DF","DF","DF","MC","MC","MC","MC","MC","DL")),
    Formation("4-2-4", listOf("PT","DF","DF","DF","DF","MC","MC","DL","DL","DL","DL")),
    Formation("2-3-5", listOf("PT","DF","DF","MC","MC","MC","DL","DL","DL","DL","DL")),
    Formation("3-5-2", listOf("PT","DF","DF","DF","MC","MC","MC","MC","MC","DL","DL")),
    Formation("4-5-1", listOf("PT","DF","DF","DF","DF","MC","MC","MC","MC","MC","DL")),
    Formation("4-1-4-1", listOf("PT","DF","DF","DF","DF","MC","MC","MC","MC","MC","DL"))
)

/**
 * Coordenadas proporcionales (x, y) dentro del campo [0..1].
 * y=0.92 -> portero (abajo)
 * y=0.78 -> defensas
 * y=0.60 -> mediocentros/pivotes
 * y=0.45 -> interiores/medias
 * y=0.20 -> delanteros
 */
val formationCoordinates = mapOf(
    // 4-4-2
    "4-4-2" to listOf(
        0.50f to 0.92f,
        0.15f to 0.78f, 0.35f to 0.78f, 0.65f to 0.78f, 0.85f to 0.78f,
        0.15f to 0.52f, 0.35f to 0.52f, 0.65f to 0.52f, 0.85f to 0.52f,
        0.38f to 0.24f, 0.62f to 0.24f
    ),

    // 4-3-3
    "4-3-3" to listOf(
        0.50f to 0.92f,
        0.15f to 0.78f, 0.35f to 0.78f, 0.65f to 0.78f, 0.85f to 0.78f,
        0.25f to 0.58f, 0.50f to 0.58f, 0.75f to 0.58f,
        0.25f to 0.20f, 0.50f to 0.18f, 0.75f to 0.20f
    ),

    // 5-2-3
    "5-2-3" to listOf(
        0.50f to 0.92f,
        0.10f to 0.80f, 0.30f to 0.80f, 0.50f to 0.80f, 0.70f to 0.80f, 0.90f to 0.80f,
        0.40f to 0.58f, 0.60f to 0.58f,
        0.25f to 0.20f, 0.50f to 0.18f, 0.75f to 0.20f
    ),

    // 3-2-3-2
    "3-2-3-2" to listOf(
        0.50f to 0.92f,                                // PT
        0.22f to 0.82f, 0.50f to 0.80f, 0.78f to 0.82f, // DF (abren a los lados)
        0.38f to 0.66f, 0.62f to 0.66f,                 // doble pivote
        0.22f to 0.48f, 0.50f to 0.44f, 0.78f to 0.48f, // línea de 3 (centro algo más bajo)
        0.38f to 0.24f, 0.62f to 0.24f                  // 2 DL
    ),

    // 3-4-3
    "3-4-3" to listOf(
        0.50f to 0.92f,
        0.28f to 0.80f, 0.50f to 0.80f, 0.72f to 0.80f,
        0.20f to 0.58f, 0.40f to 0.54f, 0.60f to 0.54f, 0.80f to 0.58f,
        0.25f to 0.22f, 0.50f to 0.18f, 0.75f to 0.22f
    ),

    // 5-3-2
    "5-3-2" to listOf(
        0.50f to 0.92f,
        0.10f to 0.78f, 0.28f to 0.78f, 0.50f to 0.78f, 0.72f to 0.78f, 0.90f to 0.78f,
        0.30f to 0.55f, 0.50f to 0.55f, 0.70f to 0.55f,
        0.38f to 0.24f, 0.62f to 0.24f
    ),

    // 4-2-3-1
    "4-2-3-1" to listOf(
        0.50f to 0.92f,
        0.15f to 0.78f, 0.35f to 0.78f, 0.65f to 0.78f, 0.85f to 0.78f,
        0.40f to 0.64f, 0.60f to 0.64f,
        0.20f to 0.44f, 0.50f to 0.42f, 0.80f to 0.44f,
        0.50f to 0.20f
    ),

    // 4-3-2-1
    "4-3-2-1" to listOf(
        0.50f to 0.92f,
        0.15f to 0.78f, 0.35f to 0.78f, 0.65f to 0.78f, 0.85f to 0.78f,
        0.25f to 0.58f, 0.50f to 0.58f, 0.75f to 0.58f,
        0.40f to 0.35f, 0.60f to 0.35f,
        0.50f to 0.20f
    ),

    // 4-2-4
    "4-2-4" to listOf(
        0.50f to 0.92f,
        0.15f to 0.78f, 0.35f to 0.78f, 0.65f to 0.78f, 0.85f to 0.78f,
        0.40f to 0.60f, 0.60f to 0.60f,
        0.20f to 0.30f,    0.38f to 0.24f, 0.62f to 0.24f  , 0.80f to 0.30f
    ),

    // 2-3-5
    "2-3-5" to listOf(
        0.50f to 0.92f,                                // PT
        0.20f to 0.82f, 0.80f to 0.82f,                 // 2 DF muy abiertos
        0.18f to 0.62f, 0.50f to 0.58f, 0.82f to 0.62f, // 3 MC anchos y escalonados
        0.12f to 0.30f, 0.30f to 0.24f, 0.50f to 0.20f, 0.70f to 0.24f, 0.88f to 0.30f // 5 DL
    ),

    // 3-5-2
    "3-5-2" to listOf(
        0.50f to 0.92f,
        0.30f to 0.78f, 0.50f to 0.78f, 0.70f to 0.78f,
        0.15f to 0.60f, 0.35f to 0.56f, 0.50f to 0.54f, 0.65f to 0.56f, 0.85f to 0.60f,
        0.38f to 0.24f, 0.62f to 0.24f
    ),

    // 4-5-1
    "4-5-1" to listOf(
        0.50f to 0.92f,
        0.15f to 0.78f, 0.35f to 0.78f, 0.65f to 0.78f, 0.85f to 0.78f,
        0.15f to 0.58f, 0.35f to 0.54f, 0.50f to 0.52f, 0.65f to 0.54f, 0.85f to 0.58f,
        0.50f to 0.20f
    ),

    // 4-1-4-1
    "4-1-4-1" to listOf(
        0.50f to 0.92f,
        0.15f to 0.78f, 0.35f to 0.78f, 0.65f to 0.78f, 0.85f to 0.78f,
        0.50f to 0.65f,
        0.15f to 0.50f, 0.35f to 0.46f, 0.65f to 0.46f, 0.85f to 0.50f,
        0.50f to 0.20f
    )
)
