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
import com.inazumadraft.R
import com.inazumadraft.data.formations
import com.inazumadraft.model.Player

class FinalTeamActivity : AppCompatActivity() {
    private lateinit var btnNewTeam: com.google.android.material.floatingactionbutton.FloatingActionButton

    private lateinit var fieldLayout: RelativeLayout
    private lateinit var btnToggleView: Button
    private lateinit var recyclerFinalTeam: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_final_team)

        fieldLayout = findViewById(R.id.fieldLayout)
        btnToggleView = findViewById(R.id.btnToggleView)
        recyclerFinalTeam = findViewById(R.id.recyclerFinalTeam)
        btnNewTeam = findViewById(R.id.btnNewTeam)
        btnNewTeam.setOnClickListener {
            val intent = Intent(this@FinalTeamActivity, DraftActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

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
                recyclerFinalTeam.visibility = View.VISIBLE
                fieldLayout.visibility = View.GONE
                btnToggleView.text = "Ver campo"
            } else {
                recyclerFinalTeam.visibility = View.GONE
                fieldLayout.visibility = View.VISIBLE
                btnToggleView.text = "Ver estadísticas"
            }
        }

        // Botón pequeño para crear nuevo equipo
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
     * Dibuja la plantilla uniforme del campo con jugadores o huecos vacíos.
     */
    private fun drawTemplateAndFill(playersIn: List<Player>, formationName: String, captainName: String?) {
        fieldLayout.removeAllViews()

        // 1) Leer formación
        val formation = formations.firstOrNull { it.name == formationName } ?: return
        val codes = formation.positions.map { toCode(it) }

        // 2) Calcular filas por tipo
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

        // Orden de roles por fila
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

        // 3) Ordenar jugadores
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

        // 4) Medidas visuales
        val d = resources.displayMetrics.density
        var cardW = 100f * d
        var cardH = cardW * 1.25f
        val hGap = 8f * d
        val topPad = fieldLayout.height * 0.10f
        val bottomPad = fieldLayout.height * 0.90f

        val maxCols = rowSpec.maxOrNull() ?: 1
        val rowWidthNeeded = maxCols * cardW + (maxCols - 1) * hGap
        if (rowWidthNeeded > fieldLayout.width) {
            cardW = (fieldLayout.width - (maxCols - 1) * hGap) / maxCols
            cardH = cardW * 1.25f
        }

        val rowsCount = rowSpec.size
        val yCenters = (0 until rowsCount).map { r ->
            val t = if (rowsCount == 1) 0.5f else r / (rowsCount - 1f)
            topPad + t * (bottomPad - topPad)
        }

        // 5) Dibujar slots
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
