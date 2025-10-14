package com.inazumadraft.ui

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.DragEvent
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
import com.inazumadraft.ui.adapters.BenchSelectedAdapter
import com.inazumadraft.ui.adapters.OptionAdapter
import com.inazumadraft.data.PlayerRepository
import com.inazumadraft.data.formationCoordinates
import com.inazumadraft.data.formations
import com.inazumadraft.model.Player
import com.inazumadraft.model.canPlay
import kotlin.math.max

class DraftActivity : AppCompatActivity() {

    // UI principales
    private lateinit var fieldLayout: RelativeLayout
    private lateinit var btnNext: Button
    private lateinit var roundTitle: TextView
    private lateinit var formationOverlay: View

    // Botones de formación
    private lateinit var btnFormation1: Button
    private lateinit var btnFormation2: Button
    private lateinit var btnFormation3: Button
    private lateinit var btnFormation4: Button

    // Estado formación
    private var selectedFormationName: String? = null
    private var formationLocked = false

    // Slots de campo
    data class Slot(val role: String, var player: Player?)
    private val slots = mutableListOf<Slot>()
    private val slotViews: MutableList<View> = mutableListOf()
    private var drawPending = false

    // Banquillo
    private val benchPlayers = MutableList<Player?>(5) { null }
    private var isBenchOpen = false
    private var benchHotZone: View? = null
    private var benchHandle: View? = null

    // Selecciones para TAP-swap
    private var pendingBenchIndex: Int? = null
    private var pendingFieldIndex: Int? = null

    // Capitán
    private var captain: Player? = null

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

        // Rellenamos nombres de formaciones
        val visible = formations.take(4)
        btnFormation1.text = visible.getOrNull(0)?.name ?: "F1"
        btnFormation2.text = visible.getOrNull(1)?.name ?: "F2"
        btnFormation3.text = visible.getOrNull(2)?.name ?: "F3"
        btnFormation4.text = visible.getOrNull(3)?.name ?: "F4"

        btnFormation1.setOnClickListener { selectFormationByIndex(0) }
        btnFormation2.setOnClickListener { selectFormationByIndex(1) }
        btnFormation3.setOnClickListener { selectFormationByIndex(2) }
        btnFormation4.setOnClickListener { selectFormationByIndex(3) }

