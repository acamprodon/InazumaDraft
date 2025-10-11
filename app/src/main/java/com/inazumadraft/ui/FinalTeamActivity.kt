package com.inazumadraft.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.inazumadraft.R
import com.inazumadraft.data.formations
import com.inazumadraft.model.Player

class FinalTeamActivity : AppCompatActivity() {

    private lateinit var fieldLayout: RelativeLayout
    private lateinit var btnToggleView: Button
    private lateinit var recyclerFinalTeam: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_final_team)

        fieldLayout = findViewById(R.id.fieldLayout)
        btnToggleView = findViewById(R.id.btnToggleView)
        recyclerFinalTeam = findViewById(R.id.recyclerFinalTeam)

        val team = intent.getParcelableArrayListExtra<Player>("finalTeam") ?: arrayListOf()
        val formationName = intent.getStringExtra("formation") ?: "4-4-2"
        val captainName = intent.getStringExtra("captainName")

        // Pintar el campo cuando ya tenemos medidas
        fieldLayout.post { drawTemplateAndFill(team, formationName, captainName) }

        // Stats list
        recyclerFinalTeam.layoutManager = LinearLayoutManager(this)
        recyclerFinalTeam.adapter = FinalTeamAdapter(team)

        // Toggle campo <-> stats
        btnToggleView.setOnClickListener {
            if (recyclerFinalTeam.visibility == View.GONE) {
                // Mostrar stats
                recyclerFinalTeam.visibility = View.VISIBLE
                fieldLayout.visibility = View.GONE
                btnToggleView.text = "Ver campo"
            } else {
                // Mostrar campo
                recyclerFinalTeam.visibility = View.GONE
                fieldLayout.visibility = View.VISIBLE
                btnToggleView.text = "Ver estadísticas"
            }
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
        "PT" -> "Portero"; "DF" -> "Defensa"; "MC" -> "Centrocampista"; "DL" -> "Delantero"; else -> code
    }

    /**
     * Template uniforme: filas de **arriba a abajo** = DL → MC → DF → PT (portero abajo).
     * Slots centrados y equiespaciados. Tamaño de carta único para todo el equipo.
     * Si falta un jugador, se muestra el nombre de la posición en el slot.
     */
    private fun drawTemplateAndFill(playersIn: List<Player>, formationName: String, captainName: String?) {
        fieldLayout.removeAllViews()

        // 1) Leer formación y contar piezas por rol
        val formation = formations.firstOrNull { it.name == formationName } ?: return
        val codes = formation.positions.map { toCode(it) }
        val nPT = codes.count { it == "PT" }
        val nDF = codes.count { it == "DF" }
        val nMC = codes.count { it == "MC" }
        val nDL = codes.count { it == "DL" }

        // 2) Reparto por filas (puedes ajustar máximos por fila)
        fun split(count: Int, maxPerRow: Int): List<Int> {
            if (count <= 0) return emptyList()
            val rows = mutableListOf<Int>()
            var left = count
            while (left > 0) {
                val take = left.coerceAtMost(maxPerRow)
                rows.add(take)
                left -= take
            }
            return rows
        }
        val dlRows = split(nDL, 3) // arriba
        val mcRows = split(nMC, 4)
        val dfRows = split(nDF, 4)
        val ptRows = if (nPT > 0) listOf(nPT) else emptyList() // abajo

        // Orden de filas **de arriba a abajo** (¡portero último!)
        val rowSpec = buildList {
            addAll(dlRows)
            addAll(mcRows)
            addAll(dfRows)
            addAll(ptRows)
        }
        if (rowSpec.isEmpty()) return

        // Para saber qué posición corresponde a cada fila
        val dlStart = 0
        val mcStart = dlStart + dlRows.size
        val dfStart = mcStart + mcRows.size
        val ptStart = dfStart + dfRows.size
        fun roleForRow(rowIdx: Int): String = when {
            rowIdx < mcStart -> "DL"
            rowIdx < dfStart -> "MC"
            rowIdx < ptStart -> "DF"
            else -> "PT"
        }

        // 3) Ordenar los jugadores reales en ese mismo orden
        val pool = playersIn.toMutableList()
        fun takeOne(role: String): Player? {
            val idx = pool.indexOfFirst { it.position.equals(role, ignoreCase = true) }
            return if (idx >= 0) pool.removeAt(idx) else null
        }
        val orderedForTemplate = mutableListOf<Player?>().apply {
            repeat(nDL) { add(takeOne("DL")) }
            repeat(nMC) { add(takeOne("MC")) }
            repeat(nDF) { add(takeOne("DF")) }
            repeat(nPT) { add(takeOne("PT")) }
        }

        // 4) Parámetros visuales
        val d = resources.displayMetrics.density
        var cardW = 100f * d
        var cardH = cardW * 1.25f
        val hGap  = 8f * d
        val topPad = fieldLayout.height * 0.10f
        val bottomPad = fieldLayout.height * 0.90f

        // Ajusta anchura si la fila más ancha no cabe
        val maxCols = rowSpec.maxOrNull() ?: 1
        val rowWidthNeeded = maxCols * cardW + (maxCols - 1) * hGap
        if (rowWidthNeeded > fieldLayout.width) {
            cardW = (fieldLayout.width - (maxCols - 1) * hGap) / maxCols
            cardH = cardW * 1.25f
        }

        // 5) Y de cada fila equiespaciadas (arriba→abajo)
        val rowsCount = rowSpec.size
        val yCenters = (0 until rowsCount).map { r ->
            val t = if (rowsCount == 1) 0.5f else r / (rowsCount - 1f)
            topPad + t * (bottomPad - topPad)
        }

        // 6) Pintar slots
        var globalIndex = 0
        rowSpec.forEachIndexed { rowIdx, cols ->
            val n = cols.coerceAtLeast(0)
            if (n == 0) return@forEachIndexed

            val rowWidth = n * cardW + (n - 1) * hGap
            val startX = (fieldLayout.width - rowWidth) / 2f
            val yCenter = yCenters[rowIdx]
            val roleForThisRow = roleForRow(rowIdx)

            repeat(n) { i ->
                val p = orderedForTemplate.getOrNull(globalIndex)
                globalIndex++

                val view = layoutInflater.inflate(R.layout.item_player_field, fieldLayout, false)
                val img  = view.findViewById<ImageView>(R.id.imgPlayer)
                val name = view.findViewById<TextView>(R.id.txtPlayerName)
                val elem = view.findViewById<ImageView>(R.id.imgElement)

                if (p != null) {
                    img.setImageResource(p.image)
                    name.text = p.name
                    elem.setImageResource(p.element)
                    if (p.name == captainName) img.setBackgroundResource(R.drawable.captain_border)
                } else {
                    // Slot vacío: badge textual de la posición
                    name.text = codeToNice(roleForThisRow)
                    elem.setImageResource(0)
                    img.setImageResource(0)
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
