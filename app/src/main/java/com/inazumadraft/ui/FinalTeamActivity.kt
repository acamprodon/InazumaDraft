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

// Drag & drop imports
import android.content.ClipData
import android.os.Build
import android.view.DragEvent

class FinalTeamActivity : AppCompatActivity() {

    private lateinit var fieldLayout: RelativeLayout
    private lateinit var btnToggleView: Button
    private lateinit var btnNewTeam: FloatingActionButton
    private lateinit var recyclerFinalTeam: RecyclerView

    // Trabajamos con una lista mutable para poder intercambiar al vuelo
    private val playerSlots = mutableListOf<Player?>()

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

        // Inicializamos slots
        playerSlots.clear()
        playerSlots.addAll(team)

        // Dibuja el campo cuando ya haya medidas
        fieldLayout.post { drawTemplateAndFill(formationName, captainName) }

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
     * Dibuja el campo con los jugadores en su formación + drag & drop entre slots.
     */
    private fun drawTemplateAndFill(formationName: String, captainName: String?) {
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

        // Intento con coordenadas reales si existen
        val coords = formationCoordinates[formationName]
        if (coords != null && coords.size == formation.positions.size) {

            // Banda visible del campo (compacta verticalmente)
            val topBand = 0.08f
            val bottomBand = 0.96f
            fun mapY(y: Float): Float = (topBand + y * (bottomBand - topBand)).coerceIn(0f, 1f)

            coords.forEachIndexed { index, (x, yRaw) ->
                val y = mapY(yRaw)
                val view = layoutInflater.inflate(R.layout.item_player_field, fieldLayout, false)
                val img = view.findViewById<ImageView>(R.id.imgPlayer)
                // ⚠️ Usa SIEMPRE txtPlayerNickname (tu layout no tiene txtPlayerName)
                val name = view.findViewById<TextView>(R.id.txtPlayerNickname)
                val elem = view.findViewById<ImageView>(R.id.imgElement)

                val p = playerSlots.getOrNull(index)
                if (p != null) {
                    img.setImageResource(p.image)
                    name.text = p.nickname
                    elem.setImageResource(p.element)
                    if (p.name == captainName) img.setBackgroundResource(R.drawable.captain_border)
                } else {
                    name.text = codeToNice(toCode(formation.positions[index]))
                    elem.setImageResource(0)
                    img.setImageResource(0)
                }

                val lp = RelativeLayout.LayoutParams(cardW.toInt(), cardH.toInt())
                lp.leftMargin = (fieldLayout.width * x - cardW / 2f).toInt()
                lp.topMargin = (fieldLayout.height * y - cardH / 2f).toInt()
                view.layoutParams = lp

                // --- DRAG & DROP CONFIG ---
                if (p != null) {
                    view.setOnLongClickListener {
                        val clipData = ClipData.newPlainText("fromIndex", index.toString())
                        val shadow = View.DragShadowBuilder(it)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            it.startDragAndDrop(clipData, shadow, index, 0)
                        } else {
                            @Suppress("DEPRECATION")
                            it.startDrag(clipData, shadow, index, 0)
                        }
                        it.alpha = 0.5f
                        true
                    }
                }

                view.setOnDragListener { v, event ->
                    when (event.action) {
                        DragEvent.ACTION_DRAG_STARTED -> true
                        DragEvent.ACTION_DRAG_ENTERED -> { v.alpha = 0.7f; true }
                        DragEvent.ACTION_DRAG_EXITED -> { v.alpha = 1f; true }
                        DragEvent.ACTION_DROP -> {
                            val from = event.localState as? Int
                            if (from != null && from != index) {
                                val tmp = playerSlots[from]
                                playerSlots[from] = playerSlots[index]
                                playerSlots[index] = tmp
                                drawTemplateAndFill(formationName, captainName)
                            }
                            true
                        }
                        DragEvent.ACTION_DRAG_ENDED -> { v.alpha = 1f; true }
                        else -> false
                    }
                }

                fieldLayout.addView(view)
            }

            return // Ya dibujado con coordenadas
        }

        // --- Fallback por filas (por si no hay coordenadas) ---
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

        // Ordenar players en el mismo orden que los slots
        val pool = playerSlots.toMutableList()
        fun takeOne(role: String): Player? {
            val idx = pool.indexOfFirst { it?.position.equals(role, ignoreCase = true) }
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

            repeat(n) { i ->
                val p = orderedForTemplate.getOrNull(globalIndex)
                val index = globalIndex
                globalIndex++

                val view = layoutInflater.inflate(R.layout.item_player_field, fieldLayout, false)
                val img = view.findViewById<ImageView>(R.id.imgPlayer)
                // ⚠️ Usa SIEMPRE txtPlayerNickname
                val name = view.findViewById<TextView>(R.id.txtPlayerNickname)
                val elem = view.findViewById<ImageView>(R.id.imgElement)

                if (p != null) {
                    img.setImageResource(p.image)
                    name.text = p.nickname
                    elem.setImageResource(p.element)
                    if (p.name == captainName) img.setBackgroundResource(R.drawable.captain_border)
                } else {
                    name.text = codeToNice(roleForRow(rowIdx))
                    elem.setImageResource(0)
                    img.setImageResource(0)
                }

                val lp = RelativeLayout.LayoutParams(cardW.toInt(), cardH.toInt())
                lp.leftMargin = (startX + i * (cardW + hGap)).toInt()
                lp.topMargin = (yCenter - cardH / 2f).toInt()
                view.layoutParams = lp

                // ----- DRAG & DROP -----
                if (p != null) {
                    view.setOnLongClickListener {
                        val clipData = ClipData.newPlainText("fromIndex", index.toString())
                        val shadow = View.DragShadowBuilder(it)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            it.startDragAndDrop(clipData, shadow, index, 0)
                        } else {
                            @Suppress("DEPRECATION")
                            it.startDrag(clipData, shadow, index, 0)
                        }
                        it.alpha = 0.5f
                        true
                    }
                }

                view.setOnDragListener { v, event ->
                    when (event.action) {
                        DragEvent.ACTION_DRAG_STARTED -> true
                        DragEvent.ACTION_DRAG_ENTERED -> { v.alpha = 0.7f; true }
                        DragEvent.ACTION_DRAG_EXITED -> { v.alpha = 1f; true }
                        DragEvent.ACTION_DROP -> {
                            val from = event.localState as? Int
                            if (from != null && from != index) {
                                val tmp = playerSlots[from]
                                playerSlots[from] = playerSlots[index]
                                playerSlots[index] = tmp
                                drawTemplateAndFill(formationName, captainName)
                            }
                            true
                        }
                        DragEvent.ACTION_DRAG_ENDED -> { v.alpha = 1f; true }
                        else -> false
                    }
                }

                fieldLayout.addView(view)
            }
        }
    }
}
