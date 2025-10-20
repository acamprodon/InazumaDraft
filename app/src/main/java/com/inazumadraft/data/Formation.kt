package com.inazumadraft.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Formation(
    val name: String,
    val positions: List<String> // PT + 10 jugadores
) : Parcelable
private fun defaultSpread(count: Int): Float = when (count) {
    1 -> 0f
    2 -> 0.36f
    3 -> 0.56f
    4 -> 0.72f
    5 -> 0.82f
    else -> 0.76f
}

private fun row(y: Float, count: Int, spread: Float? = null): List<Pair<Float, Float>> {
    if (count <= 0) return emptyList()
    if (count == 1) return listOf(0.50f to y)

    val width = spread ?: defaultSpread(count)
    val start = 0.50f - width / 2f
    val step = width / (count - 1)

    return List(count) { index -> (start + step * index) to y }
}

private fun formation(vararg rows: List<Pair<Float, Float>>): List<Pair<Float, Float>> =
    rows.flatMap { it }

val formations = listOf(
    Formation("4-4-2", listOf("PT","DF","DF","DF","DF","MC","MC","MC","MC","DL","DL")),
    Formation("4-3-3", listOf("PT","DF","DF","DF","DF","MC","MC","MC","DL","DL","DL")),
    Formation("5-2-3", listOf("PT","DF","DF","DF","DF","DF","MC","MC","DL","DL","DL")),
    Formation("3-4-3", listOf("PT","DF","DF","DF","MC","MC","MC","MC","DL","DL","DL")),
    Formation("5-3-2", listOf("PT","DF","DF","DF","DF","DF","MC","MC","MC","DL","DL")),
    Formation("4-2-3-1", listOf("PT","DF","DF","DF","DF","MC","MC","MC","MC","MC","DL")),
    Formation("4-3-2-1", listOf("PT","DF","DF","DF","DF","MC","MC","MC","MC","MC","DL")),
    Formation("4-2-4", listOf("PT","DF","DF","DF","DF","MC","MC","DL","DL","DL","DL")),
    Formation("2-3-5", listOf("PT","DF","DF","MC","MC","MC","DL","DL","DL","DL","DL")),
    Formation("3-5-2", listOf("PT","DF","DF","DF","MC","MC","MC","MC","MC","DL","DL")),
    Formation("4-5-1", listOf("PT","DF","DF","DF","DF","MC","MC","MC","MC","MC","DL")),
    Formation("4-1-4-1", listOf("PT","DF","DF","DF","DF","MC","MC","MC","MC","MC","DL")),
    Formation ("5-4-1", listOf("PT","DF","DF","DF","DF","DF","MC","MC","MC","MC","DL"))
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
    "4-4-2" to formation(
        row(0.92f, 1),
        row(0.80f, 4),
        row(0.60f, 4),
        row(0.28f, 2, spread = 0.40f)
    ),

    // 4-3-3
        "4-3-3" to formation(
            row(0.92f, 1),
            row(0.80f, 4),
            row(0.62f, 3),
            row(0.28f, 3, spread = 0.68f)
    ),

    // 5-2-3
        "5-2-3" to formation(
            row(0.92f, 1),
            row(0.82f, 5),
            row(0.62f, 2, spread = 0.40f),
            row(0.28f, 3, spread = 0.68f)
    ),


    // 3-4-3
        "3-4-3" to formation(
            row(0.92f, 1),
            row(0.82f, 3, spread = 0.60f),
            row(0.58f, 4, spread = 0.74f),
            row(0.28f, 3, spread = 0.68f)
    ),

    // 5-3-2
    "5-3-2" to formation(
        row(0.92f, 1),
        row(0.82f, 5),
        row(0.60f, 3),
        row(0.28f, 2, spread = 0.40f)
    ),

    // 4-2-3-1
    "4-2-3-1" to formation(
        row(0.92f, 1),
        row(0.80f, 4),
        row(0.64f, 2, spread = 0.36f),
        row(0.48f, 3, spread = 0.60f),
        row(0.26f, 1)
    ),

    // 4-3-2-1
        "4-3-2-1" to formation(
            row(0.92f, 1),
            row(0.80f, 4),
            row(0.60f, 3),
            row(0.44f, 2, spread = 0.38f),
            row(0.26f, 1)
    ),

    // 4-2-4
    "4-2-4" to formation(
        row(0.92f, 1),
        row(0.80f, 4),
        row(0.62f, 2, spread = 0.36f),
        row(0.30f, 4, spread = 0.76f)
    ),

    // 2-3-5
    "2-3-5" to formation(
        row(0.92f, 1),
        row(0.82f, 2, spread = 0.60f),
        row(0.62f, 3),
        row(0.30f, 5)
    ),

    // 3-5-2
    "3-5-2" to formation(
        row(0.92f, 1),
        row(0.82f, 3, spread = 0.60f),
        row(0.66f, 2, spread = 0.40f),
        row(0.52f, 3, spread = 0.62f),
        row(0.28f, 2, spread = 0.40f)
    ),

    // 4-5-1
    "4-5-1" to formation(
        row(0.92f, 1),
        row(0.80f, 4),
        row(0.64f, 2, spread = 0.36f),
        row(0.50f, 3),
        row(0.26f, 1)
    ),

    // 4-1-4-1
    "4-1-4-1" to formation(
        row(0.92f, 1),
        row(0.80f, 4),
        row(0.64f, 1),
        row(0.50f, 4),
        row(0.26f, 1)
    ),
    "5-4-1" to formation(
        row(0.92f, 1),
        row(0.82f, 5),
        row(0.56f, 4),
        row(0.26f, 1)
    )
)
