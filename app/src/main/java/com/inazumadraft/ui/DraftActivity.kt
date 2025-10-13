package com.inazumadraft.ui

import android.content.ClipData
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.DragEvent
import android.view.Gravity
import android.view.MotionEvent
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
import com.inazumadraft.R
import com.inazumadraft.data.Formation
import com.inazumadraft.data.PlayerRepository
import com.inazumadraft.data.formations
import com.inazumadraft.model.Player
import com.inazumadraft.model.canPlay
import com.inazumadraft.ui.adapters.BenchSelectedAdapter

class DraftActivity : AppCompatActivity() {

    private lateinit var overlayPreview: View
    private lateinit var fieldLayout: RelativeLayout
    private lateinit var btnFormation1: Button
    private lateinit var btnFormation2: Button
    private lateinit var btnFormation3: Button
    private lateinit var btnFormation4: Button
    private lateinit var rvOptions: RecyclerView
    private lateinit var btnNext: Button
    private lateinit var roundTitle: TextView

    private var visibleFormations: List<Formation> = emptyList()
    private var selectedFormation: Formation? = null
    private var formationLocked: Boolean = false

    private data class Slot(var role: String, var player: Player? = null)
    private val slots: MutableList<Slot> = mutableListOf()
    private val selectedPlayers: MutableList<Player> = mutableListOf()
    private var captain: Player? = null
    private var rowSpec: List<Int> = emptyList()

    private var isPreviewing = false

    // ------- BANQUILLO -------
    private val benchPlayers = MutableList<Player?>(5) { null }
    private val benchPickOptions: MutableList<List<Player>?> = MutableList(5) { null }

    // ------- Drawer del banquillo: estado + hot-zone -------
    private var isBenchOpen = false
    private var benchHotZone: View? = null

    private fun openBench(root: View, drawer: View, scrim: View, onOpen: () -> Unit = {}) {
        root.visibility = View.VISIBLE
        root.bringToFront()
        scrim.visibility = View.VISIBLE
        drawer.animate().translationX(0f).setDuration(220)
            .withStartAction { onOpen() }
            .start()
        isBenchOpen = true
    }

    private fun closeBench(root: View, drawer: View, scrim: View, onClose: () -> Unit = {}) {
        drawer.animate().translationX(drawer.width.toFloat()).setDuration(200)
            .withEndAction {
                scrim.visibility = View.GONE
                root.visibility = View.GONE
                onClose()
            }.start()
        isBenchOpen = false
    }

    private fun ensureBenchHotZone(root: View, drawer: View, scrim: View, onOpen: () -> Unit) {
        val parent: ViewGroup = findViewById(android.R.id.content)
        if (benchHotZone != null) return

        val d = resources.displayMetrics.density
        benchHotZone = View(this).apply {
            layoutParams = FrameLayout.LayoutParams((28 * d).toInt(), FrameLayout.LayoutParams.MATCH_PARENT).apply {
                gravity = Gravity.END
            }
            setOnDragListener { _, event ->
                if (event.action == DragEvent.ACTION_DRAG_ENTERED && !isBenchOpen) {
                    openBench(root, drawer, scrim, onOpen)
                    return@setOnDragListener true
                }
                false
            }
        }
        parent.addView(benchHotZone)
    }

