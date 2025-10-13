package com.inazumadraft.ui

import android.app.Dialog
import android.content.ClipData
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
import com.inazumadraft.R
import com.inazumadraft.data.PlayerRepository
import com.inazumadraft.data.formations
import com.inazumadraft.data.formationCoordinates
import com.inazumadraft.model.Player
import com.inazumadraft.model.canPlay
import com.inazumadraft.ui.adapters.OptionAdapter
import com.inazumadraft.ui.adapters.BenchSelectedAdapter
import kotlin.math.max

class DraftActivity : AppCompatActivity() {

    // UI
    private lateinit var fieldLayout: RelativeLayout
    private lateinit var rvOptions: RecyclerView
    private lateinit var btnNext: Button
    private lateinit var roundTitle: TextView
    private lateinit var overlayPreview: View
    private lateinit var formationOverlay: View

    // Formación
    private lateinit var btnFormation1: Button
    private lateinit var btnFormation2: Button
    private lateinit var btnFormation3: Button
    private lateinit var btnFormation4: Button
    private var selectedFormationName: String? = null
    private var formationLocked = false

    // Slots de campo
    data class Slot(val role: String, var player: Player?)
    private val slots = mutableListOf<Slot>()
    private val slotViews: MutableList<View> = mutableListOf()
    private var drawPending = false

    // Banquillo
    private var pendingBenchIndex: Int? = null
    private var pendingFieldIndex: Int? = null
    private val benchPlayers = MutableList<Player?>(5) { null }
    private var isBenchOpen = false
    private var benchHotZone: View? = null
    private var benchHandle: View? = null

