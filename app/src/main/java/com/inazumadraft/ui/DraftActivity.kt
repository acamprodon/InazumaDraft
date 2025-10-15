package com.inazumadraft.ui

import android.app.Dialog
import android.content.ClipData
import android.content.ClipDescription
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.inazumadraft.ui.adapters.OptionAdapter
import com.inazumadraft.ui.adapters.BenchSelectedAdapter
import com.inazumadraft.R
import com.inazumadraft.data.PlayerRepository
import com.inazumadraft.data.formationCoordinates
import com.inazumadraft.data.formations
import com.inazumadraft.model.Player
import com.inazumadraft.model.canPlay
import kotlin.math.max

class DraftActivity : AppCompatActivity() {

    // UI
    private lateinit var fieldLayout: RelativeLayout
    private lateinit var btnNext: Button
    private lateinit var roundTitle: TextView
    private lateinit var formationOverlay: View
    private lateinit var btnFormation1: Button
    private lateinit var btnFormation2: Button
    private lateinit var btnFormation3: Button
    private lateinit var btnFormation4: Button

    // Estado
    data class Slot(val role: String, var player: Player?)
    internal val slots = mutableListOf<Slot>()                 // <- usado por drag unificado
    private val slotViews: MutableList<View> = mutableListOf()
    private var drawPending = false

    private var formationLocked = false
    private var selectedFormationName: String? = null
    private lateinit var formationChoices: List<com.inazumadraft.data.Formation>
private lateinit var availablePlayers: List<Player>
private lateinit var selectedSeasons: List<String>
    // Capitán
    private var captain: Player? = null

    // Banquillo
    internal val benchPlayers = MutableList<Player?>(5) { null }
    private var isBenchOpen = false
    private var benchHandle: View? = null
    private var benchHotZone: View? = null

