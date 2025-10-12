package com.inazumadraft.ui

import android.content.ClipData
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.DragEvent
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.inazumadraft.R
import com.inazumadraft.data.PlayerRepository
import com.inazumadraft.data.formations
import com.inazumadraft.data.formationCoordinates
import com.inazumadraft.model.Player
import com.inazumadraft.model.canPlay

class FinalTeamActivity : AppCompatActivity() {

    private lateinit var fieldLayout: RelativeLayout
    private lateinit var btnToggleView: Button
    private lateinit var btnNewTeam: FloatingActionButton
    private lateinit var recyclerFinalTeam: RecyclerView

    // Banquillo (máx 5, sin restricción por posición)
    private val benchPlayers = mutableListOf<Player>()
    private lateinit var rvBench: RecyclerView
    private lateinit var benchPanel: View
    private lateinit var btnCloseBench: Button
    private lateinit var txtBenchTitle: TextView

    // Titulares en slots de la formación (posición fija + posible null si vacío)
    private val playerSlots = mutableListOf<Player?>()

    private var formationName: String = "4-4-2"
    private var captainName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_final_team)

        fieldLayout = findViewById(R.id.fieldLayout)
        btnToggleView = findViewById(R.id.btnToggleView)
        recyclerFinalTeam = findViewById(R.id.recyclerFinalTeam)
        btnNewTeam = findViewById(R.id.btnNewTeam)

        // --- Panel banquillo (layout_bench_panel.xml incluido en activity_final_team.xml) ---
        benchPanel = findViewById(R.id.benchPanel)
        rvBench = findViewById(R.id.rvBench)
        btnCloseBench = findViewById(R.id.btnCloseBench)
        txtBenchTitle = findViewById(R.id.txtBenchTitle)

        // Datos recibidos
        val team = intent.getParcelableArrayListExtra<Player>("finalTeam") ?: arrayListOf()
        formationName = intent.getStringExtra("formation") ?: "4-4-2"
        captainName = intent.getStringExtra("captainName")

        playerSlots.clear()
        playerSlots.addAll(team)

        // Banquillo inicial: 5 aleatorios que no estén ya en el once
        benchPlayers.clear()
        benchPlayers.addAll(
            PlayerRepository.players
                .filter { p -> p !in team }
                .shuffled()
                .take(5)
        )

        // Dibujar campo
        fieldLayout.post { drawTemplateAndFill() }

        // Lista de estadísticas
        recyclerFinalTeam.layoutManager = LinearLayoutManager(this)
        recyclerFinalTeam.adapter = FinalTeamAdapter(team)

        // Toggle campo / stats
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

        // Nuevo equipo
        btnNewTeam.setOnClickListener {
            val intent = Intent(this@FinalTeamActivity, DraftActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

        setupBenchPanel()
    }

    // ====================== BANQUILLO ======================

    private fun setupBenchPanel() {
        txtBenchTitle.text = "Banquillo (${benchPlayers.size}/5)"
        rvBench.layoutManager = LinearLayoutManager(this)
        rvBench.adapter = FinalTeamAdapter(benchPlayers)

        // Botón flotante para abrir el panel
        val btnShowBench = FloatingActionButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_sort_by_size)
            contentDescription = "Abrir banquillo"
            setOnClickListener {
                benchPanel.animate().translationX(0f).setDuration(200).start()
            }
        }
        (findViewById<ViewGroup>(R.id.fieldLayout)).addView(btnShowBench)

        // Cerrar panel
        btnCloseBench.setOnClickListener {
            closeBench()
        }

        // Acepta soltar jugadores del CAMPO → BANQUILLO
        rvBench.setOnDragListener { _, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> true
                DragEvent.ACTION_DROP -> {
                    // Si el origen NO es "benchPlayer", viene del campo
                    val fromFieldIndex = event.localState as? Int
                    val fromBench = event.clipDescription?.label == "benchPlayer"
                    if (fromFieldIndex != null && !fromBench) {
                        val p = playerSlots.getOrNull(fromFieldIndex) ?: return@setOnDragListener true

                        val child = rvBench.findChildViewUnder(event.x, event.y)
                        val hoveredPos = if (child != null) rvBench.getChildAdapterPosition(child) else RecyclerView.NO_POSITION

                        when {
                            benchPlayers.size < 5 -> {
                                // Añadir si hay hueco
                                benchPlayers.add(p)
                                playerSlots[fromFieldIndex] = null
                            }
                            hoveredPos != RecyclerView.NO_POSITION -> {
                                // Swap con el elemento sobre el que se suelta
                                val tmp = benchPlayers[hoveredPos]
                                benchPlayers[hoveredPos] = p
                                playerSlots[fromFieldIndex] = tmp
                            }
                            else -> {
                                // Lleno y sin hover → feedback
                                shakeView(rvBench)
                                return@setOnDragListener true
                            }
                        }
                        rvBench.adapter?.notifyDataSetChanged()
                        txtBenchTitle.text = "Banquillo (${benchPlayers.size}/5)"
                        drawTemplateAndFill()
                    }
                    true
                }
                DragEvent.ACTION_DRAG_ENDED -> true
                else -> false
            }
        }

        // Permite iniciar drag desde BANQUILLO → CAMPO
        val gestureDetector =
            GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
                override fun onLongPress(e: MotionEvent) {
                    val child = rvBench.findChildViewUnder(e.x, e.y) ?: return
                    val pos = rvBench.getChildAdapterPosition(child)
                    if (pos == RecyclerView.NO_POSITION) return

                    // Iniciar drag desde BANQUILLO → CAMPO
                    val clip = ClipData.newPlainText("benchPlayer", "benchPlayer")
                    val shadow = View.DragShadowBuilder(child)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                        child.startDragAndDrop(clip, shadow, pos, 0)
                    else @Suppress("DEPRECATION")
                    child.startDrag(clip, shadow, pos, 0)
                }
            })

        rvBench.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                gestureDetector.onTouchEvent(e)
                return false
            }
            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
                // no-op
            }
            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
                // no-op
            }
        })
    }

    private fun closeBench() {
        benchPanel.animate().translationX(benchPanel.width.toFloat()).setDuration(200).start()
    }

    private fun shakeView(v: View) {
        v.animate().translationX(12f).setDuration(40).withEndAction {
            v.animate().translationX(-10f).setDuration(40).withEndAction {
                v.animate().translationX(0f).setDuration(40).start()
            }.start()
        }.start()
    }

    // ====================== UTILIDADES ======================

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

    // ====================== CAMPO + DRAG & DROP ======================

    /**
     * Dibuja el campo con los slots de la formación y gestiona drag & drop:
     * - Campo → Campo (validado por roles)
     * - Banquillo → Campo (validado por roles)
     */
    private fun drawTemplateAndFill() {
        fieldLayout.removeAllViews()

        val formation = formations.firstOrNull { it.name == formationName } ?: return

        // Medidas carta
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

        val coords = formationCoordinates[formationName]
        if (coords != null && coords.size == formation.positions.size) {
            val topBand = 0.08f
            val bottomBand = 0.96f
            fun mapY(y: Float): Float = (topBand + y * (bottomBand - topBand)).coerceIn(0f, 1f)

            coords.forEachIndexed { i, (x, yRaw) ->
                val y = mapY(yRaw)
                val view = layoutInflater.inflate(R.layout.item_player_field, fieldLayout, false)
                bindSlotView(view, i)

                val lp = RelativeLayout.LayoutParams(cardW.toInt(), cardH.toInt())
                lp.leftMargin = (fieldLayout.width * x - cardW / 2f).toInt()
                lp.topMargin = (fieldLayout.height * y - cardH / 2f).toInt()
                view.layoutParams = lp

                attachDragLogicToFieldView(view, i)
                fieldLayout.addView(view)
            }
            return
        }

        // Fallback por filas (si faltan coords)
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
        val rowSpec = buildList { addAll(dlRows); addAll(mcRows); addAll(dfRows); addAll(ptRows) }
        if (rowSpec.isEmpty()) return

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
                val index = globalIndex
                val view = layoutInflater.inflate(R.layout.item_player_field, fieldLayout, false)
                bindSlotView(view, index)

                val lp = RelativeLayout.LayoutParams(cardW.toInt(), cardH.toInt())
                lp.leftMargin = (startX + i * (cardW + hGap)).toInt()
                lp.topMargin = (yCenter - cardH / 2f).toInt()
                view.layoutParams = lp

                attachDragLogicToFieldView(view, index)
                fieldLayout.addView(view)
                globalIndex++
            }
        }
    }

    private fun bindSlotView(view: View, i: Int) {
        val img = view.findViewById<ImageView>(R.id.imgPlayer)
        val name = view.findViewById<TextView>(R.id.txtPlayerNickname)
        val elem = view.findViewById<ImageView>(R.id.imgElement)

        val formation = formations.firstOrNull { it.name == formationName } ?: return
        val p = playerSlots.getOrNull(i)
        if (p != null) {
            img.setImageResource(p.image)
            name.text = p.nickname
            elem.setImageResource(p.element)
            if (p.name == captainName) img.setBackgroundResource(R.drawable.captain_border)
        } else {
            name.text = codeToNice(toCode(formation.positions[i]))
            elem.setImageResource(0)
            img.setImageResource(0)
        }
    }

    private fun attachDragLogicToFieldView(view: View, index: Int) {
        val formation = formations.first { it.name == formationName }
        val dstRoleCode = toCode(formation.positions[index])

        // Iniciar drag desde CAMPO (si hay jugador)
        val p = playerSlots.getOrNull(index)
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

        // Recibir drop (campo ← campo / campo ← banquillo)
        view.setOnDragListener { v, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> true
                DragEvent.ACTION_DRAG_ENTERED -> { v.alpha = 0.7f; true }
                DragEvent.ACTION_DRAG_EXITED -> { v.alpha = 1f; true }
                DragEvent.ACTION_DROP -> {
                    val fromBench = event.clipDescription?.label == "benchPlayer"
                    if (fromBench) {
                        // BANQUILLO → CAMPO
                        val benchIdx = event.localState as Int
                        val benchPlayer = benchPlayers.getOrNull(benchIdx) ?: return@setOnDragListener true
                        if (!benchPlayer.canPlay(dstRoleCode)) {
                            shakeView(v); return@setOnDragListener true
                        }
                        val replaced = playerSlots[index]
                        playerSlots[index] = benchPlayer
                        if (replaced == null) {
                            benchPlayers.removeAt(benchIdx)
                        } else {
                            benchPlayers[benchIdx] = replaced
                        }
                        rvBench.adapter?.notifyDataSetChanged()
                        txtBenchTitle.text = "Banquillo (${benchPlayers.size}/5)"
                        drawTemplateAndFill()
                    } else {
                        // CAMPO → CAMPO (swap con validación)
                        val fromIdx = event.localState as? Int
                        if (fromIdx == null || fromIdx == index) return@setOnDragListener true
                        val src = playerSlots[fromIdx]
                        val dst = playerSlots[index]

                        val srcRoleCode = toCode(formation.positions[index])   // destino del arrastre
                        val dstRoleCode2 = toCode(formation.positions[fromIdx]) // destino del que estaba aquí

                        val okSrc = src?.canPlay(srcRoleCode) ?: true
                        val okDst = dst?.canPlay(dstRoleCode2) ?: true

                        if (okSrc && okDst) {
                            val tmp = playerSlots[fromIdx]
                            playerSlots[fromIdx] = playerSlots[index]
                            playerSlots[index] = tmp
                            drawTemplateAndFill()
                        } else {
                            shakeView(v)
                        }
                    }
                    true
                }
                DragEvent.ACTION_DRAG_ENDED -> { v.alpha = 1f; true }
                else -> false
            }
        }
    }
}