    // ------- PERF -------
    private val slotViews: MutableList<View> = mutableListOf()
    private var drawPending = false
    private fun requestDrawSlots() {
        if (drawPending) return
        drawPending = true
        fieldLayout.post {
            drawPending = false
            drawSlotsInternal()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_draft)

        fieldLayout = findViewById(R.id.fieldLayout)
        btnFormation1 = findViewById(R.id.btnFormation1)
        btnFormation2 = findViewById(R.id.btnFormation2)
        btnFormation3 = findViewById(R.id.btnFormation3)
        btnFormation4 = findViewById(R.id.btnFormation4)
        rvOptions = findViewById(R.id.rvOptions)
        btnNext = findViewById(R.id.btnNext)
        roundTitle = findViewById(R.id.roundTitle)
        overlayPreview = findViewById(R.id.overlayPreview)

        refreshVisibleFormations()

        btnFormation1.setOnClickListener { selectFormationByIndex(0) }
        btnFormation2.setOnClickListener { selectFormationByIndex(1) }
        btnFormation3.setOnClickListener { selectFormationByIndex(2) }
        btnFormation4.setOnClickListener { selectFormationByIndex(3) }

        btnNext.setOnClickListener {
            val formation = selectedFormation ?: return@setOnClickListener
            val team = buildFinalTeamFromSlots(formation)
            if (team.size == formation.positions.size) {
                val intent = Intent(this, FinalTeamActivity::class.java)
                intent.putParcelableArrayListExtra("finalTeam", ArrayList(team))
                intent.putExtra("formation", formation.name)
                intent.putExtra("captainName", captain?.name ?: "")
                val benchList = ArrayList(benchPlayers.filterNotNull())
                intent.putParcelableArrayListExtra("benchPlayers", benchList)
                startActivity(intent)
            }
        }

        rvOptions.visibility = View.GONE
        fieldLayout.visibility = View.GONE
        findViewById<View>(R.id.formationButtonsLayout).bringToFront()
        formationLocked = false

        setupBenchPanel()
    }

    // ----------------- BANQUILLO -----------------
    private fun benchCount(): Int = benchPlayers.count { it != null }

    private fun showBenchOptionsForSlot(slotIndex: Int) {
        val options = benchPickOptions[slotIndex] ?: run {
            val usados = mutableSetOf<Player>().apply {
                captain?.let { add(it) }
                addAll(slots.mapNotNull { it.player })
                addAll(benchPlayers.filterNotNull())
            }
            val list = PlayerRepository.players
                .filter { it !in usados }
                .shuffled()
                .take(4)
            benchPickOptions[slotIndex] = list
            list
        }

        val dialogView = layoutInflater.inflate(R.layout.layout_player_picker, null)
        val dialog = android.app.Dialog(this)
        dialog.setContentView(dialogView)
        val title = dialogView.findViewById<TextView>(R.id.txtTitle)
        val rvPicker = dialogView.findViewById<RecyclerView>(R.id.rvPickerOptions)
        title.text = "Elige Suplente"

        rvPicker.layoutManager = GridLayoutManager(this, 2)
        rvPicker.setHasFixedSize(true)
        rvPicker.adapter = OptionAdapter(
            options,
            onClick = { chosen ->
                benchPlayers[slotIndex] = chosen
                findViewById<RecyclerView?>(R.id.rvBenchSelected)?.adapter?.notifyItemChanged(slotIndex)
                findViewById<TextView?>(R.id.txtBenchTitle)?.text = "Banquillo (${benchCount()}/5)"
                dialog.dismiss()
            }
        )

        dialog.show()
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
            drawer.translationX = drawer.width.toFloat()
            root.visibility = View.GONE
            scrim.visibility = View.GONE
        }
        scrim.setOnClickListener { closeBench(root, drawer, scrim, onClose) }