    // Estado
    private var captain: Player? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_draft)

        // Views
        fieldLayout = findViewById(R.id.fieldLayout)
        rvOptions = findViewById(R.id.rvOptions)
        btnNext = findViewById(R.id.btnNext)
        roundTitle = findViewById(R.id.roundTitle)
        overlayPreview = findViewById(R.id.overlayPreview)
        formationOverlay = findViewById(R.id.formationOverlay)

        btnFormation1 = findViewById(R.id.btnFormation1)
        btnFormation2 = findViewById(R.id.btnFormation2)
        btnFormation3 = findViewById(R.id.btnFormation3)
        btnFormation4 = findViewById(R.id.btnFormation4)

        // Botones de formación (elige y pasa a capitán)
        val visible = formations.take(4)
        btnFormation1.text = visible.getOrNull(0)?.name ?: "F1"
        btnFormation2.text = visible.getOrNull(1)?.name ?: "F2"
        btnFormation3.text = visible.getOrNull(2)?.name ?: "F3"
        btnFormation4.text = visible.getOrNull(3)?.name ?: "F4"

        btnFormation1.setOnClickListener { selectFormationByIndex(0) }
        btnFormation2.setOnClickListener { selectFormationByIndex(1) }
        btnFormation3.setOnClickListener { selectFormationByIndex(2) }
        btnFormation4.setOnClickListener { selectFormationByIndex(3) }

        // Confirmar equipo → abre pantalla final
        btnNext.setOnClickListener {
            val filled = slots.count { it.player != null }
            val total = slots.size
            if (filled != total) {
                Toast.makeText(this, "Completa el 11 inicial", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val i = android.content.Intent(this, FinalTeamActivity::class.java).apply {
                putParcelableArrayListExtra(
                    "finalTeam",
                    ArrayList(slots.mapNotNull { it.player })
                )
                putParcelableArrayListExtra(
                    "benchPlayers",
                    ArrayList(benchPlayers.filterNotNull())
                )
                putExtra("formation", selectedFormationName ?: "4-4-2")
                putExtra("captainName", captain?.name)
            }
            startActivity(i)
        }

        // Drawer del banquillo
        setupBenchPanel()

        // Al iniciar NO se permite abrir banquillo hasta completar 11
        setBenchAccessEnabled(false)
    }

    // ---------------- Formación y capitán ----------------

    private fun selectFormationByIndex(i: Int) {
        if (formationLocked) return
        val f = formations.getOrNull(i) ?: return
        selectedFormationName = f.name
        formationLocked = true

        // Oculta overlay de selección
        findViewById<View>(R.id.formationButtonsLayout)?.visibility = View.GONE
        formationOverlay.visibility = View.GONE

        // Prepara slots
        slots.clear()
        slots.addAll(f.positions.map { Slot(toCode(it), null) })
        slotViews.clear()
        fieldLayout.removeAllViews()

        // Pide capitán
        roundTitle.text = "Elige tu capitán (${f.name})"
        showCaptainOptions()
    }

    private fun showCaptainOptions() {
        val options = PlayerRepository.players.shuffled().take(4)
        rvOptions.visibility = View.GONE

        val dialogView = layoutInflater.inflate(R.layout.layout_player_picker, null)
        val dialog = Dialog(this)
        dialog.setContentView(dialogView)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)

        dialogView.findViewById<TextView>(R.id.txtTitle).text = "Elige Capitán"
        val rvPicker = dialogView.findViewById<RecyclerView>(R.id.rvPickerOptions)
        rvPicker.layoutManager = GridLayoutManager(this, 2)
        rvPicker.adapter = OptionAdapter(
            players = options,
            onClick = { chosen ->
                captain = chosen

                placeCaptainInPrimaryPositionOnly()
                dialog.dismiss()

                // ✅ Ahora solo pintamos el campo y dejamos que el usuario pulse un hueco vacío
                roundTitle.text = "Completa tu 11 inicial"
                requestFieldRedraw()
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
        if (idx >= 0) {
            slots[idx].player = cap
        }
    }
    // ---------------- Picks y diálogos ----------------
    private fun showOptionsForSlot(slotIndex: Int) {
        val used = slots.mapNotNull { it.player }.toMutableSet().apply { addAll(benchPlayers.filterNotNull()) }
        val needRole = slots[slotIndex].role
        // Filtra por rol (pueden ser posiciones secundarias; si quieres ignorarlas, compara solo con principal)
        val pool = PlayerRepository.players.filter { it !in used && it.canPlay(needRole) }.shuffled()
        val options = if (pool.size >= 4) pool.take(4) else pool + PlayerRepository.players.filter { it !in used && it.canPlay(needRole) }.shuffled().take(4 - pool.size)

        val dialogView = layoutInflater.inflate(R.layout.layout_player_picker, null)
        val dialog = Dialog(this)
        dialog.setContentView(dialogView)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)

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
            // onPreview = null
        )


        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun updateBenchTitle() {
        findViewById<TextView?>(R.id.txtBenchTitle)
            ?.text = "Banquillo (${benchSize()}/5)"
    }

    private fun showBenchOptionsForSlot(slotIndex: Int) {
        // Picks aleatorios de 4, sin reroll; posición no importa
        val used = slots.mapNotNull { it.player }.toMutableSet().apply { addAll(benchPlayers.filterNotNull()) }
        val options = PlayerRepository.players.filter { it !in used }.shuffled().take(4)

        val dialogView = layoutInflater.inflate(R.layout.layout_player_picker, null)
        val dialog = Dialog(this)
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
                updateBenchTitle()
                dialog.dismiss()
            }
            // onPreview = null
        )


        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    // ---------------- Dibujo del campo ----------------

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

        // Ajustes de tamaño/espaciado
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
            // Mapeo con bandas más abiertas
            val topBand = 0.06f
            val bottomBand = 0.94f
            fun mapY(y: Float) = (topBand + y * (bottomBand - topBand)).coerceIn(0f, 1f)

            slots.forEachIndexed { i, slot ->
                val (x, yRaw) = coords[i]
                val y = mapY(yRaw)
                val view = slotViews[i]
                bindSlotView(view, i)

                val lp = RelativeLayout.LayoutParams(cardW.toInt(), cardH.toInt())
                lp.leftMargin = (fieldLayout.width * x - cardW / 2f).toInt()
                lp.topMargin = (fieldLayout.height * y - cardH / 2f).toInt()
                view.layoutParams = lp

                attachDragLogicToFieldView(view, i)
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

                    attachDragLogicToFieldView(view, index)
                    globalIndex++
                }
            }
        }

        // Estado de next/bench access
        val total = slots.size
        val filled = slots.count { it.player != null }
        btnNext.visibility = if (filled == total) View.VISIBLE else View.GONE
        setBenchAccessEnabled(filled == total)
    }

    private fun bindSlotView(view: View, index: Int) {
        val img = view.findViewById<ImageView>(R.id.imgPlayer)
        val name = view.findViewById<TextView>(R.id.txtPlayerNickname)
        val elem = view.findViewById<ImageView>(R.id.imgElement)
        val slot = slots[index]
        val p = slot.player

        // Limpia listeners
        view.setOnClickListener(null)

        if (p != null) {
            img.setImageResource(p.image)
            name.text = p.nickname
            elem.setImageResource(p.element)

            // TAP EN CAMPO CON JUGADOR
            view.setOnClickListener {
                // a) Si hay un banquillo seleccionado → intentar ponerlo aquí (validando posición)
                pendingBenchIndex?.let { bIndex ->
                    val incoming = benchPlayers[bIndex] ?: return@setOnClickListener
                    if (!incoming.canPlay(slot.role)) {
                        Toast.makeText(this, "No puede jugar en ${codeToNice(slot.role)}", Toast.LENGTH_SHORT).show()
                        pendingBenchIndex = null
                        return@setOnClickListener
                    }
                    // swap bench→field
                    benchPlayers[bIndex] = slot.player
                    slots[index].player = incoming
                    pendingBenchIndex = null
                    requestFieldRedraw()
                    findViewById<RecyclerView?>(R.id.rvBenchSelected)?.adapter?.notifyItemChanged(bIndex)
                    updateBenchTitle()
                    return@setOnClickListener
                }

                // b) Si NO hay bench seleccionado → el usuario quiere sacar este jugador al banquillo
                pendingFieldIndex = index
                Toast.makeText(this, "Elige un hueco del banquillo para mover a ${p.nickname}", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Slot vacío → texto del rol y opción de abrir picker (si no hay bench seleccionado)
            name.text = codeToNice(slot.role)
            img.setImageResource(0)
            elem.setImageResource(0)
            img.background = null

            view.setOnClickListener {
                if (pendingBenchIndex != null) {
                    // Colocar el seleccionado del banquillo aquí
                    val bIndex = pendingBenchIndex!!
                    val incoming = benchPlayers[bIndex] ?: return@setOnClickListener
                    if (!incoming.canPlay(slot.role)) {
                        Toast.makeText(this, "No puede jugar en ${codeToNice(slot.role)}", Toast.LENGTH_SHORT).show()
                        pendingBenchIndex = null
                        return@setOnClickListener
                    }
                    benchPlayers[bIndex] = null
                    slots[index].player = incoming
                    pendingBenchIndex = null
                    requestFieldRedraw()
                    findViewById<RecyclerView?>(R.id.rvBenchSelected)?.adapter?.notifyItemChanged(bIndex)
                    updateBenchTitle()
                } else {
                    // comportamiento normal: abrir picker del rol
                    showOptionsForSlot(index)
                }
            }
        }
    }


    private fun attachDragLogicToFieldView(view: View, index: Int) {
        val slot = slots[index]
        val p = slot.player

        // Drag desde CAMPO → label "fromIndex"
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

        // Drop: desde BANQUILLO o desde otro slot del CAMPO
        view.setOnDragListener { v, e ->
            when (e.action) {
                DragEvent.ACTION_DRAG_STARTED -> true
                DragEvent.ACTION_DRAG_ENTERED -> { v.alpha = 0.7f; true }
                DragEvent.ACTION_DRAG_EXITED  -> { v.alpha = 1f; true }
                DragEvent.ACTION_DROP -> {
                    if (e.clipDescription?.label == "benchPlayer") {
                        val benchIdx = e.localState as? Int ?: return@setOnDragListener true
                        val incoming = benchPlayers.getOrNull(benchIdx) ?: return@setOnDragListener true
                        // Validar que puede jugar en el rol del slot
                        if (!incoming.canPlay(slot.role)) { shakeView(v); return@setOnDragListener true }
                        val out = slots[index].player
                        slots[index].player = incoming
                        benchPlayers[benchIdx] = out
                        findViewById<RecyclerView?>(R.id.rvBenchSelected)?.adapter?.notifyItemChanged(benchIdx)
                        requestFieldRedraw()
                    } else if (e.clipDescription?.label == "fromIndex") {
                        val fromIndex = e.localState as? Int ?: return@setOnDragListener true
                        if (fromIndex != index) {
                            val src = slots[fromIndex].player
                            val dst = slots[index].player
                            val okSrc = src?.canPlay(slot.role) ?: true
                            val okDst = dst?.canPlay(slots[fromIndex].role) ?: true
                            if (okSrc && okDst) {
                                slots[fromIndex].player = dst
                                slots[index].player = src
                                requestFieldRedraw()
                            } else shakeView(v)
                        }
                    }
                    true
                }
                DragEvent.ACTION_DRAG_ENDED -> { v.alpha = 1f; true }
                else -> false
            }
        }
    }

    // ---------------- Banquillo (drawer) ----------------

    private fun setupBenchPanel() {
        val root   = findViewById<View?>(R.id.benchPanel) ?: return
        val drawer = findViewById<View?>(R.id.benchDrawer) ?: return
        val scrim  = findViewById<View?>(R.id.benchScrim) ?: return
        val rvSel  = findViewById<RecyclerView?>(R.id.rvBenchSelected) ?: return
        val rvOpts = findViewById<RecyclerView?>(R.id.rvBenchOptions) ?: return
        val txtTitle = findViewById<TextView?>(R.id.txtBenchTitle) ?: return

        fun updateTitle() { txtTitle.text = "Banquillo (${benchSize()}/5)" }

        // Ajusta ancho dinámico (máx 42% / min 200dp) y deja cerrado
        drawer.post {
            val w = (resources.displayMetrics.widthPixels * 0.42f).toInt()
            val min = (200 * resources.displayMetrics.density).toInt()
            drawer.layoutParams = drawer.layoutParams.apply { width = max(min, w) }
            drawer.translationX = drawer.width.toFloat()
            root.visibility = View.GONE
            scrim.visibility = View.GONE
        }
        scrim.setOnClickListener { closeBench(root, drawer, scrim) }

        // Asa oculta inicialmente; se muestra al completar 11
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
                layoutParams = FrameLayout.LayoutParams((44 * d).toInt(), (120 * d).toInt()).apply {
                    gravity = Gravity.END or Gravity.CENTER_VERTICAL
                    marginEnd = (4 * d).toInt()
                }
            }
            (findViewById<ViewGroup>(android.R.id.content)).addView(benchHandle)
        }
        benchHandle?.visibility = View.GONE

        // Hot-zone para abrir con drag
        ensureBenchHotZone(root, drawer, scrim)

        // Lista seleccionada (vertical, 5 huecos)
        rvSel.layoutManager = LinearLayoutManager(this)
        rvSel.itemAnimator = null
        rvSel.adapter = BenchSelectedAdapter(
            benchPlayers = benchPlayers,
            onTapSlot = { index -> handleBenchTap(index) },
            onChanged = { updateBenchTitle() }
        )


        // Opciones ocultas
        rvOpts.layoutManager = GridLayoutManager(this, 2)
        rvOpts.adapter = OptionAdapter(
            players = emptyList(),
            onClick = { /* no-op */ }
            // onPreview = null  // ← solo si tu OptionAdapter lo tiene
        )

        updateTitle()
    }
    private fun handleBenchTap(index: Int) {
        val benchPlayer = benchPlayers[index]

        // 1) Si hay un campo previamente seleccionado → swap campo→banquillo
        pendingFieldIndex?.let { fIndex ->
            val fieldPlayer = slots[fIndex].player
            // Si el banquillo tiene jugador, swap; si está vacío, mover
            benchPlayers[index] = fieldPlayer
            slots[fIndex].player = benchPlayer
            pendingFieldIndex = null
            pendingBenchIndex = null
            requestFieldRedraw()
            findViewById<RecyclerView?>(R.id.rvBenchSelected)?.adapter?.notifyItemChanged(index)
            updateBenchTitle()
            return
        }

        // 2) Si pulsas un jugador de banquillo → espera toque en campo para ponerlo
        if (benchPlayer != null) {
            pendingBenchIndex = index
            Toast.makeText(this, "Elige un hueco del campo para colocar a ${benchPlayer.nickname}", Toast.LENGTH_SHORT).show()
            return
        }

        // 3) Si pulsas un hueco vacío y no hay selección previa → abre picker de suplentes
        showBenchOptionsForSlot(index)
    }

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

    private fun ensureBenchHotZone(root: View, drawer: View, scrim: View) {
        if (benchHotZone != null) return
        val d = resources.displayMetrics.density
        benchHotZone = View(this).apply {
            layoutParams = FrameLayout.LayoutParams((28 * d).toInt(), FrameLayout.LayoutParams.MATCH_PARENT).apply {
                gravity = Gravity.END
            }
            setOnDragListener { _, e ->
                if (e.action == DragEvent.ACTION_DRAG_ENTERED && !isBenchOpen) {
                    openBench(root, drawer, scrim); true
                } else false
            }
            visibility = View.GONE // al inicio deshabilitado
        }
        (findViewById<ViewGroup>(android.R.id.content)).addView(benchHotZone)
    }

    private fun setBenchAccessEnabled(enabled: Boolean) {
        benchHandle?.visibility = if (enabled) View.VISIBLE else View.GONE
        benchHotZone?.visibility = if (enabled) View.VISIBLE else View.GONE
    }

    private fun benchSize(): Int = benchPlayers.count { it != null }

    private fun shakeView(v: View) {
        v.animate().translationX(12f).setDuration(40).withEndAction {
            v.animate().translationX(-10f).setDuration(40).withEndAction {
                v.animate().translationX(0f).setDuration(40).start()
            }.start()
        }.start()
    }

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