        btnNext.setOnClickListener {
            val filled = slots.count { it.player != null }
            if (filled != slots.size) {
                Toast.makeText(this, "Completa el 11 inicial", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = android.content.Intent(this, FinalTeamActivity::class.java).apply {
                putParcelableArrayListExtra("finalTeam", ArrayList(slots.mapNotNull { it.player }))
                putParcelableArrayListExtra("benchPlayers", ArrayList(benchPlayers.filterNotNull()))
                putExtra("formation", selectedFormationName ?: "4-4-2")
                putExtra("captainName", captain?.name)
            }
            startActivity(intent)
        }

        setupBenchPanel()
        setBenchAccessEnabled(false) // hasta completar 11
    }

    // ---------------- Formación y capitán ----------------

    private fun selectFormationByIndex(i: Int) {
        if (formationLocked) return
        val f = formations.getOrNull(i) ?: return
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
        val options = PlayerRepository.players.shuffled().take(4)

        val dialogView = layoutInflater.inflate(R.layout.layout_player_picker, null)
        val dialog = Dialog(this).apply {
            setContentView(dialogView)
            setCanceledOnTouchOutside(false)
            setCancelable(false)
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
                // ❌ sin auto-abrir siguientes picks
            }
        )

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun placeCaptainInPrimaryPositionOnly() {
        val cap = captain ?: return
        val idx = slots.indexOfFirst { it.player == null && it.role.equals(cap.position, ignoreCase = true) }
        if (idx >= 0) slots[idx].player = cap
    }

    // ---------------- Player-picks por rol (solo posición principal) ----------------

    private fun showOptionsForSlot(slotIndex: Int) {
        val used = slots.mapNotNull { it.player }.toMutableSet().apply { addAll(benchPlayers.filterNotNull()) }
        val needRole = slots[slotIndex].role

        val pool = PlayerRepository.players
            .filter { it !in used && it.position.equals(needRole, ignoreCase = true) }
            .shuffled()

        val options = pool.take(4)

        val dialogView = layoutInflater.inflate(R.layout.layout_player_picker, null)
        val dialog = Dialog(this).apply {
            setContentView(dialogView)
            setCanceledOnTouchOutside(false)
            setCancelable(false)
        }

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

                // ❌ sin auto-abrir el siguiente
            }
        )

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun showBenchOptionsForSlot(slotIndex: Int) {
        val used = slots.mapNotNull { it.player }.toMutableSet().apply { addAll(benchPlayers.filterNotNull()) }
        val options = PlayerRepository.players.filter { it !in used }.shuffled().take(4)

        val dialogView = layoutInflater.inflate(R.layout.layout_player_picker, null)
        val dialog = Dialog(this).apply {
            setContentView(dialogView)
            setCanceledOnTouchOutside(false)
            setCancelable(false)
        }

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

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    // ---------------- Render del campo + TAP swap ----------------

    private fun requestFieldRedraw() {
        if (drawPending) return
        drawPending = true
        fieldLayout.post {
            drawPending = false
            drawSlotsInternal()
        }
    }

    private fun drawSlotsInternal() {
        if (fieldLayout.width == 0 || fieldLayout.height == 0) {
            fieldLayout.post { drawSlotsInternal() }
            return
        }
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

        val coords = formationCoordinates[formationName]
        if (coords != null && coords.size == totalSlots) {
            val topBand = 0.06f
            val bottomBand = 0.94f
            fun mapY(y: Float) = (topBand + y * (bottomBand - topBand)).coerceIn(0f, 1f)

            slots.forEachIndexed { i, _ ->
                val (x, yRaw) = coords[i]
                val y = mapY(yRaw)
                val view = slotViews[i]
                bindSlotView(view, i)

                val lp = RelativeLayout.LayoutParams(cardW.toInt(), cardH.toInt())
                lp.leftMargin = (fieldLayout.width * x - cardW / 2f).toInt()
                lp.topMargin = (fieldLayout.height * y - cardH / 2f).toInt()
                view.layoutParams = lp
            }
        } else {
            // Fallback por filas
            val codes = slots.map { it.role }
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

                    globalIndex++
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

        view.setOnClickListener(null)

        val slot = slots[index]
        val p = slot.player

        view.setOnDragListener { v, e ->
            when (e.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    // Acepta solo si el drag viene del banquillo
                    e.clipDescription?.label == "bench"
                }
                DragEvent.ACTION_DROP -> {
                    val bIndex = e.localState as? Int ?: return@setOnDragListener false
                    val incoming = benchPlayers.getOrNull(bIndex) ?: return@setOnDragListener false
                    if (!incoming.canPlay(slot.role)) {
                        Toast.makeText(this, "No puede jugar en ${codeToNice(slot.role)}", Toast.LENGTH_SHORT).show()
                        return@setOnDragListener false
                    }
                    // swap o mover
                    benchPlayers[bIndex] = slot.player
                    slots[index].player = incoming
                    pendingBenchIndex = null
                    pendingFieldIndex = null
                    requestFieldRedraw()
                    findViewById<RecyclerView?>(R.id.rvBenchSelected)?.adapter?.notifyItemChanged(bIndex)
                    updateBenchTitle()
                    true
                }
                else -> false
            }
        }
        if (p != null) {
            img.setImageResource(p.image)
            name.text = p.nickname
            elem.setImageResource(p.element)

            // TAP SWAP (campo ocupado)
            view.setOnClickListener {
                // si hay un banquillo seleccionado, intentar swap (ambos deben tener jugador)
                pendingBenchIndex?.let { bIndex ->
                    val incoming = benchPlayers[bIndex] ?: run {
                        pendingBenchIndex = null
                        return@setOnClickListener
                    }
                    if (!incoming.canPlay(slot.role)) {
                        Toast.makeText(this, "No puede jugar en ${codeToNice(slot.role)}", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    // swap jugador↔jugador (no con vacíos)
                    benchPlayers[bIndex] = p
                    slots[index].player = incoming
                    pendingBenchIndex = null
                    requestFieldRedraw()
                    findViewById<RecyclerView?>(R.id.rvBenchSelected)?.adapter?.notifyItemChanged(bIndex)
                    updateBenchTitle()
                } ?: run {
                    // marcar este jugador de campo para swap
                    pendingFieldIndex = index
                    Toast.makeText(this, "Toca un jugador del banquillo para intercambiar", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            name.text = codeToNice(slot.role)
            img.setImageResource(0)
            elem.setImageResource(0)
            img.background = null

            // Campo vacío: no permite swap con banquillo seleccionado (ambos deben ser jugadores)
            view.setOnClickListener {
                if (pendingBenchIndex != null) {
                    Toast.makeText(this, "El intercambio debe ser entre dos jugadores", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                // flujo normal: abrir picker del rol
                showOptionsForSlot(index)
            }
        }
    }

    // ---------------- Banquillo (drawer) y helpers ----------------
    private fun openBench(root: View, drawer: View, scrim: View) {
        root.visibility = View.VISIBLE
        root.bringToFront()
        scrim.visibility = View.VISIBLE
        drawer.animate().translationX(0f).setDuration(220).start()
        isBenchOpen = true
    }

    private fun closeBench(root: View, drawer: View, scrim: View) {
        drawer.animate().translationX(drawer.width.toFloat()).setDuration(200)
            .withEndAction {
                scrim.visibility = View.GONE
                root.visibility = View.GONE
            }.start()
        isBenchOpen = false
    }
    private fun setupBenchPanel() {
        val root   = findViewById<View?>(R.id.benchPanel) ?: return
        val drawer = findViewById<View?>(R.id.benchDrawer) ?: return
        val scrim  = findViewById<View?>(R.id.benchScrim) ?: return
        val rvSel  = findViewById<RecyclerView?>(R.id.rvBenchSelected) ?: return
        val rvOpts = findViewById<RecyclerView?>(R.id.rvBenchOptions) ?: return
        val txtTitle = findViewById<TextView?>(R.id.txtBenchTitle) ?: return

        // Configura ancho del drawer y déjalo cerrado
        drawer.post {
            val w = (resources.displayMetrics.widthPixels * 0.42f).toInt()
            val min = (200 * resources.displayMetrics.density).toInt()
            drawer.layoutParams = drawer.layoutParams.apply { width = kotlin.math.max(min, w) }
            drawer.translationX = drawer.width.toFloat()
            root.visibility = View.GONE
            scrim.visibility = View.GONE
        }
        scrim.setOnClickListener { closeBench(root, drawer, scrim) }

        // ---------- Asa lateral (usamos addContentView para evitar NPE) ----------
        if (benchHandle == null) {
            val d = resources.displayMetrics.density
            benchHandle = Button(this).apply {
                text = "BANQUILLO"
                rotation = -90f
                setAllCaps(true)
                setBackgroundColor(0xff_ff_cc_00.toInt())
                setTextColor(0xff_00_00_00.toInt())
                elevation = 16f
                tag = "bench_handle_draft"
                setOnClickListener { openBench(root, drawer, scrim) }
            }
            val handleLp = FrameLayout.LayoutParams((44 * d).toInt(), (120 * d).toInt()).apply {
                gravity = android.view.Gravity.END or android.view.Gravity.CENTER_VERTICAL
                marginEnd = (4 * d).toInt()
            }
            // <- en lugar de parent.addView(...), usamos addContentView(...)
            addContentView(benchHandle, handleLp)
        }
        // El banquillo solo se habilita cuando está el 11 completo; por defecto oculto
        benchHandle?.visibility = View.GONE

        // ---------- Hot-zone para abrir con gesto (también usando addContentView) ----------
        if (benchHotZone == null) {
            val d = resources.displayMetrics.density
            benchHotZone = View(this).apply {
                setOnDragListener { _, e ->
                    if (e.action == android.view.DragEvent.ACTION_DRAG_ENTERED && !isBenchOpen) {
                        openBench(root, drawer, scrim); true
                    } else false
                }
                visibility = View.GONE // se habilita cuando se complete el 11
            }
            val hzLp = FrameLayout.LayoutParams((28 * d).toInt(), FrameLayout.LayoutParams.MATCH_PARENT).apply {
                gravity = android.view.Gravity.END
            }
            addContentView(benchHotZone, hzLp)
        }

        // ---------- Lista de seleccionados del banquillo ----------
        rvSel.layoutManager = LinearLayoutManager(this)
        rvSel.itemAnimator = null
        rvSel.adapter = BenchSelectedAdapter(
            benchPlayers = benchPlayers,
            onTapSlot = { index -> handleBenchTap(index) },
            onChanged = { updateBenchTitle() }
        )

        // ---------- Lista de opciones (inicialmente vacía) ----------
        rvOpts.layoutManager = GridLayoutManager(this, 2)
        rvOpts.adapter = OptionAdapter(players = emptyList(), onClick = { /* no-op */ })

        // Título inicial
        txtTitle.text = "Banquillo (${benchSize()}/5)"
    }


    private fun handleBenchTap(index: Int) {
        val benchPlayer = benchPlayers[index]

        // 1) Si hay un CAMPO seleccionado y pulsas un banquillo con jugador → SWAP
        pendingFieldIndex?.let { fIndex ->
            val fieldPlayer = slots[fIndex].player
            if (fieldPlayer == null) {
                Toast.makeText(this, "Selecciona primero un jugador del campo", Toast.LENGTH_SHORT).show()
                pendingFieldIndex = null
                return
            }
            if (benchPlayer != null) {
                // SWAP jugador↔jugador
                benchPlayers[index] = fieldPlayer
                slots[fIndex].player = benchPlayer
                pendingFieldIndex = null
                pendingBenchIndex = null
                requestFieldRedraw()
                findViewById<RecyclerView?>(R.id.rvBenchSelected)?.adapter?.notifyItemChanged(index)
                updateBenchTitle()
                return
            } else {
                // ✅ Si el hueco está vacío: cancelar selección y abrir pick de suplentes
                pendingFieldIndex = null
                pendingBenchIndex = null
                showBenchOptionsForSlot(index)
                return
            }
        }

        // 2) Si hay un BANQUILLO seleccionado y tocas otro banquillo:
        pendingBenchIndex?.let { bIndex ->
            if (benchPlayer == null) {
                // ✅ Vacío → cancelar selección y abrir pick
                pendingBenchIndex = null
                showBenchOptionsForSlot(index)
                return
            } else {
                // Mismo patrón: permitir mover la selección a otro banquillo con jugador (opcional),
                // aquí simplemente cambiamos la selección al nuevo índice.
                pendingBenchIndex = index
                return
            }
        }

        // 3) Sin selecciones previas:
        if (benchPlayer != null) {
            // marcar selección para swap con CAMPO
            pendingBenchIndex = index
            Toast.makeText(this, "Toca un jugador del campo para intercambiar", Toast.LENGTH_SHORT).show()
        } else {
            // ✅ Hueco vacío → abrir pick de suplentes
            showBenchOptionsForSlot(index)
        }
    }

    private fun setBenchAccessEnabled(enabled: Boolean) {
        benchHandle?.visibility = if (enabled) View.VISIBLE else View.GONE
        benchHotZone?.visibility = if (enabled) View.VISIBLE else View.GONE
    }

    private fun updateBenchTitle() {
        findViewById<TextView?>(R.id.txtBenchTitle)?.text = "Banquillo (${benchSize()}/5)"
    }

    private fun benchSize(): Int = benchPlayers.count { it != null }

    // ---------------- Helpers ----------------

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
}
