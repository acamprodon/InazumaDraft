package com.inazumadraft.ui

import android.content.ClipData
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.DragEvent
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.inazumadraft.R
import com.inazumadraft.data.PlayerRepository
import com.inazumadraft.data.formationCoordinates
import com.inazumadraft.data.formations
import com.inazumadraft.model.Player
import com.inazumadraft.model.canPlay
import com.inazumadraft.ui.adapters.BenchSelectedAdapter
import com.inazumadraft.ui.adapters.OptionAdapter
import com.inazumadraft.ui.adapters.FinalTeamAdapter
import kotlin.math.max

class FinalTeamActivity : AppCompatActivity() {

    private lateinit var fieldLayout: RelativeLayout
    private lateinit var btnToggleView: Button
    private lateinit var btnNewTeam: FloatingActionButton
    private lateinit var recyclerFinalTeam: RecyclerView

    private val playerSlots = mutableListOf<Player?>()
    private val benchPlayers = MutableList<Player?>(5) { null }
    private var formationName: String = "4-4-2"
    private var captainName: String? = null

    private val slotViews: MutableList<View> = mutableListOf()
    private var drawPending = false

    private var isBenchOpen = false
    private var benchHotZone: View? = null
    private var benchHandle: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_final_team)

        fieldLayout = findViewById(R.id.fieldLayout)
        btnToggleView = findViewById(R.id.btnToggleView)
        recyclerFinalTeam = findViewById(R.id.recyclerFinalTeam)
        btnNewTeam = findViewById(R.id.btnNewTeam)

        val team = intent.getParcelableArrayListExtra<Player>("finalTeam") ?: arrayListOf()
        formationName = intent.getStringExtra("formation") ?: "4-4-2"
        captainName = intent.getStringExtra("captainName")

        playerSlots.clear()
        playerSlots.addAll(team)
        intent.getParcelableArrayListExtra<Player>("benchPlayers")?.let { list ->
            for (i in 0 until 5) benchPlayers[i] = list.getOrNull(i)
        }

        fieldLayout.post { requestDrawField() }

        recyclerFinalTeam.layoutManager = LinearLayoutManager(this)
        refreshStatsList()

        btnToggleView.visibility = View.VISIBLE
        btnToggleView.text = "VER ESTADÍSTICAS"
        btnToggleView.setOnClickListener {
            if (recyclerFinalTeam.visibility == View.GONE) {
                recyclerFinalTeam.visibility = View.VISIBLE
                fieldLayout.visibility = View.GONE
                refreshStatsList()
                btnToggleView.text = "VER CAMPO"
            } else {
                recyclerFinalTeam.visibility = View.GONE
                fieldLayout.visibility = View.VISIBLE
                btnToggleView.text = "VER ESTADÍSTICAS"
            }
        }

        btnNewTeam.setOnClickListener {
            val i = Intent(this@FinalTeamActivity, DraftActivity::class.java)
            i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(i); finish()
        }

        setupBenchPanel()
    }

    // --------- Stats list ---------
    private fun refreshStatsList() {
        val combined = ArrayList<Player>().apply {
            addAll(playerSlots.filterNotNull())
            addAll(benchPlayers.filterNotNull())
        }
        recyclerFinalTeam.adapter = FinalTeamAdapter(combined)
    }

    // --------- Bench Drawer ---------
    private fun openBenchDrawer(root: View, drawer: View, scrim: View, onOpen: () -> Unit = {}) {
        root.visibility = View.VISIBLE
        root.bringToFront()
        scrim.visibility = View.VISIBLE
        drawer.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        drawer.animate().translationX(0f).setDuration(220)
            .withStartAction { onOpen() }
            .withEndAction { drawer.setLayerType(View.LAYER_TYPE_NONE, null) }
            .start()
        isBenchOpen = true
    }

    private fun closeBenchDrawer(root: View, drawer: View, scrim: View, onClose: () -> Unit = {}) {
        drawer.animate().translationX(drawer.width.toFloat()).setDuration(200)
            .withEndAction {
                scrim.visibility = View.GONE
                root.visibility = View.GONE
                drawer.setLayerType(View.LAYER_TYPE_NONE, null)
                onClose()
            }.start()
        isBenchOpen = false
    }

    private fun setupBenchDrawerBasics(
        root: View,
        drawer: View,
        scrim: View,
        handleParent: ViewGroup,
        onOpen: () -> Unit,
        onClose: () -> Unit
    ) {
        drawer.post {
            val w = (resources.displayMetrics.widthPixels * 0.42f).toInt()
            val min = (200 * resources.displayMetrics.density).toInt()
            drawer.layoutParams = drawer.layoutParams.apply { width = max(min, w) }
            drawer.translationX = drawer.width.toFloat()
            root.visibility = View.GONE
            scrim.visibility = View.GONE
        }
        scrim.setOnClickListener { closeBenchDrawer(root, drawer, scrim, onClose) }

        if (benchHandle == null) {
            val d = resources.displayMetrics.density
            benchHandle = Button(handleParent.context).apply {
                text = "BANQUILLO"
                rotation = -90f
                setAllCaps(true)
                setBackgroundColor(0xff_ff_cc_00.toInt())
                setTextColor(0xff_00_00_00.toInt())
                elevation = 16f
                tag = "bench_handle_final"
                setOnClickListener { openBenchDrawer(root, drawer, scrim, onOpen) }
                layoutParams = FrameLayout.LayoutParams((44 * d).toInt(), (120 * d).toInt()).apply {
                    gravity = Gravity.END or Gravity.CENTER_VERTICAL
                    marginEnd = (4 * d).toInt()
                }
            }
            handleParent.addView(benchHandle)
            benchHandle?.bringToFront()
        } else benchHandle?.visibility = View.VISIBLE
    }

    private fun ensureBenchHotZone(root: View, drawer: View, scrim: View, onOpen: () -> Unit) {
        val parent: ViewGroup = findViewById(android.R.id.content)
        if (benchHotZone != null) return
        val d = resources.displayMetrics.density
        benchHotZone = View(this).apply {
            layoutParams = FrameLayout.LayoutParams((28 * d).toInt(), FrameLayout.LayoutParams.MATCH_PARENT).apply {
                gravity = Gravity.END
            }
            setOnDragListener { _, e ->
                if (e.action == DragEvent.ACTION_DRAG_ENTERED && !isBenchOpen) {
                    openBenchDrawer(root, drawer, scrim, onOpen); true
                } else false
            }
        }
        parent.addView(benchHotZone)
    }

    private fun setupBenchPanel() {
        val root   = findViewById<View?>(R.id.benchPanel) ?: return
        val drawer = findViewById<View?>(R.id.benchDrawer) ?: return
        val scrim  = findViewById<View?>(R.id.benchScrim) ?: return
        val rvSel  = findViewById<RecyclerView?>(R.id.rvBenchSelected) ?: return
        val rvOpts = findViewById<RecyclerView?>(R.id.rvBenchOptions) ?: return
        val txtTitle = findViewById<TextView?>(R.id.txtBenchTitle) ?: return

        fun updateTitle() { txtTitle.text = "Banquillo (${benchSize()}/5)" }
        updateTitle()

        rvSel.layoutManager = LinearLayoutManager(this)
        rvSel.itemAnimator = null
        rvSel.adapter = BenchSelectedAdapter(
            benchPlayers = benchPlayers,
            onChanged = { updateTitle(); refreshStatsList() },
            onDropFromField = { toIndex, fromFieldIndex ->
                val p = playerSlots.getOrNull(fromFieldIndex) ?: return@BenchSelectedAdapter
                val replaced = benchPlayers[toIndex]
                benchPlayers[toIndex] = p
                playerSlots[fromFieldIndex] = replaced
                rvSel.adapter?.notifyItemChanged(toIndex)
                updateTitle()
                refreshStatsList()
                requestDrawField()
            },
            onClickSlot = { index -> showBenchOptionsForSlotFinal(index) }
        )

        rvOpts.layoutManager = GridLayoutManager(this, 2)
        rvOpts.adapter = OptionAdapter(
            players = emptyList(),
            onClick = { /* no-op */ }
            // onPreview = null  // ← solo si tu OptionAdapter lo tiene
        )
        setupBenchDrawerBasics(
            root = root, drawer = drawer, scrim = scrim,
            handleParent = findViewById(android.R.id.content),
            onOpen = { updateTitle(); rvSel.adapter?.notifyDataSetChanged() },
            onClose = { }
        )
        ensureBenchHotZone(root, drawer, scrim) {
            updateTitle(); rvSel.adapter?.notifyDataSetChanged()
        }
    }

    private fun showBenchOptionsForSlotFinal(slotIndex: Int) {
        val usados = mutableSetOf<Player>().apply {
            addAll(playerSlots.filterNotNull())
            addAll(benchPlayers.filterNotNull())
        }
        val options = PlayerRepository.players
            .filter { it !in usados }
            .shuffled()
            .take(4)

        val dialogView = layoutInflater.inflate(R.layout.layout_player_picker, null)
        val dialog = android.app.Dialog(this)
        dialog.setContentView(dialogView)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)

        dialogView.findViewById<TextView>(R.id.txtTitle).text = "Elige Suplente"
        val rvPicker = dialogView.findViewById<RecyclerView>(R.id.rvPickerOptions)
        rvPicker.layoutManager = GridLayoutManager(this, 2)
        rvPicker.adapter = OptionAdapter(
            players = options,
            onClick = { chosen ->
                benchPlayers[slotIndex] = chosen
                findViewById<RecyclerView?>(R.id.rvBenchSelected)?.adapter?.notifyItemChanged(slotIndex)
                findViewById<TextView?>(R.id.txtBenchTitle)?.text = "Banquillo (${benchSize()}/5)"
                refreshStatsList()
                dialog.dismiss()
            }
            // onPreview = null  // si tu OptionAdapter tiene más callbacks, añádelos aquí
        )

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    // --------- Campo y DnD ---------
    private fun requestDrawField() {
        if (drawPending) return
        drawPending = true
        fieldLayout.post {
            drawPending = false
            drawTemplateAndFillInternal()
        }
    }

    private fun drawTemplateAndFillInternal() {
        if (fieldLayout.width == 0 || fieldLayout.height == 0) {
            fieldLayout.post { drawTemplateAndFillInternal() }; return
        }
        val d = resources.displayMetrics.density
        var cardW = 100f * d
        var cardH = cardW * 1.25f
        val hGap = 16f * d
        val maxCols = 4
        val rowWidthNeeded = maxCols * cardW + (maxCols - 1) * hGap
        if (rowWidthNeeded > fieldLayout.width) {
            cardW = (fieldLayout.width - (maxCols - 1) * hGap) / maxCols
            cardH = cardW * 1.25f
        }

        val formation = formations.firstOrNull { it.name == formationName } ?: return
        val coords = formationCoordinates[formationName]

        val totalSlots = formation.positions.size
        while (slotViews.size < totalSlots) {
            val v = layoutInflater.inflate(R.layout.item_player_field, fieldLayout, false)
            slotViews.add(v); fieldLayout.addView(v)
        }
        while (slotViews.size > totalSlots) {
            val last = slotViews.removeAt(slotViews.lastIndex)
            fieldLayout.removeView(last)
        }

        if (coords != null && coords.size == totalSlots) {
            val topBand = 0.05f
            val bottomBand = 0.90f
            fun mapY(y: Float) = (topBand + y * (bottomBand - topBand)).coerceIn(0f, 1f)

            formation.positions.forEachIndexed { i, _ ->
                val view = slotViews[i]
                bindSlotView(view, i)

                val roleCode = toCode(formation.positions[i])
                val lp = RelativeLayout.LayoutParams(cardW.toInt(), cardH.toInt())

                if (roleCode == "PT") {
                    val xPx = fieldLayout.width * 0.50f
                    val yPx = fieldLayout.height * 0.965f
                    lp.leftMargin = (xPx - cardW / 2f).toInt()
                    lp.topMargin = (yPx - cardH / 2f).toInt()
                } else {
                    val (x, yRaw) = coords[i]
                    val y = mapY(yRaw)
                    lp.leftMargin = (fieldLayout.width * x - cardW / 2f).toInt()
                    lp.topMargin  = (fieldLayout.height * y - cardH / 2f).toInt()
                }
                view.layoutParams = lp
                attachDragLogicToFieldView(view, i)
            }
        } else {
            val codes = formation.positions.map { toCode(it) }
            val nPT = codes.count { it == "PT" }
            val nDF = codes.count { it == "DF" }
            val nMC = codes.count { it == "MC" }
            val nDL = codes.count { it == "DL" }

            fun split(count: Int, maxPerRow: Int): List<Int> {
                if (count <= 0) return emptyList()
                val rows = mutableListOf<Int>()
                var left = count
                while (left > 0) { val take = left.coerceAtMost(maxPerRow); rows.add(take); left -= take }
                return rows
            }
            val dlRows = split(nDL, 3)
            val mcRows = split(nMC, 4)
            val dfRows = split(nDF, 4)
            val ptRows = if (nPT > 0) listOf(nPT) else emptyList()
            val rowSpec = buildList { addAll(dlRows); addAll(mcRows); addAll(dfRows); addAll(ptRows) }

            val topPad = fieldLayout.height * 0.06f
            val bottomPad = fieldLayout.height * 0.94f
            val rowsCount = rowSpec.size
            val spacing = (bottomPad - topPad) / (rowsCount + 1)
            val yCenters = (0 until rowsCount).map { r -> topPad + (r + 1) * spacing }

            var globalIndex = 0
            rowSpec.forEachIndexed { rowIdx, cols ->
                val n = cols
                val rowWidth = n * cardW + (n - 1) * hGap
                val startX = (fieldLayout.width - rowWidth) / 2f
                val yCenter = yCenters[rowIdx]

                repeat(n) { col ->
                    val index = globalIndex
                    val view = slotViews[index]
                    bindSlotView(view, index)

                    val lp = RelativeLayout.LayoutParams(cardW.toInt(), cardH.toInt())
                    lp.leftMargin = (startX + col * (cardW + hGap)).toInt()
                    lp.topMargin = (yCenter - cardH / 2f).toInt()
                    view.layoutParams = lp

                    attachDragLogicToFieldView(view, index)
                    globalIndex++
                }
            }

            // PT pegado a portería
            val ptIndex = formation.positions.indexOfFirst { toCode(it) == "PT" }
            if (ptIndex >= 0) {
                val view = slotViews[ptIndex]
                (view.layoutParams as? RelativeLayout.LayoutParams)?.let { lp ->
                    val yPx = fieldLayout.height * 0.965f
                    lp.topMargin = (yPx - cardH / 2f).toInt()
                    view.layoutParams = lp
                }
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
            if (p.name == captainName) img.setBackgroundResource(R.drawable.captain_border) else img.background = null
        } else {
            name.text = codeToNice(toCode(formation.positions[i]))
            elem.setImageResource(0)
            img.setImageResource(0)
            img.background = null
        }
    }

    private fun attachDragLogicToFieldView(view: View, index: Int) {
        val formation = formations.first { it.name == formationName }
        val dstRoleCode = toCode(formation.positions[index])

        val p = playerSlots.getOrNull(index)
        view.setOnLongClickListener(
            if (p != null)
                View.OnLongClickListener {
                    val clip = ClipData.newPlainText("fromIndex", index.toString())
                    val shadow = View.DragShadowBuilder(it)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) it.startDragAndDrop(clip, shadow, index, 0)
                    else @Suppress("DEPRECATION") it.startDrag(clip, shadow, index, 0)
                    it.alpha = 0.5f
                    true
                }
            else null
        )

        view.setOnDragListener { v, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> true
                DragEvent.ACTION_DRAG_ENTERED -> { v.alpha = 0.7f; true }
                DragEvent.ACTION_DRAG_EXITED  -> { v.alpha = 1f; true }
                DragEvent.ACTION_DROP -> {
                    val fromBench = event.clipDescription?.label == "benchPlayer"
                    if (fromBench) {
                        val benchIdx = event.localState as? Int
                        if (benchIdx == null || benchIdx !in benchPlayers.indices) { shakeView(v); return@setOnDragListener true }
                        val player = benchPlayers[benchIdx] ?: run { shakeView(v); return@setOnDragListener true }
                        if (!player.canPlay(dstRoleCode)) {
                            shakeView(v)
                            Toast.makeText(this@FinalTeamActivity, "No puede jugar en esta posición", Toast.LENGTH_SHORT).show()
                            return@setOnDragListener true
                        }
                        val replaced = playerSlots[index]
                        playerSlots[index] = player
                        benchPlayers[benchIdx] = replaced
                        findViewById<RecyclerView?>(R.id.rvBenchSelected)?.adapter?.notifyItemChanged(benchIdx)
                        refreshStatsList()
                        requestDrawField()
                    } else {
                        val fromIdx = event.localState as? Int ?: return@setOnDragListener true
                        if (fromIdx == index) return@setOnDragListener true
                        val src = playerSlots[fromIdx]
                        val dst = playerSlots[index]
                        val okSrc = src?.canPlay(dstRoleCode) ?: true
                        val okDst = dst?.canPlay(toCode(formation.positions[fromIdx])) ?: true
                        if (okSrc && okDst) {
                            val tmp = playerSlots[fromIdx]
                            playerSlots[fromIdx] = playerSlots[index]
                            playerSlots[index] = tmp
                            refreshStatsList()
                            requestDrawField()
                        } else shakeView(v)
                    }
                    true
                }
                DragEvent.ACTION_DRAG_ENDED -> { v.alpha = 1f; true }
                else -> false
            }
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
        "PT" -> "Portero"; "DF" -> "Defensa"; "MC" -> "Centrocampista"; "DL" -> "Delantero"; else -> code
    }

    private fun benchSize(): Int = benchPlayers.count { it != null }

    private fun shakeView(v: View) {
        v.animate().translationX(12f).setDuration(40).withEndAction {
            v.animate().translationX(-10f).setDuration(40).withEndAction {
                v.animate().translationX(0f).setDuration(40).start()
            }.start()
        }.start()
    }
}
