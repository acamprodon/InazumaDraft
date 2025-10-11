package com.inazumadraft.ui

import android.os.Bundle
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.inazumadraft.R
import com.inazumadraft.data.formations
import com.inazumadraft.model.Player

class FinalTeamActivity : AppCompatActivity() {

    private lateinit var fieldLayout: RelativeLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_final_team)

        fieldLayout = findViewById(R.id.fieldLayout)

        val team = intent.getParcelableArrayListExtra<Player>("finalTeam") ?: arrayListOf()
        val formationName = intent.getStringExtra("formation") ?: "4-4-2"
        val captainName = intent.getStringExtra("captainName")

        fieldLayout.post {
            drawTemplateAndFill(team, formationName, captainName)
        }
    }

    // --- Helpers de posiciones ---
    private fun toCode(pos: String): String = when (pos.trim().lowercase()) {
        "portero", "pt" -> "PT"
        "defensa", "df" -> "DF"
        "centrocampista", "mc" -> "MC"
        "delantero", "dl" -> "DL"
        else -> pos.trim().uppercase()
    }

    private fun codeToNice(code: String): String = when (code.uppercase()) {
        "PT" -> "Portero"
        "DF" -> "Defensa"
        "MC" -> "Centrocampista"
        "DL" -> "Delantero"
        else -> code
    }

    /**
     * TEMPLATE UNIFORME (estilo “opción 2”):
     * - Tamaño de carta único para todo el equipo.
     * - Filas equiespaciadas: PT / DF / MC / DL (si hay muchas piezas, se parte en varias filas).
     * - Slots centrados y equiespaciados por fila (sin solapes).
     * - Si falta un jugador, el slot muestra un badge de posición (PT/DF/MC/DL).
     */
    private fun drawTemplateAndFill(playersIn: List<Player>, formationName: String, captainName: String?) {
        fieldLayout.removeAllViews()

        // 1) Leer la formación elegida y contar piezas por rol
        val formation = formations.firstOrNull { it.name == formationName } ?: return
        val codes = formation.positions.map { toCode(it) }

        val nPT = codes.count { it == "PT" }
        val nDF = codes.count { it == "DF" }
        val nMC = codes.count { it == "MC" }
        val nDL = codes.count { it == "DL" }

        // 2) Reparto por filas para estética uniforme
        fun split(count: Int, maxPerRow: Int): List<Int> {
            if (count <= 0) return emptyList()
            if (count <= maxPerRow) return listOf(count)
            val rows = mutableListOf<Int>()
            var left = count
            while (left > 0) {
                val take = left.coerceAtMost(maxPerRow)
                rows.add(take)
                left -= take
            }
            return rows
        }
        // Ajusta aquí el “máximo por fila” de cada línea si quieres otro look
        val dfRows = split(nDF, 4) // ej. hasta 4 defensas por fila
        val mcRows = split(nMC, 4) // hasta 4 medios por fila
        val dlRows = split(nDL, 3) // hasta 3 delanteros por fila (queda más “punta”)

        // Orden de filas: Portero → Defensas → Medios → Delanteros
        val rowSpec = buildList {
            if (nPT > 0) add(nPT)
            addAll(dfRows)
            addAll(mcRows)
            addAll(dlRows)
        }
        if (rowSpec.isEmpty()) return

        // 3) Ordenar los jugadores reales para meterlos en los slots en ese mismo orden
        val pool = playersIn.toMutableList()
        fun takeOne(role: String): Player? {
            val idx = pool.indexOfFirst { it.position.equals(role, ignoreCase = true) }
            return if (idx >= 0) pool.removeAt(idx) else null
        }
        val orderedForTemplate = mutableListOf<Player?>().apply {
            repeat(nPT) { add(takeOne("PT")) }
            repeat(nDF) { add(takeOne("DF")) }
            repeat(nMC) { add(takeOne("MC")) }
            repeat(nDL) { add(takeOne("DL")) }
        }

        // 4) Parámetros visuales globales
        val d = resources.displayMetrics.density
        var cardW = 100f * d               // ancho uniforme
        var cardH = cardW * 1.25f          // alto uniforme (aspecto 4:5 aprox)
        val hGap  = 8f * d                 // separación horizontal entre cartas
        val vGap  = 22f * d                // separación vertical entre filas
        val topPad = fieldLayout.height * 0.12f
        val bottomPad = fieldLayout.height * 0.88f

        // Asegura que la fila más ancha cabe; si no, ajusta cardW/cardH a la baja
        val maxCols = rowSpec.maxOrNull() ?: 1
        val rowWidthNeeded = maxCols * cardW + (maxCols - 1) * hGap
        if (rowWidthNeeded > fieldLayout.width) {
            cardW = (fieldLayout.width - (maxCols - 1) * hGap) / maxCols
            cardH = cardW * 1.25f
        }

        // 5) Y de cada fila (equiespaciadas)
        val rowsCount = rowSpec.size
        val yCenters = (0 until rowsCount).map { r ->
            val t = if (rowsCount == 1) 0.5f else r / (rowsCount - 1f)
            topPad + t * (bottomPad - topPad)
        }

        // 6) Pintar filas y slots
        var globalIndex = 0
        rowSpec.forEachIndexed { rowIdx, cols ->
            val n = cols.coerceAtLeast(0)
            if (n == 0) return@forEachIndexed

            val rowWidth = n * cardW + (n - 1) * hGap
            val startX = (fieldLayout.width - rowWidth) / 2f
            val yCenter = yCenters[rowIdx]

            repeat(n) { i ->
                val p = orderedForTemplate.getOrNull(globalIndex)
                globalIndex++

                val view = layoutInflater.inflate(R.layout.item_player_field, fieldLayout, false)
                val img  = view.findViewById<ImageView>(R.id.imgPlayer)
                val name = view.findViewById<TextView>(R.id.txtPlayerName)
                val elem = view.findViewById<ImageView>(R.id.imgElement)

                if (p != null) {
                    // Slot con jugador
                    img.setImageResource(p.image)
                    name.text = p.name
                    elem.setImageResource(p.element)
                    if (p.name == captainName) {
                        img.setBackgroundResource(R.drawable.captain_border)
                    }
                } else {
                    // Slot vacío: badge de posición
                    val neededRole = when {
                        rowIdx == 0 && nPT > 0 -> "PT"
                        // cuántas filas ocupa cada bloque
                        rowIdx in 1..dfRows.size && nDF > 0 -> "DF"
                        rowIdx in (1 + dfRows.size)..(dfRows.size + mcRows.size) && nMC > 0 -> "MC"
                        else -> "DL"
                    }
                    name.text = codeToNice(neededRole)  // muestra “Portero/Defensa/…”
                    elem.setImageResource(0)
                    img.setImageResource(0)
                    // Fondo sutil para distinguir slot vacío (opcional):
                    // view.setBackgroundResource(R.drawable.slot_placeholder)
                }

                val lp = RelativeLayout.LayoutParams(cardW.toInt(), cardH.toInt())
                lp.leftMargin = (startX + i * (cardW + hGap)).toInt()
                lp.topMargin  = (yCenter - cardH / 2f).toInt()
                view.layoutParams = lp
                fieldLayout.addView(view)
            }
        }
    }
}
