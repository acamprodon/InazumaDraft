package com.inazumadraft.ui

import android.content.ClipData
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.DragEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.inazumadraft.R
import com.inazumadraft.data.Formation
import com.inazumadraft.data.PlayerRepository
import com.inazumadraft.data.formations
import com.inazumadraft.model.Player
import com.inazumadraft.model.canPlay

class DraftActivity : AppCompatActivity() {

    private lateinit var overlayPreview: View
    private lateinit var fieldLayout: RelativeLayout
    private lateinit var rvOptions: RecyclerView
    private lateinit var btnNext: Button
    private lateinit var roundTitle: TextView

    private var selectedFormation: Formation? = null
    private var formationLocked = false
    private var isPreviewing = false

    private data class Slot(var role: String, var player: Player? = null)
    private val slots = mutableListOf<Slot>()
    private val selectedPlayers = mutableListOf<Player>()
    private var captain: Player? = null
    private var rowSpec: List<Int> = emptyList()

    // Banquillo
    private val benchPlayers = mutableListOf<Player>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_draft)

        fieldLayout = findViewById(R.id.fieldLayout)
        rvOptions = findViewById(R.id.rvOptions)
        btnNext = findViewById(R.id.btnNext)
        roundTitle = findViewById(R.id.roundTitle)
        overlayPreview = findViewById(R.id.overlayPreview)

        setupFormationSelection()
        setupBenchPanel()
    }

    // ---------- FORMATION ----------
    private fun setupFormationSelection() {
        val formationButtons = listOf(
            findViewById<Button>(R.id.btnFormation1),
            findViewById<Button>(R.id.btnFormation2),
            findViewById<Button>(R.id.btnFormation3),
            findViewById<Button>(R.id.btnFormation4)
        )

        val visibleFormations = formations.shuffled().take(4)
        formationButtons.forEachIndexed { i, btn ->
            btn.text = visibleFormations[i].name
            btn.setOnClickListener {
                if (formationLocked) return@setOnClickListener
                selectedFormation = visibleFormations[i]
                formationLocked = true
                startDraft()
            }
        }
    }

    private fun startDraft() {
        roundTitle.text = "Elige tu capitán"
        val options = PlayerRepository.players.shuffled().take(4)
        rvOptions.visibility = View.VISIBLE
        rvOptions.layoutManager = GridLayoutManager(this, 2)
        rvOptions.adapter = OptionAdapter(options, onClick = { p ->
            captain = p
            buildSlotsTemplateAndPlaceCaptain()
            fieldLayout.visibility = View.VISIBLE
            rvOptions.visibility = View.GONE
            roundTitle.text = "Coloca tu equipo"
            drawSlots()
        })
    }

    private fun buildSlotsTemplateAndPlaceCaptain() {
        val formation = selectedFormation ?: return
        val codes = formation.positions.map { it.uppercase() }

        slots.clear()
        codes.forEach { slots.add(Slot(it)) }

        captain?.let { cap ->
            val idx = slots.indexOfFirst { it.player == null && cap.canPlay(it.role) }
            if (idx >= 0) {
                slots[idx].player = cap
                selectedPlayers.add(cap)
            }
        }

        // 5 suplentes aleatorios
        benchPlayers.clear()
        benchPlayers.addAll(PlayerRepository.players.shuffled()
            .filter { it != captain }
            .take(5))
    }

    // ---------- DRAW FIELD ----------
    private fun drawSlots() {
        if (fieldLayout.width == 0) {
            fieldLayout.post { drawSlots() }
            return
        }

        fieldLayout.removeAllViews()
        val d = resources.displayMetrics.density
        var cardW = 100f * d
        var cardH = cardW * 1.25f
        val hGap = 8f * d
        val topPad = fieldLayout.height * 0.1f
        val bottomPad = fieldLayout.height * 0.9f

        val formation = selectedFormation ?: return
        val total = formation.positions.size
        val rows = when {
            total <= 5 -> listOf(total)
            total <= 8 -> listOf(4, total - 4)
            else -> listOf(3, 4, total - 7)
        }
        val yCenters = (0 until rows.size).map { r ->
            topPad + (r / (rows.size - 1f)) * (bottomPad - topPad)
        }

        var globalIndex = 0
        rows.forEachIndexed { rowIdx, cols ->
            val n = cols
            val rowWidth = n * cardW + (n - 1) * hGap
            val startX = (fieldLayout.width - rowWidth) / 2f
            val yCenter = yCenters[rowIdx]
            repeat(n) { i ->
                if (globalIndex >= slots.size) return@repeat
                val slot = slots[globalIndex]
                val view = layoutInflater.inflate(R.layout.item_player_field, fieldLayout, false)
                val img = view.findViewById<ImageView>(R.id.imgPlayer)
                val txt = view.findViewById<TextView>(R.id.txtPlayerNickname)
                val elem = view.findViewById<ImageView>(R.id.imgElement)
                val p = slot.player

                if (p != null) {
                    img.setImageResource(p.image)
                    elem.setImageResource(p.element)
                    txt.text = p.nickname
                } else {
                    img.setImageResource(0)
                    elem.setImageResource(0)
                    txt.text = slot.role
                }

                val lp = RelativeLayout.LayoutParams(cardW.toInt(), cardH.toInt())
                lp.leftMargin = (startX + i * (cardW + hGap)).toInt()
                lp.topMargin = (yCenter - cardH / 2f).toInt()
                view.layoutParams = lp

                val index = globalIndex

                // Drag desde campo
                if (p != null) {
                    view.setOnLongClickListener {
                        val shadow = View.DragShadowBuilder(it)
                        val clip = ClipData.newPlainText("fromIndex", index.toString())
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                            it.startDragAndDrop(clip, shadow, index, 0)
                        else @Suppress("DEPRECATION")
                        it.startDrag(clip, shadow, index, 0)
                        it.alpha = 0.5f
                        true
                    }
                }

                // Drop destino campo
                view.setOnDragListener { v, event ->
                    when (event.action) {
                        DragEvent.ACTION_DRAG_STARTED -> true
                        DragEvent.ACTION_DROP -> {
                            val fromIdx = event.localState as? Int
                            val fromBench = event.clipDescription?.label == "benchPlayer"
                            if (fromIdx != null && !fromBench) {
                                val src = slots[fromIdx].player
                                val dst = slots[index].player
                                val ok = src?.canPlay(slot.role) ?: true
                                if (ok) {
                                    slots[fromIdx].player = dst
                                    slots[index].player = src
                                    drawSlots()
                                }
                            } else if (fromBench) {
                                val benchIdx = event.localState as Int
                                val player = benchPlayers[benchIdx]
                                if (player.canPlay(slot.role)) {
                                    val replaced = slot.player
                                    slot.player = player
                                    benchPlayers[benchIdx] = replaced ?: benchPlayers[benchIdx]
                                    drawSlots()
                                    findViewById<RecyclerView>(R.id.rvBench).adapter?.notifyDataSetChanged()
                                }
                            }
                            true
                        }
                        DragEvent.ACTION_DRAG_ENDED -> { v.alpha = 1f; true }
                        else -> false
                    }
                }

                fieldLayout.addView(view)
                globalIndex++
            }
        }
    }

    // ---------- BANQUILLO ----------
    private fun setupBenchPanel() {
        val benchPanel = findViewById<View>(R.id.benchPanel)
        val rvBench = findViewById<RecyclerView>(R.id.rvBench)
        val btnCloseBench = findViewById<Button>(R.id.btnCloseBench)

        rvBench.layoutManager = LinearLayoutManager(this)
        rvBench.adapter = FinalTeamAdapter(benchPlayers)

        // botón flotante
        val btnShowBench = FloatingActionButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_sort_by_size)
            setOnClickListener {
                benchPanel.animate().translationX(0f).setDuration(250).start()
            }
        }
        (findViewById<ViewGroup>(R.id.fieldLayout)).addView(btnShowBench)

        btnCloseBench.setOnClickListener {
            benchPanel.animate().translationX(benchPanel.width.toFloat()).setDuration(250).start()
        }

        // Drag hacia banquillo
        rvBench.setOnDragListener { _, event ->
            when (event.action) {
                DragEvent.ACTION_DROP -> {
                    val fromIdx = event.localState as? Int ?: return@setOnDragListener true
                    val p = slots.getOrNull(fromIdx)?.player ?: return@setOnDragListener true
                    if (benchPlayers.size < 5) {
                        benchPlayers.add(p)
                        slots[fromIdx].player = null
                        drawSlots()
                        rvBench.adapter?.notifyDataSetChanged()
                    }
                    true
                }
                else -> true
            }
        }

        // Drag desde banquillo
        rvBench.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onLongPress(e: MotionEvent) {
                val child = rvBench.findChildViewUnder(e.x, e.y) ?: return
                val pos = rvBench.getChildAdapterPosition(child)
                val clip = ClipData.newPlainText("benchPlayer", "benchPlayer")
                val shadow = View.DragShadowBuilder(child)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    child.startDragAndDrop(clip, shadow, pos, 0)
                else @Suppress("DEPRECATION") child.startDrag(clip, shadow, pos, 0)
            }
        })
    }
}