        val d = handleParent.resources.displayMetrics.density
        val handle = Button(handleParent.context).apply {
            text = "BANQUILLO"
            rotation = -90f
            setAllCaps(true)
            setBackgroundColor(0xff_ff_cc_00.toInt())
            setTextColor(0xff_00_00_00.toInt())
            elevation = 16f
            tag = "bench_handle"
            setOnClickListener { openBench(root, drawer, scrim, onOpen) }
        }
        val lp = FrameLayout.LayoutParams((44 * d).toInt(), (120 * d).toInt()).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            marginEnd = (4 * d).toInt()
        }
        handle.layoutParams = lp
        if (handleParent.findViewWithTag<View>("bench_handle") == null) {
            handleParent.addView(handle)
            handle.bringToFront()
        }
    }

    // ----------------- FORMATION FLOW -----------------
    private fun refreshVisibleFormations() {
        visibleFormations = if (formations.size >= 4) formations.shuffled().take(4) else formations
        btnFormation1.text = visibleFormations.getOrNull(0)?.name ?: "—"
        btnFormation2.text = visibleFormations.getOrNull(1)?.name ?: "—"
        btnFormation3.text = visibleFormations.getOrNull(2)?.name ?: "—"
        btnFormation4.text = visibleFormations.getOrNull(3)?.name ?: "—"

        rvOptions.visibility = View.GONE
        btnNext.visibility = View.GONE
        fieldLayout.visibility = View.GONE
        roundTitle.text = "Elige una formación"

        clearDraftState()
        setFormationButtonsEnabled(true)
        formationLocked = false
    }

    private fun selectFormationByIndex(i: Int) {
        if (formationLocked) return
        val f = visibleFormations.getOrNull(i) ?: return
        selectedFormation = f

        findViewById<View>(R.id.formationButtonsLayout).visibility = View.GONE

        clearDraftState()
        roundTitle.text = "Elige tu capitán (${f.name})"
        showCaptainOptions()

        formationLocked = true
        setFormationButtonsEnabled(false)
        highlightChosenButton(i)
    }

    private fun setFormationButtonsEnabled(enabled: Boolean) {
        val alpha = if (enabled) 1f else 0.5f
        listOf(btnFormation1, btnFormation2, btnFormation3, btnFormation4).forEach {
            it.isEnabled = enabled
            it.alpha = alpha
        }
    }

    private fun highlightChosenButton(index: Int) {
        listOf(btnFormation1, btnFormation2, btnFormation3, btnFormation4).forEachIndexed { idx, b ->
            b.alpha = if (idx == index) 1f else 0.4f
        }
    }

    private fun clearDraftState() {
        slots.clear()
        selectedPlayers.clear()
        captain = null
        rowSpec = emptyList()
        for (i in benchPlayers.indices) benchPlayers[i] = null
        for (i in benchPickOptions.indices) benchPickOptions[i] = null
    }

    private fun toCode(pos: String): String = when (pos.trim().lowercase()) {
        "portero", "pt" -> "PT"
        "defensa", "df" -> "DF"
        "centrocampista", "mc" -> "MC"
        "delantero", "dl" -> "DL"
        else -> pos.trim().uppercase()
    }

    private fun codeToLabel(code: String): String = when (code.uppercase()) {
        "PT" -> "Portero"; "DF" -> "Defensa"; "MC" -> "Centrocampista"; "DL" -> "Delantero"; else -> code
    }

    private fun showCaptainOptions() {
        rvOptions.visibility = View.VISIBLE
        rvOptions.bringToFront()
        btnNext.visibility = View.GONE
        fieldLayout.visibility = View.GONE
        val options = PlayerRepository.players.shuffled().take(4)

        rvOptions.layoutManager = GridLayoutManager(this, 2)
        rvOptions.setHasFixedSize(true)

        rvOptions.adapter = OptionAdapter(
            players = options,
            onClick = { player ->
                captain = player
                roundTitle.text = "Capitán: ${player.name}\nToca los huecos para rellenar la alineación"
                buildSlotsTemplateAndPlaceCaptain()

                fieldLayout.visibility = View.VISIBLE
                rvOptions.visibility = View.GONE
                btnNext.visibility = View.GONE

                fieldLayout.post {
                    fieldLayout.bringToFront()
                    requestDrawSlots()
                }
            }
        )
    }

    private fun buildSlotsTemplateAndPlaceCaptain() {
        val formation = selectedFormation ?: return
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
        rowSpec = buildList { addAll(dlRows); addAll(mcRows); addAll(dfRows); addAll(ptRows) }

        slots.clear()
        repeat(nDL) { slots.add(Slot("DL")) }
        repeat(nMC) { slots.add(Slot("MC")) }
        repeat(nDF) { slots.add(Slot("DF")) }
        repeat(nPT) { slots.add(Slot("PT")) }

        captain?.let { cap ->
            val idx = slots.indexOfFirst { slot -> slot.player == null && cap.canPlay(slot.role) }
            if (idx >= 0) { slots[idx].player = cap; selectedPlayers.add(cap) }
        }

        findViewById<TextView?>(R.id.txtBenchTitle)?.text = "Banquillo (${benchCount()}/5)"
        findViewById<RecyclerView?>(R.id.rvBenchSelected)?.adapter?.notifyDataSetChanged()
        requestDrawSlots()
    }

    // ----------------- RENDER DEL CAMPO + DnD -----------------
    private fun drawSlotsInternal() {
        if (fieldLayout.width == 0 || fieldLayout.height == 0) {
            fieldLayout.post { drawSlotsInternal() }
            return
        }

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
            val t = if (rowsCount == 1) 0.5f else r / (rowsCount - 1f).toFloat()
            topPad + t * (bottomPad - topPad)
        }

        val totalSlots = slots.size
        while (slotViews.size < totalSlots) {
            val v = layoutInflater.inflate(R.layout.item_player_field, fieldLayout, false)
            slotViews.add(v)
            fieldLayout.addView(v)
        }
        while (slotViews.size > totalSlots) {
            val last = slotViews.removeAt(slotViews.lastIndex)
            fieldLayout.removeView(last)
        }

        var slotGlobalIndex = 0
        rowSpec.forEachIndexed { rowIdx, cols ->
            val n = cols
            val rowWidth = n * cardW + (n - 1) * hGap
            val startX = (fieldLayout.width - rowWidth) / 2f
            val yCenter = yCenters[rowIdx]

            repeat(n) { i ->
                val view = slotViews[slotGlobalIndex]
                val slot = slots[slotGlobalIndex]

                val img = view.findViewById<ImageView>(R.id.imgPlayer)
                val name = view.findViewById<TextView>(R.id.txtPlayerNickname)
                val elem = view.findViewById<ImageView>(R.id.imgElement)

                if (slot.player != null) {
                    val p = slot.player!!
                    img.setImageResource(p.image)
                    elem.setImageResource(p.element)
                    name.text = p.nickname
                    if (p == captain) img.setBackgroundResource(R.drawable.captain_border) else img.background = null
                } else {
                    img.setImageResource(0)
                    elem.setImageResource(0)
                    name.text = codeToLabel(slot.role)
                    img.background = null
                }

                val lp = (view.layoutParams as? RelativeLayout.LayoutParams)
                    ?: RelativeLayout.LayoutParams(cardW.toInt(), cardH.toInt())
                lp.width = cardW.toInt()
                lp.height = cardH.toInt()
                lp.leftMargin = (startX + i * (cardW + hGap)).toInt()
                lp.topMargin = (yCenter - cardH / 2f).toInt()
                view.layoutParams = lp

                val indexForClick = slotGlobalIndex
                view.setOnClickListener {
                    if (isPreviewing) return@setOnClickListener
                    if (slots[indexForClick].player == null) showOptionsForSlot(indexForClick)
                }
                view.setOnLongClickListener(
                    if (slot.player != null) {
                        View.OnLongClickListener {
                            val shadow = View.DragShadowBuilder(it)
                            val clip = ClipData.newPlainText("fromIndex", indexForClick.toString())
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) it.startDragAndDrop(clip, shadow, indexForClick, 0)
                            else @Suppress("DEPRECATION") it.startDrag(clip, shadow, indexForClick, 0)
                            it.alpha = 0.5f
                            true
                        }
                    } else null
                )
                view.setOnDragListener { v, event ->
                    when (event.action) {
                        DragEvent.ACTION_DRAG_STARTED -> true
                        DragEvent.ACTION_DRAG_ENTERED -> { v.alpha = 0.7f; true }
                        DragEvent.ACTION_DRAG_EXITED  -> { v.alpha = 1f; true }
                        DragEvent.ACTION_DROP -> {
                            val fromBench = event.clipDescription?.label == "benchPlayer"
                            if (fromBench) {
                                val benchIdx = (event.localState as? Int)
                                if (benchIdx == null || benchIdx !in benchPlayers.indices) { shakeView(v); return@setOnDragListener true }
                                val p = benchPlayers[benchIdx] ?: run { shakeView(v); return@setOnDragListener true }
                                if (!p.canPlay(slots[indexForClick].role)) {
                                    shakeView(v)
                                    Toast.makeText(this@DraftActivity, "No puede jugar de ${codeToLabel(slots[indexForClick].role)}", Toast.LENGTH_SHORT).show()
                                    return@setOnDragListener true
                                }
                                val replaced = slots[indexForClick].player
                                slots[indexForClick].player = p
                                benchPlayers[benchIdx] = replaced
                                findViewById<RecyclerView?>(R.id.rvBenchSelected)?.adapter?.notifyItemChanged(benchIdx)
                                findViewById<TextView?>(R.id.txtBenchTitle)?.text = "Banquillo (${benchCount()}/5)"
                                requestDrawSlots()
                            } else {
                                val from = event.localState as? Int
                                if (from != null && from != indexForClick) {
                                    val src = slots[from].player
                                    val dst = slots[indexForClick].player
                                    val okSrc = src?.canPlay(slots[indexForClick].role) ?: true
                                    val okDst = dst?.canPlay(slots[from].role) ?: true
                                    if (okSrc && okDst) {
                                        val temp = slots[from].player
                                        slots[from].player = slots[indexForClick].player
                                        slots[indexForClick].player = temp
                                        requestDrawSlots()
                                    } else shakeView(v)
                                }
                            }
                            true
                        }
                        DragEvent.ACTION_DRAG_ENDED -> { v.alpha = 1f; true }
                        else -> false
                    }
                }

                slotGlobalIndex++
            }
        }

        val total = selectedFormation?.positions?.size ?: 0
        val filled = slots.count { it.player != null }
        btnNext.visibility = if (filled == total) View.VISIBLE else View.GONE
    }

    // ----------------- PLAYER PICKER DE CAMPO -----------------
    private fun showOptionsForSlot(slotIndex: Int) {
        val slot = slots[slotIndex]
        val role = slot.role.uppercase()

        val options = PlayerRepository.players
            .filter { p ->
                p.canPlay(role) &&
                        p != captain &&
                        p !in selectedPlayers &&
                        p !in slots.mapNotNull { s -> s.player } &&
                        p !in benchPlayers.filterNotNull()
            }
            .shuffled()
            .take(4)

        val dialogView = layoutInflater.inflate(R.layout.layout_player_picker, null)
        val dialog = android.app.Dialog(this)
        dialog.setContentView(dialogView)

        val title = dialogView.findViewById<TextView>(R.id.txtTitle)
        val rvPicker = dialogView.findViewById<RecyclerView>(R.id.rvPickerOptions)

        title.text = "Elige ${codeToLabel(role)}"
        rvPicker.layoutManager = GridLayoutManager(this, 2)
        rvPicker.setHasFixedSize(true)

        rvPicker.adapter = OptionAdapter(
            options,
            onClick = { chosen ->
                slots[slotIndex].player = chosen
                selectedPlayers.add(chosen)
                dialog.dismiss()
                fieldLayout.post { requestDrawSlots() }
            },
            onLongClick = { player ->
                showPreviewOnLongPress(slotIndex, player, dialog, options)
            }
        )

        dialog.show()
    }

    private fun showPreviewOnLongPress(
        slotIndex: Int,
        player: Player,
        dialog: android.app.Dialog,
        currentOptions: List<Player>
    ) {
        val backup = slots.map { it.copy() }
        isPreviewing = true
        dialog.dismiss()

        slots[slotIndex].player = player
        requestDrawSlots()

        fieldLayout.post {
            fieldLayout.getChildAt(slotIndex)
                ?.findViewById<ImageView>(R.id.imgPlayer)
                ?.setBackgroundResource(R.drawable.captain_border)
        }

        overlayPreview.visibility = View.VISIBLE
        overlayPreview.alpha = 0.25f

        overlayPreview.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    for (i in slots.indices) slots[i].player = backup[i].player
                    requestDrawSlots()
                    isPreviewing = false
                    overlayPreview.visibility = View.GONE
                    overlayPreview.setOnTouchListener(null)
                    reopenSamePlayerPicker(slotIndex, currentOptions)
                    true
                }
                else -> true
            }
        }
    }

    private fun reopenSamePlayerPicker(slotIndex: Int, sameOptions: List<Player>) {
        val slot = slots[slotIndex]
        val dialogView = layoutInflater.inflate(R.layout.layout_player_picker, null)
        val dialog = android.app.Dialog(this)
        dialog.setContentView(dialogView)
        val title = dialogView.findViewById<TextView>(R.id.txtTitle)
        val rvPicker = dialogView.findViewById<RecyclerView>(R.id.rvPickerOptions)
        title.text = "Elige ${codeToLabel(slot.role)}"
        rvPicker.layoutManager = GridLayoutManager(this, 2)
        rvPicker.setHasFixedSize(true)
        rvPicker.adapter = OptionAdapter(
            sameOptions,
            onClick = { chosen ->
                slots[slotIndex].player = chosen
                selectedPlayers.add(chosen)
                dialog.dismiss()
                fieldLayout.post { requestDrawSlots() }
            },
            onLongClick = { player ->
                showPreviewOnLongPress(slotIndex, player, dialog, sameOptions)
            }
        )
        dialog.show()
    }

    private fun buildFinalTeamFromSlots(formation: Formation): List<Player> {
        val result = mutableListOf<Player>()
        val tmp = slots.toMutableList()
        formation.positions.forEach { pos ->
            val idx = tmp.indexOfFirst { it.role.equals(pos, ignoreCase = true) && it.player != null }
            if (idx >= 0) { result.add(tmp[idx].player!!); tmp.removeAt(idx) }
        }
        return result
    }

    private fun setupBenchPanel() {
        val root   = findViewById<View?>(R.id.benchPanel) ?: return
        val drawer = findViewById<View?>(R.id.benchDrawer) ?: return
        val scrim  = findViewById<View?>(R.id.benchScrim) ?: return
        val rvSel  = findViewById<RecyclerView?>(R.id.rvBenchSelected) ?: return
        val rvOpts = findViewById<RecyclerView?>(R.id.rvBenchOptions) ?: return
        val btnClose = findViewById<Button?>(R.id.btnCloseBench) // puede existir en el XML
        val txtTitle = findViewById<TextView?>(R.id.txtBenchTitle) ?: return

        // 👇 Ocultar el botón Cerrar
        btnClose?.visibility = View.GONE

        fun updateTitle() { txtTitle.text = "Banquillo (${benchCount()}/5)" }
        updateTitle()

        // 👇 Cambiamos a VERTICAL (arriba → abajo)
        rvSel.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        rvSel.itemAnimator = null
        rvSel.adapter = BenchSelectedAdapter(
            benchPlayers = benchPlayers,
            onChanged = { updateTitle() },
            onDropFromField = { toIndex, fromFieldIndex ->
                val p = slots.getOrNull(fromFieldIndex)?.player ?: return@BenchSelectedAdapter
                val replaced = benchPlayers[toIndex]
                benchPlayers[toIndex] = p
                slots[fromFieldIndex].player = replaced
                rvSel.adapter?.notifyItemChanged(toIndex)
                updateTitle()
                requestDrawSlots()
            },
            onClickSlot = { index -> showBenchOptionsForSlot(index) }
        )

        // La grilla inferior no se usa
        rvOpts.layoutManager = GridLayoutManager(this, 2)
        rvOpts.adapter = OptionAdapter(emptyList(), onClick = { _: Player -> })

        // Cierre con scrim
        scrim.setOnClickListener { closeBench(root, drawer, scrim) }

        val activityRoot: ViewGroup = findViewById(android.R.id.content)
        setupBenchDrawerBasics(
            root = root,
            drawer = drawer,
            scrim = scrim,
            handleParent = activityRoot,
            onOpen = {
                updateTitle()
                rvSel.adapter?.notifyDataSetChanged()
            },
            onClose = { }
        )

        // Hot-zone: abre al arrastrar hacia el borde derecho
        ensureBenchHotZone(
            root = root, drawer = drawer, scrim = scrim
        ) {
            updateTitle()
            rvSel.adapter?.notifyDataSetChanged()
        }
    }

    private fun shakeView(v: View) {
        v.animate().translationX(12f).setDuration(50).withEndAction {
            v.animate().translationX(-10f).setDuration(50).withEndAction {
                v.animate().translationX(0f).setDuration(40).start()
            }.start()
        }.start()
    }
}
