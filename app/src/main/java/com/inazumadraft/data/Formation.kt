package com.inazumadraft.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Formation(
    val name: String,
    val positions: List<String> // Debe tener 11 elementos (PT + 10)
) : Parcelable

// OJO: usamos "MC" como etiqueta de medios. (Tus Player pueden tener "Mediocampista":
// tu lógica ya trata ambas como equivalentes si usas el helper matchesPosition del DraftActivity.)
val formations = listOf(
    Formation(
        "4-4-2",
        listOf(
            "PT",
            "DF","DF","DF","DF",
            "MC","MC","MC","MC",
            "DL","DL"
        )
    ),
    Formation(
        "4-3-3",
        listOf(
            "PT",
            "DF","DF","DF","DF",
            "MC","MC","MC",
            "DL","DL","DL"
        )
    ),
    Formation(
        "5-2-3",
        listOf(
            "PT",
            "DF","DF","DF","DF","DF",
            "MC","MC",
            "DL","DL","DL"
        )
    ),
    Formation(
        "3-2-3-2",
        listOf(
            "PT",
            "DF","DF","DF",
            "MC","MC",
            "MC","MC","MC",
            "DL","DL"
        )
    ),
    Formation("2-3-5", listOf(
        "PT", "DF", "DF", "MC", "MC", "MC", "DL", "DL", "DL", "DL", "DL")
),
    Formation("3-4-3", listOf("PT", "DF", "DF", "MC", "MC", "MC", "MC", "DL", "DL", "DL")),
            Formation(
            name = "5-3-2",
    positions = listOf(
        "PT",
        "DF","DF","DF","DF","DF", // 5 DF (incluye laterales)
        "MC","MC","MC",// 3 MC
        "DL","DL"                            // 2 DL
)), Formation(
        name = "4-2-3-1",
        positions = listOf(
            "PT",
            "DF","DF","DF","DF",           // 4 DF
            "MC","MC",                 // 2 pivotes
            "MC","MC","MC",// 3 medias puntas/extremos
            "DL"                                        // 1 DL
        )
    )

)


/**
 * Coordenadas proporcionales (x,y) dentro del campo [0..1].
 * y=0.92 PT (abajo), y=0.78 DFs, y=0.62 pivotes, y=0.50/0.45 interiores/medias,
 * y=0.20 DLs. x repartido de izquierda a derecha.
 */
val formationCoordinates = mapOf(
    // 4-4-2
    "4-4-2" to listOf(
        0.50f to 0.92f, // GK
        0.15f to 0.78f, 0.35f to 0.78f, 0.65f to 0.78f, 0.85f to 0.78f, // 4 DEF
        0.15f to 0.50f, 0.35f to 0.50f, 0.65f to 0.50f, 0.85f to 0.50f, // 4 MID
        0.40f to 0.20f, 0.60f to 0.20f  // 2 FWD
    ),
    // 4-3-3
    "4-3-3" to listOf(
        0.50f to 0.92f, // GK
        0.15f to 0.78f, 0.35f to 0.78f, 0.65f to 0.78f, 0.85f to 0.78f, // 4 DEF
        0.25f to 0.55f, 0.50f to 0.55f, 0.75f to 0.55f, // 3 MID
        0.25f to 0.20f, 0.50f to 0.20f, 0.75f to 0.20f // 3 FWD
    ),
    // 5-2-3
    "5-2-3" to listOf(
        0.50f to 0.92f, // GK
        0.10f to 0.80f, 0.30f to 0.80f, 0.50f to 0.80f, 0.70f to 0.80f, 0.90f to 0.80f, // 5 DEF
        0.40f to 0.58f, 0.60f to 0.58f, // 2 MID
        0.25f to 0.20f, 0.50f to 0.20f, 0.75f to 0.20f // 3 FWD
    ),
    // 3-2-3-2  (3 DEF, 2 pivotes, 3 medias/volantes, 2 FWD)
    "3-2-3-2" to listOf(
        0.50f to 0.92f, // GK
        0.30f to 0.80f, 0.50f to 0.80f, 0.70f to 0.80f, // 3 DEF
        0.40f to 0.62f, 0.60f to 0.62f, // 2 MID (pivotes)
        0.25f to 0.48f, 0.50f to 0.48f, 0.75f to 0.48f, // 3 MID (interiores/medias)
        0.40f to 0.18f, 0.60f to 0.18f // 2 FWD
    ),
    "5-3-2" to listOf(
        0.50f to 0.95f, // GK
        0.20f to 0.72f, // LWB
        0.35f to 0.78f, // LCB
        0.50f to 0.78f, // CB
        0.65f to 0.78f, // RCB
        0.80f to 0.72f, // RWB
        0.30f to 0.50f, // CM-L
        0.50f to 0.50f, // CM
        0.70f to 0.50f, // CM-R
        0.45f to 0.18f, // ST-L
        0.55f to 0.18f  // ST-R
    ),

    // 4-2-3-1 (2 pivotes + 3 MP/EXT + 1 punta)
    "4-2-3-1" to listOf(
        0.50f to 0.95f, // GK
        0.15f to 0.78f, // LB
        0.35f to 0.78f, // LCB
        0.65f to 0.78f, // RCB
        0.85f to 0.78f, // RB
        0.40f to 0.62f, // CDM-L
        0.60f to 0.62f, // CDM-R
        0.20f to 0.38f, // LW
        0.50f to 0.38f, // CAM
        0.80f to 0.38f, // RW
        0.50f to 0.16f  // ST
    ),
    "2-3-5" to listOf(
        0.50f to 0.95f, // GK

        0.30f to 0.80f, // DF-L
        0.70f to 0.80f, // DF-R

        0.25f to 0.60f, // MC-L
        0.50f to 0.58f, // MC-C
        0.75f to 0.60f, // MC-R

        0.18f to 0.24f, // DL-L (Extremo izq)
        0.36f to 0.20f, // DL-IL (Interior izq)
        0.50f to 0.16f, // DL-C  (Delantero centro)
        0.64f to 0.20f, // DL-IR (Interior dcha)
        0.82f to 0.24f  // DL-R (Extremo dcha)
    ),
    // 3-4-3: 3 centrales, 4 medios (dos carriles + dos interiores), tridente arriba
    "3-4-3" to listOf(
        0.50f to 0.95f, // GK

        0.28f to 0.80f, // CB-L
        0.50f to 0.80f, // CB-C
        0.72f to 0.80f, // CB-R

        0.20f to 0.56f, // WM-L (carrilero izq)
        0.40f to 0.52f, // CM-L
        0.60f to 0.52f, // CM-R
        0.80f to 0.56f, // WM-R (carrilero dcha)

        0.25f to 0.22f, // DL-L (extremo izq)
        0.50f to 0.16f, // DL-C (punta)
        0.75f to 0.22f  // DL-R (extremo dcha)
    )


)