    // Selecciones TAP
    internal var pendingBenchIndex: Int? = null
    internal var pendingFieldIndex: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_draft)

        fieldLayout = findViewById(R.id.fieldLayout)
        btnNext = findViewById(R.id.btnNext)
        roundTitle = findViewById(R.id.roundTitle)
        formationOverlay = findViewById(R.id.formationOverlay)
        btnFormation1 = findViewById(R.id.btnFormation1)
        btnFormation2 = findViewById(R.id.btnFormation2)
        btnFormation3 = findViewById(R.id.btnFormation3)
        btnFormation4 = findViewById(R.id.btnFormation4)

        // Botones de formaciones aleatorias
        formationChoices = formations.shuffled().take(4)
        btnFormation1.text = formationChoices.getOrNull(0)?.name ?: "F1"
        btnFormation2.text = formationChoices.getOrNull(1)?.name ?: "F2"
        btnFormation3.text = formationChoices.getOrNull(2)?.name ?: "F3"
        btnFormation4.text = formationChoices.getOrNull(3)?.name ?: "F4"

        btnFormation1.setOnClickListener { selectFormationByIndex(0) }
        btnFormation2.setOnClickListener { selectFormationByIndex(1) }
        btnFormation3.setOnClickListener { selectFormationByIndex(2) }
        btnFormation4.setOnClickListener { selectFormationByIndex(3) }

        selectedSeasons = intent.getStringArrayListExtra("selectedSeasons") ?: arrayListOf()
        availablePlayers = if (selectedSeasons.isEmpty()) {
            PlayerRepository.players
        } else {
            PlayerRepository.players.filter { p -> p.season.any { it in selectedSeasons } }
        }

        btnNext.setOnClickListener {
            val filled = slots.count { it.player != null }
            if (filled != slots.size) {
                Toast.makeText(this, "Completa el 11 inicial", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val i = Intent(this, FinalTeamActivity::class.java).apply {
                putParcelableArrayListExtra("finalTeam", ArrayList(slots.mapNotNull { it.player }))
                putParcelableArrayListExtra("benchPlayers", ArrayList(benchPlayers.filterNotNull()))
                putExtra("formation", selectedFormationName ?: "4-4-2")
                putExtra("captainName", captain?.name)
            }
            startActivity(i)
        }

        setupBenchPanel()
        setBenchAccessEnabled(false)

    }

        // ---------------------- Formación y capitán ----------------------

    private fun selectFormationByIndex(i: Int) {
        if (formationLocked) return
        val f = formationChoices.getOrNull(i) ?: return
        selectedFormationName = f.name
        formationLocked = true

        findViewById<View>(R.id.formationButtonsLayout)?.visibility = View.GONE
        formationOverlay.visibility = View.GONE

        slots.clear()
        slots.addAll(f.positions.map { Slot(toCode(it), null) })
        slotViews.clear()
        fieldLayout.removeAllViews()

        roundTitle.text = "Elige tu capitán (${f.name})"
        showCaptainOptions()
    }

    private fun showCaptainOptions() {

        val options = availablePlayers.shuffled().take(4)
        val dialogView = layoutInflater.inflate(R.layout.layout_player_picker, null)
        val dialog = Dialog(this).apply {
            setContentView(dialogView); setCanceledOnTouchOutside(false); setCancelable(false)
        }

        dialogView.findViewById<TextView>(R.id.txtTitle).text = "Elige Capitán"
        val rvPicker = dialogView.findViewById<RecyclerView>(R.id.rvPickerOptions)
        rvPicker.layoutManager = GridLayoutManager(this, 2)
        rvPicker.adapter = OptionAdapter(
            players = options,
            onClick = { chosen ->
                captain = chosen
                placeCaptainInPrimaryPositionOnly()
                dialog.dismiss()
                roundTitle.text = "Completa tu 11 inicial"
                requestFieldRedraw()
            }
        )


        // Preview “live”: coloca candidato en su posición principal, oculta diálogo y permite volver
        attachPickerPreviewLive(rvPicker, dialog, options) { candidate ->
            val idx = slots.indexOfFirst { it.role.equals(candidate.position, true) }
            val prev = if (idx >= 0) slots[idx].player else null
            if (idx >= 0) slots[idx].player = candidate
            { if (idx >= 0) slots[idx].player = prev }
        }

        dialog.show()
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.92f).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun placeCaptainInPrimaryPositionOnly() {
        val cap = captain ?: return
        val idx = slots.indexOfFirst { it.player == null && it.role.equals(cap.position, true) }
        if (idx >= 0) slots[idx].player = cap
    }

    // ---------------------- Pickers por rol ----------------------

    private fun showOptionsForSlot(slotIndex: Int) {
        val used = slots.mapNotNull { it.player }.toMutableSet().apply { addAll(benchPlayers.filterNotNull()) }
        val needRole = slots[slotIndex].role
        val options = availablePlayers
            .filter { it !in used && it.position.equals(needRole, true) }
            .shuffled()
            .take(4)

        val dialogView = layoutInflater.inflate(R.layout.layout_player_picker, null)
        val dialog = Dialog(this).apply { setContentView(dialogView); setCanceledOnTouchOutside(false); setCancelable(false) }
        dialogView.findViewById<TextView>(R.id.txtTitle).text = "Elige ${codeToNice(needRole)}"
        val rvPicker = dialogView.findViewById<RecyclerView>(R.id.rvPickerOptions)
        rvPicker.layoutManager = GridLayoutManager(this, 2)
        rvPicker.adapter = OptionAdapter(
            players = options,
            onClick = { chosen ->
                slots[slotIndex].player = chosen
                dialog.dismiss()
                requestFieldRedraw()
                val total = slots.size
                val filled = slots.count { it.player != null }
                setBenchAccessEnabled(filled == total)
                btnNext.visibility = if (filled == total) View.VISIBLE else View.GONE
            }
        )


        attachPickerPreviewLive(rvPicker, dialog, options) { candidate ->
            val prev = slots[slotIndex].player
            slots[slotIndex].player = candidate
            { slots[slotIndex].player = prev }
        }

        dialog.show()
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.92f).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun showBenchOptionsForSlot(slotIndex: Int) {
        val used = slots.mapNotNull { it.player }.toMutableSet().apply { addAll(benchPlayers.filterNotNull()) }
        val options = availablePlayers.filter { it !in used }.shuffled().take(4)

        val dialogView = layoutInflater.inflate(R.layout.layout_player_picker, null)
        val dialog = Dialog(this).apply { setContentView(dialogView); setCanceledOnTouchOutside(false); setCancelable(false) }
        dialogView.findViewById<TextView>(R.id.txtTitle).text = "Elige Suplente"
        val rvPicker = dialogView.findViewById<RecyclerView>(R.id.rvPickerOptions)
        rvPicker.layoutManager = GridLayoutManager(this, 2)
        rvPicker.adapter = OptionAdapter(
            players = options,
            onClick = { chosen ->
                benchPlayers[slotIndex] = chosen
                findViewById<RecyclerView?>(R.id.rvBenchSelected)?.adapter?.notifyItemChanged(slotIndex)
                updateBenchTitle()
                dialog.dismiss()
            }
        )


        attachPickerPreviewLive(rvPicker, dialog, options) { candidate ->
            val prev = benchPlayers[slotIndex]
            benchPlayers[slotIndex] = candidate
            {
                benchPlayers[slotIndex] = prev
                findViewById<RecyclerView?>(R.id.rvBenchSelected)?.adapter?.notifyItemChanged(slotIndex)
                updateBenchTitle()
            }
        }

        dialog.show()
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.92f).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    // ---------------------- Render de campo + drag unificado ----------------------

    private fun requestFieldRedraw() {
        if (drawPending) return
        drawPending = true
        fieldLayout.post { drawPending = false; drawSlotsInternal() }
    }

    private fun drawSlotsInternal() {
        if (fieldLayout.width == 0 || fieldLayout.height == 0) { fieldLayout.post { drawSlotsInternal() }; return }
        val formationName = selectedFormationName ?: return
        val formation = formations.firstOrNull { it.name == formationName } ?: return

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

        // asegurar vistas
        val totalSlots = slots.size
        while (slotViews.size < totalSlots) {
            val v = layoutInflater.inflate(R.layout.item_player_field, fieldLayout, false)
            slotViews.add(v); fieldLayout.addView(v)
        }
        while (slotViews.size > totalSlots) {
            val last = slotViews.removeAt(slotViews.lastIndex)
            fieldLayout.removeView(last)
        }

        val coords = formationCoordinates[formationName]
        if (coords != null && coords.size == totalSlots) {
            val topBand = 0.06f; val bottomBand = 0.94f
            fun mapY(y: Float) = (topBand + y * (bottomBand - topBand)).coerceIn(0f, 1f)
            slots.forEachIndexed { i, _ ->
                val (x, raw) = coords[i]; val y = mapY(raw)
                val v = slotViews[i]; bindSlotView(v, i)
                val lp = RelativeLayout.LayoutParams(cardW.toInt(), cardH.toInt())
                lp.leftMargin = (fieldLayout.width * x - cardW / 2f).toInt()
                lp.topMargin = (fieldLayout.height * y - cardH / 2f).toInt()
                v.layoutParams = lp
            }
        } else {
            // distribución por filas
            val codes = slots.map { it.role }
            val nPT = codes.count { it == "PT" }
            val nDF = codes.count { it == "DF" }
            val nMC = codes.count { it == "MC" }
            val nDL = codes.count { it == "DL" }

            fun split(c: Int, maxPerRow: Int): List<Int> {
                if (c <= 0) return emptyList()
                val rows = mutableListOf<Int>(); var left = c
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

            var gi = 0
            rowSpec.forEachIndexed { r, cols ->
                val n = cols
                val rowW = n * cardW + (n - 1) * hGap
                val startX = (fieldLayout.width - rowW) / 2f
                val y = yCenters[r]
                repeat(n) { c ->
                    val idx = gi
                    val v = slotViews[idx]; bindSlotView(v, idx)
                    val lp = RelativeLayout.LayoutParams(cardW.toInt(), cardH.toInt())
                    lp.leftMargin = (startX + c * (cardW + hGap)).toInt()
                    lp.topMargin = (y - cardH / 2f).toInt()
                    v.layoutParams = lp
                    gi++
                }
            }
        }

        val filled = slots.count { it.player != null }
        btnNext.visibility = if (filled == slots.size) View.VISIBLE else View.GONE
        setBenchAccessEnabled(filled == slots.size)
    }

    private fun bindSlotView(view: View, index: Int) {
        val img = view.findViewById<ImageView>(R.id.imgPlayer)
        val name = view.findViewById<TextView>(R.id.txtPlayerNickname)
        val elem = view.findViewById<ImageView>(R.id.imgElement)

        val slot = slots[index]
        val p = slot.player

        // Long-press para iniciar drag desde CAMPO (si hay jugador)
        if (p != null) {
            view.setOnLongClickListener {
                val clip = ClipData("field",
                    arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN),
                    ClipData.Item(index.toString()))
                it.startDragAndDrop(clip, View.DragShadowBuilder(it), index, 0)
                true
            }
        } else {
            view.setOnLongClickListener(null)
        }

        // Listener de DROP unificado (campo↔campo y banquillo→campo)
        view.setOnDragListener { v, e ->
            when (e.action) {
                DragEvent.ACTION_DRAG_STARTED -> true // aceptamos y validamos en DROP
                DragEvent.ACTION_DRAG_ENTERED -> { v.alpha = 0.85f; true }
                DragEvent.ACTION_DRAG_EXITED  -> { v.alpha = 1f;    true }
                DragEvent.ACTION_DROP -> {
                    v.alpha = 1f
                    val label = e.clipDescription?.label?.toString()
                    if (label == "field") {
                        // swap CAMPO↔CAMPO (ambos deben existir y poder jugar el rol del otro)
                        val fromIndex = e.localState as? Int ?: return@setOnDragListener false
                        val toIndex = index
                        if (fromIndex == toIndex) return@setOnDragListener false
                        val src = slots.getOrNull(fromIndex)?.player
                        val dst = slots.getOrNull(toIndex)?.player
                        val roleFrom = slots.getOrNull(fromIndex)?.role ?: return@setOnDragListener false
                        val roleTo   = slots.getOrNull(toIndex)?.role   ?: return@setOnDragListener false
                        if (src == null || dst == null) {
                            Toast.makeText(this, "Solo entre jugadores", Toast.LENGTH_SHORT).show(); return@setOnDragListener false
                        }
                        if (!src.canPlay(roleTo) || !dst.canPlay(roleFrom)) {
                            Toast.makeText(this, "No pueden intercambiar sus posiciones", Toast.LENGTH_SHORT).show(); return@setOnDragListener false
                        }
                        slots[fromIndex].player = dst
                        slots[toIndex].player   = src
                        requestFieldRedraw()
                        true
                    } else if (label == "bench") {
                        // entrada desde BANQUILLO -> valida rol y hace swap/mover
                        val bIndex = e.localState as? Int ?: return@setOnDragListener false
                        val incoming = benchPlayers.getOrNull(bIndex) ?: return@setOnDragListener false
                        if (!incoming.canPlay(slot.role)) {
                            Toast.makeText(this, "No puede jugar en ${codeToNice(slot.role)}", Toast.LENGTH_SHORT).show()
                            return@setOnDragListener false
                        }
                        val prev = slots[index].player
                        benchPlayers[bIndex] = prev
                        slots[index].player = incoming
                        pendingBenchIndex = null; pendingFieldIndex = null
                        requestFieldRedraw()
                        findViewById<RecyclerView?>(R.id.rvBenchSelected)?.adapter?.notifyItemChanged(bIndex)
                        updateBenchTitle()
                        true
                    } else false
                }
                DragEvent.ACTION_DRAG_ENDED -> { v.alpha = 1f; true }
                else -> false
            }
        }

        // Binding visual + TAP behaviour (sin auto-avance)
        if (p != null) {
            img.setImageResource(p.image)
            name.text = p.nickname
            elem.setImageResource(p.element)
            view.setOnClickListener {
                // Tap-swap bench↔campo
                pendingBenchIndex?.let { bIndex ->
                    val incoming = benchPlayers[bIndex] ?: return@setOnClickListener
                    if (!incoming.canPlay(slot.role)) {
                        Toast.makeText(this, "No puede jugar en ${codeToNice(slot.role)}", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    benchPlayers[bIndex] = p
                    slots[index].player = incoming
                    pendingBenchIndex = null
                    requestFieldRedraw()
                    findViewById<RecyclerView?>(R.id.rvBenchSelected)?.adapter?.notifyItemChanged(bIndex)
                    updateBenchTitle()
                    return@setOnClickListener
                }
                // Selección de campo para posible swap por tap (opcional)
                pendingFieldIndex = index
                Toast.makeText(this, "Arrastra sobre otro jugador para intercambiar", Toast.LENGTH_SHORT).show()
            }
        } else {
            name.text = codeToNice(slot.role)
            img.setImageResource(0); elem.setImageResource(0); img.background = null
            view.setOnClickListener {
                if (pendingBenchIndex != null) {
                    Toast.makeText(this, "Intercambio solo entre jugadores", Toast.LENGTH_SHORT).show()
                } else showOptionsForSlot(index)
            }
        }
    }

    // ---------------------- Banquillo (drawer) ----------------------

    private fun setupBenchPanel() {
        val root   = findViewById<View?>(R.id.benchPanel) ?: return
        val drawer = findViewById<View?>(R.id.benchDrawer) ?: return
        val scrim  = findViewById<View?>(R.id.benchScrim) ?: return
        val rvSel  = findViewById<RecyclerView?>(R.id.rvBenchSelected) ?: return
        val rvOpts = findViewById<RecyclerView?>(R.id.rvBenchOptions) ?: return
        val txtTitle = findViewById<TextView?>(R.id.txtBenchTitle) ?: return

        drawer.post {
            val w = (resources.displayMetrics.widthPixels * 0.42f).toInt()
            val min = (200 * resources.displayMetrics.density).toInt()
            drawer.layoutParams = drawer.layoutParams.apply { width = max(min, w) }
            drawer.translationX = drawer.width.toFloat()
            root.visibility = View.GONE; scrim.visibility = View.GONE
        }
        scrim.setOnClickListener { closeBench(root, drawer, scrim) }

        // Asa lateral segura
        if (benchHandle == null) {
            val d = resources.displayMetrics.density
            benchHandle = Button(this).apply {
                text = "BANQUILLO"; rotation = -90f; setAllCaps(true)
                setBackgroundColor(0xff_ff_cc_00.toInt())
                setTextColor(0xff_00_00_00.toInt()); elevation = 16f
                setOnClickListener { openBench(root, drawer, scrim) }
            }
            val lp = FrameLayout.LayoutParams((44 * d).toInt(), (120 * d).toInt()).apply {
                gravity = Gravity.END or Gravity.CENTER_VERTICAL; marginEnd = (4 * d).toInt()
            }
            addContentView(benchHandle, lp)
        }
        benchHandle?.visibility = View.GONE

        if (benchHotZone == null) {
            val d = resources.displayMetrics.density
            benchHotZone = View(this).apply { visibility = View.GONE }
            val lp = FrameLayout.LayoutParams((28 * d).toInt(), FrameLayout.LayoutParams.MATCH_PARENT).apply {
                gravity = Gravity.END
            }
            addContentView(benchHotZone, lp)
        }

        rvSel.layoutManager = LinearLayoutManager(this)
        rvSel.itemAnimator = null
        rvSel.adapter = BenchSelectedAdapter(
            benchPlayers = benchPlayers,
            onTapSlot = { index -> handleBenchTap(index) },
            onChanged = { updateBenchTitle() },
            onDropFromField = { benchIndex: Int, fieldIndex: Int ->
                val fieldPlayer = slots.getOrNull(fieldIndex)?.player
                if (fieldPlayer == null) {
                    false
                } else {
                    val prevBench = benchPlayers[benchIndex]
                    benchPlayers[benchIndex] = fieldPlayer
                    slots[fieldIndex].player = prevBench

                    requestFieldRedraw()
                    findViewById<RecyclerView?>(R.id.rvBenchSelected)
                        ?.adapter?.notifyItemChanged(benchIndex)
                    true
                }
            }
        )


        rvOpts.layoutManager = GridLayoutManager(this, 2)
        rvOpts.adapter = OptionAdapter(players = emptyList(), onClick = { })
        txtTitle.text = "Banquillo (${benchSize()}/5)"
    }

    private fun handleBenchTap(index: Int) {
        val benchPlayer = benchPlayers[index]

        pendingFieldIndex?.let { fIndex ->
            val fieldPlayer = slots[fIndex].player ?: run {
                pendingFieldIndex = null
                return
            }
            if (benchPlayer != null) {
                // ✅ validar que el del banquillo puede jugar en el rol del hueco del campo
                val roleCode = slots[fIndex].role
                if (!benchPlayer.canPlay(roleCode)) {
                    Toast.makeText(this, "No puede jugar en ${codeToNice(roleCode)}", Toast.LENGTH_SHORT).show()
                    pendingFieldIndex = null
                    return
                }
                benchPlayers[index] = fieldPlayer
                slots[fIndex].player = benchPlayer
                pendingFieldIndex = null; pendingBenchIndex = null
                requestFieldRedraw()
                findViewById<RecyclerView?>(R.id.rvBenchSelected)?.adapter?.notifyItemChanged(index)
                updateBenchTitle()
                return
            } else {
                pendingFieldIndex = null; pendingBenchIndex = null
                showBenchOptionsForSlot(index)
                return
            }
        }


        pendingBenchIndex?.let {
            if (benchPlayer == null) { pendingBenchIndex = null; showBenchOptionsForSlot(index) }
            else pendingBenchIndex = index
            return
        }

        if (benchPlayer != null) {
            pendingBenchIndex = index
            Toast.makeText(this, "Toca un jugador del campo o arrástralo", Toast.LENGTH_SHORT).show()
        } else showBenchOptionsForSlot(index)
    }

    private fun openBench(root: View, drawer: View, scrim: View) {
        root.visibility = View.VISIBLE; root.bringToFront()
        scrim.visibility = View.VISIBLE
        drawer.animate().translationX(0f).setDuration(220).start()
        isBenchOpen = true
    }
    private fun closeBench(root: View, drawer: View, scrim: View) {
        drawer.animate().translationX(drawer.width.toFloat()).setDuration(200)
            .withEndAction { scrim.visibility = View.GONE; root.visibility = View.GONE }.start()
        isBenchOpen = false
    }

    private fun setBenchAccessEnabled(enabled: Boolean) {
        benchHandle?.visibility = if (enabled) View.VISIBLE else View.GONE
        benchHotZone?.visibility = if (enabled) View.VISIBLE else View.GONE
    }
    private fun updateBenchTitle() {
        findViewById<TextView?>(R.id.txtBenchTitle)?.text = "Banquillo (${benchSize()}/5)"
    }
    private fun benchSize(): Int = benchPlayers.count { it != null }

    // ---------------------- Helpers preview ----------------------

    private fun startLivePreview(dialog: Dialog, onClose: () -> Unit) {
        dialog.hide()
        val root = findViewById<ViewGroup>(android.R.id.content)
        val overlay = FrameLayout(this).apply {
            setBackgroundColor(0x66000000); isClickable = true; isFocusable = true
            val btn = MaterialButton(context).apply {
                text = "VOLVER AL PICK"; setOnClickListener {
                onClose(); root.removeView(this@apply); dialog.show()
            }
            }
            addView(btn, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            ).apply { bottomMargin = (16 * resources.displayMetrics.density).toInt() })
            setOnClickListener { onClose(); root.removeView(this); dialog.show() }
        }
        root.addView(overlay, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        requestFieldRedraw()
    }

    private fun attachPickerPreviewLive(
        rv: RecyclerView,
        dialog: Dialog,
        options: List<Player>,
        applyBlock: (candidate: Player) -> (() -> Unit)
    ) {
        val detector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) {
                val child = rv.findChildViewUnder(e.x, e.y) ?: return
                val pos = rv.getChildAdapterPosition(child)
                if (pos != RecyclerView.NO_POSITION && pos < options.size) {
                    val candidate = options[pos]
                    val revert = applyBlock(candidate)
                    startLivePreview(dialog) { revert() }
                }
            }
        })
        rv.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                detector.onTouchEvent(e); return false
            }
            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        })
    }

    private fun toCode(pos: String): String = when (pos.trim().lowercase()) {
        "portero", "pt" -> "PT"
        "defensa", "df" -> "DF"
        "centrocampista", "mc" -> "MC"
        "delantero", "dl" -> "DL"
        else -> pos.trim().uppercase()
    }
    internal fun codeToNice(code: String): String = when (code.uppercase()) {
        "PT" -> "Portero"; "DF" -> "Defensa"; "MC" -> "Centrocampista"; "DL" -> "Delantero"; else -> code
    }
}
