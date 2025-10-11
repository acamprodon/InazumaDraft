package com.inazumadraft.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.inazumadraft.R
import com.inazumadraft.data.formations
import com.inazumadraft.data.formationCoordinates
import com.inazumadraft.model.Player

class FinalTeamActivity : AppCompatActivity() {

    private lateinit var fieldLayout: RelativeLayout
    private lateinit var btnToggleView: Button
    private lateinit var btnNewTeam: FloatingActionButton
    private lateinit var recyclerFinalTeam: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_final_team)

        // Bind views
        fieldLayout = findViewById(R.id.fieldLayout)
        btnToggleView = findViewById(R.id.btnToggleView)
        recyclerFinalTeam = findViewById(R.id.recyclerFinalTeam)
        btnNewTeam = findViewById(R.id.btnNewTeam)

        // Datos recibidos
        val team = intent.getParcelableArrayListExtra<Player>("finalTeam") ?: arrayListOf()
        val formationName = intent.getStringExtra("formation") ?: "4-4-2"
        val captainName = intent.getStringExtra("captainName")

        // Dibuja el campo cuando ya haya medidas
        fieldLayout.post { drawTemplateAndFill(team, formationName, captainName) }

        // Configura lista de estadísticas
        recyclerFinalTeam.layoutManager = LinearLayoutManager(this)
        recyclerFinalTeam.adapter = FinalTeamAdapter(team)

        // Botón alternar vista
        btnToggleView.setOnClickListener {
            if (recyclerFinalTeam.visibility == View.GONE) {
                recyclerFinalTeam.visibility = View.VISIBLE
                fieldLayout.visibility = View.GONE
                btnToggleView.text = "VER CAMPO"
            } else {
                recyclerFinalTeam.visibility = View.GONE
                fieldLayout.visibility = View.VISIBLE
                btnToggleView.text = "VER ESTADÍSTICAS"
            }
        }

        // Botón crear nuevo equipo
        btnNewTeam.setOnClickListener {
            val intent = Intent(this@FinalTeamActivity, DraftActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
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
     * Dibuja el campo con los jugadores en su formación.
     */
    private fun drawTemplateAndFill(playersIn: List<Player>, formationName: String, captainName: String?) {
        fieldLayout.removeAllViews()

        val formation = formations.firstOrNull { it.name == formationName } ?: return

        // --- Medidas de la carta ---
        val d = resources.displayMetrics.density
        var cardW = 100f * d
        var cardH = cardW * 1.25f
        val hGap = 8f * d

        val maxCols = 4
        val rowWidthNeeded = maxCols * cardW + (maxCols - 1) * hGap
        if (rowWidthNeeded > fieldLayout.width) {
            cardW = (fieldLayout.width - (maxCols - 1) * hGap) / maxCols
            cardH = cardW * 1.25f
        }

        // --- Intento 1: usar coordenadas reales si existen ---
        val coords = formationCoordinates[formationName]
        if (coords != null && coords.size == formation.positions.size) {

            // Banda visible del campo (compacta verticalmente)
            val topBand = 0.08f
            val bottomBand = 0.96f
            fun mapY(y: Float): Float = (topBand + y * (bottomBand - topBand)).coerceIn(0f, 1f)

            // Ordenar jugadores según posiciones de la formación
            val pool = playersIn.toMutableList()
            fun take(role: String): Player? {
                val i = pool.indexOfFirst { it.position.equals(role, true) }
                return if (i >= 0) pool.removeAt(i) else null
            }

            val playersByFormationOrder: List<Player?> = formation.positions.map { pos ->
                when (toCode(pos)) {
                    "PT" -> take("PT")
                    "DF" -> take("DF")
                    "MC" -> take("MC")
                    "DL" -> take("DL")
                    else -> null
                }
            }

            // Dibuja cada posición con coordenadas proporcionales
            coords.forEachIndexed { i, (x, yRaw) ->
                val y = mapY(yRaw)
                val view = layoutInflater.inflate(R.layout.item_player_field, fieldLayout, false)
                val img = view.findViewById<ImageView>(R.id.imgPlayer)
                val name = view.findViewById<TextView>(R.id.txtPlayerName)
                val elem = view.findViewById<ImageView>(R.id.imgElement)

                val p = playersByFormationOrder[i]
                if (p != null) {
                    img.setImageResource(p.image)
                    name.text = p.name
                    elem.setImageResource(p.element)
                    if (p.name == captainName) img.setBackgroundResource(R.drawable.captain_border)
                } else {
                    name.text = codeToNice(toCode(formation.positions[i]))
                    elem.setImageResource(0)
                    img.setImageResource(0)
                }

                val lp = RelativeLayout.LayoutParams(cardW.toInt(), cardH.toInt())
                lp.leftMargin = (fieldLayout.width * x - cardW / 2f).toInt()
                lp.topMargin = (fieldLayout.height * y - cardH / 2f).toInt()
                view.layoutParams = lp
                fieldLayout.addView(view)
            }

            return // Ya dibujamos con coordenadas reales
        }

        // --- Fallback: método de filas (por si no hay coordenadas) ---
        val codes = formation.positions.map { toCode(it) }

        val nPT = codes.count { it == "PT" }
        val nDF = codes.count { it == "DF" }
        val nMC = codes.count { it == "MC" }
        val nDL = codes.count { it == "DL" }

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

        val dlRows = split(nDL, 3)
        val mcRows = split(nMC, 4)
        val dfRows = split(nDF, 4)
        val ptRows = if (nPT > 0) listOf(nPT) else emptyList()
        val rowSpec = buildList {
            addAll(dlRows)
            addAll(mcRows)
            addAll(dfRows)
            addAll(ptRows)
        }
        if (rowSpec.isEmpty()) return

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

        val topPad = fieldLayout.height * 0.10f
        val bottomPad = fieldLayout.height * 0.95f
        val rowsCount = rowSpec.size
        val spacing = (bottomPad - topPad) / (rowsCount.coerceAtLeast(1))
        val yCenters = (0 until rowsCount).map { r -> topPad + r * spacing }

        var globalIndex = 0
        rowSpec.forEachIndexed { rowIdx, cols ->
            val n = cols
            val rowWidth = n * cardW + (n - 1) * hGap
            val startX = (fieldLayout.width - rowWidth) / 2f
            val yCenter = yCenters[rowIdx]
            val roleForThisRow = roleForRow(rowIdx)

            repeat(n) { i ->
                val p = orderedForTemplate.getOrNull(globalIndex)
                globalIndex++

                val view = layoutInflater.inflate(R.layout.item_player_field, fieldLayout, false)
                val img = view.findViewById<ImageView>(R.id.imgPlayer)
                val name = view.findViewById<TextView>(R.id.txtPlayerName)
                val elem = view.findViewById<ImageView>(R.id.imgElement)

                if (p != null) {
                    img.setImageResource(p.image)
                    name.text = p.name
                    elem.setImageResource(p.element)
                    if (p.name == captainName) img.setBackgroundResource(R.drawable.captain_border)
                } else {
                    name.text = codeToNice(roleForThisRow)
                    elem.setImageResource(0)
                    img.setImageResource(0)
                }

                val lp = RelativeLayout.LayoutParams(cardW.toInt(), cardH.toInt())
                lp.leftMargin = (startX + i * (cardW + hGap)).toInt()
                lp.topMargin = (yCenter - cardH / 2f).toInt()
                view.layoutParams = lp
                fieldLayout.addView(view)
            }
        }
    }
}
