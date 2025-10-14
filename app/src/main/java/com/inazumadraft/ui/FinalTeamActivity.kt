package com.inazumadraft.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.DragEvent
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
import com.inazumadraft.ui.adapters.BenchSelectedAdapter
import com.inazumadraft.ui.adapters.FinalTeamAdapter
import com.inazumadraft.ui.adapters.OptionAdapter
import com.inazumadraft.data.PlayerRepository
import com.inazumadraft.data.formationCoordinates
import com.inazumadraft.data.formations
import com.inazumadraft.model.Player
import com.inazumadraft.model.canPlay
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

    // TAP swap
    private var pendingBenchIndex: Int? = null
    private var pendingFieldIndex: Int? = null

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

    // --------- Stats ---------
    private fun refreshStatsList() {
        val combined = ArrayList<Player>().apply {
            addAll(playerSlots.filterNotNull())
            addAll(benchPlayers.filterNotNull())
        }
        recyclerFinalTeam.adapter = FinalTeamAdapter(combined)
    }

    // --------- Bench Drawer ---------
    private fun openBenchDrawer(root: View, drawer: View, scrim: View) {
        root.visibility = View.VISIBLE
        root.bringToFront()
        scrim.visibility = View.VISIBLE
        drawer.animate().translationX(0f).setDuration(220).start()
        isBenchOpen = true
    }

    private fun closeBenchDrawer(root: View, drawer: View, scrim: View) {
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

        // Coloca ancho y deja cerrado
        drawer.post {
            val w = (resources.displayMetrics.widthPixels * 0.42f).toInt()
            val min = (200 * resources.displayMetrics.density).toInt()
            drawer.layoutParams = drawer.layoutParams.apply { width = kotlin.math.max(min, w) }
            drawer.translationX = drawer.width.toFloat()
            root.visibility = View.GONE
            scrim.visibility = View.GONE
        }
        scrim.setOnClickListener { closeBenchDrawer(root, drawer, scrim) }

        // ---------- Asa lateral (usa addContentView; evita NPE) ----------
        if (benchHandle == null) {
            val d = resources.displayMetrics.density
            benchHandle = Button(this).apply {
                text = "BANQUILLO"
                rotation = -90f
                setAllCaps(true)
                setBackgroundColor(0xff_ff_cc_00.toInt())
                setTextColor(0xff_00_00_00.toInt())
                elevation = 16f
                setOnClickListener { openBenchDrawer(root, drawer, scrim) }
            }
            val handleLp = FrameLayout.LayoutParams((44 * d).toInt(), (120 * d).toInt()).apply {
                gravity = android.view.Gravity.END or android.view.Gravity.CENTER_VERTICAL
                marginEnd = (4 * d).toInt()
            }
            addContentView(benchHandle, handleLp)  // ← en vez de parent.addView(...)
        }
        benchHandle?.visibility = View.VISIBLE
        benchHandle?.bringToFront()

        // ---------- Hot zone (también addContentView) ----------
        if (benchHotZone == null) {
            val d = resources.displayMetrics.density
            benchHotZone = View(this).apply {
                // opcional: abrir con gesto / área táctil
                setOnClickListener { openBenchDrawer(root, drawer, scrim) }
            }
            val hzLp = FrameLayout.LayoutParams((28 * d).toInt(), FrameLayout.LayoutParams.MATCH_PARENT).apply {
                gravity = android.view.Gravity.END
            }
            addContentView(benchHotZone, hzLp)
        }

        // ---------- Lista de banquillo (misma carta que campo) ----------
        rvSel.layoutManager = LinearLayoutManager(this)
        rvSel.itemAnimator = null
        rvSel.adapter = BenchSelectedAdapter(
            benchPlayers = benchPlayers,
            onTapSlot = { index -> handleBenchTapFinal(index) },
            onChanged = {
                txtTitle.text = "Banquillo (${benchSize()}/5)"
                refreshStatsList()
            },
            onDropFromField = { benchIndex, fieldIndex ->
                val fieldPlayer = playerSlots.getOrNull(fieldIndex) ?: return@BenchSelectedAdapter false
                val prevBench = benchPlayers[benchIndex]
                benchPlayers[benchIndex] = fieldPlayer
                playerSlots[fieldIndex] = prevBench
                requestDrawField()
                findViewById<RecyclerView?>(R.id.rvBenchSelected)
                    ?.adapter?.notifyItemChanged(benchIndex)
                refreshStatsList()
                true
            }
        )


        // Opciones (vacías por defecto, se llenan al tocar hueco vacío)
        rvOpts.layoutManager = GridLayoutManager(this, 2)
        rvOpts.adapter = OptionAdapter(players = emptyList(), onClick = { /* no-op */ })

        txtTitle.text = "Banquillo (${benchSize()}/5)"
    }

    private fun showBenchOptionsForSlotFinal(slotIndex: Int) {
        val usados = mutableSetOf<Player>().apply {
            addAll(playerSlots.filterNotNull())
            addAll(benchPlayers.filterNotNull())
        }
        val options = PlayerRepository.players.filter { it !in usados }.shuffled().take(4)

        val dialogView = layoutInflater.inflate(R.layout.layout_player_picker, null)
        val dialog = android.app.Dialog(this).apply {
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
                findViewById<TextView?>(R.id.txtBenchTitle)?.text = "Banquillo (${benchSize()}/5)"
                refreshStatsList()
                dialog.dismiss()
            }
        )

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun handleBenchTapFinal(index: Int) {
        val benchPlayer = benchPlayers[index]

        // 1) Si hay CAMPO seleccionado y tocas banquillo con jugador → SWAP
        pendingFieldIndex?.let { fIndex ->
            val fieldPlayer = playerSlots.getOrNull(fIndex)
            if (fieldPlayer == null) {
                Toast.makeText(this, "Selecciona primero un jugador del campo", Toast.LENGTH_SHORT).show()
                pendingFieldIndex = null
                return
            }
            if (benchPlayer != null) {
                benchPlayers[index] = fieldPlayer
                playerSlots[fIndex] = benchPlayer
                pendingFieldIndex = null
                pendingBenchIndex = null
                requestDrawField()
                findViewById<RecyclerView?>(R.id.rvBenchSelected)?.adapter?.notifyItemChanged(index)
                findViewById<TextView?>(R.id.txtBenchTitle)?.text = "Banquillo (${benchSize()}/5)"
                refreshStatsList()
                return
            } else {
                // ✅ Hueco vacío → cancelar selección y abrir pick de suplentes
                pendingFieldIndex = null
                pendingBenchIndex = null
                showBenchOptionsForSlotFinal(index)
                return
            }
        }

        // 2) Si hay BANQUILLO seleccionado:
        pendingBenchIndex?.let { bIndex ->
            if (benchPlayer == null) {
                // ✅ Vacío → cancelar selección y abrir pick
                pendingBenchIndex = null
                showBenchOptionsForSlotFinal(index)
                return
            } else {
                // Cambiamos la selección al nuevo índice con jugador (opcional)
                pendingBenchIndex = index
                return
            }
        }

        // 3) Sin selecciones previas:
        if (benchPlayer != null) {
            pendingBenchIndex = index
            Toast.makeText(this, "Toca un jugador del campo para intercambiar", Toast.LENGTH_SHORT).show()
        } else {
            // ✅ Hueco vacío → abrir pick
            showBenchOptionsForSlotFinal(index)
        }
    }


    // --------- Campo y render ---------

    // ✅ ÚNICA definición
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

        view.setOnClickListener(null)

        val formation = formations.firstOrNull { it.name == formationName } ?: return
        val roleCode = toCode(formation.positions[i])
        val p = playerSlots.getOrNull(i)

        view.setOnLongClickListener {
            val clip = android.content.ClipData(
                "field",
                arrayOf(android.content.ClipDescription.MIMETYPE_TEXT_PLAIN),
                android.content.ClipData.Item(i.toString())
            )
            it.startDragAndDrop(clip, View.DragShadowBuilder(it), i, 0)
            true
        }
        view.setOnDragListener { v, e ->
            when (e.action) {
                android.view.DragEvent.ACTION_DRAG_STARTED -> true
                android.view.DragEvent.ACTION_DRAG_ENTERED -> { v.alpha = 0.85f; true }
                android.view.DragEvent.ACTION_DRAG_EXITED  -> { v.alpha = 1f;    true }
                android.view.DragEvent.ACTION_DROP -> {
                    v.alpha = 1f
                    val fromIndex = e.localState as? Int ?: return@setOnDragListener false
                    val toIndex = i
                    if (fromIndex == toIndex) return@setOnDragListener false

                    val formation = com.inazumadraft.data.formations.first { it.name == formationName }
                    val src = playerSlots.getOrNull(fromIndex)
                    val dst = playerSlots.getOrNull(toIndex)
                    val roleFrom = toCode(formation.positions[fromIndex])
                    val roleTo   = toCode(formation.positions[toIndex])

                    if (src == null || dst == null) {
                        android.widget.Toast.makeText(this, "Solo se puede intercambiar entre jugadores", android.widget.Toast.LENGTH_SHORT).show()
                        return@setOnDragListener false
                    }
                    if (!src.canPlay(roleTo) || !dst.canPlay(roleFrom)) {
                        android.widget.Toast.makeText(this, "No pueden intercambiar sus posiciones", android.widget.Toast.LENGTH_SHORT).show()
                        return@setOnDragListener false
                    }

                    // SWAP
                    playerSlots[fromIndex] = dst
                    playerSlots[toIndex]   = src
                    requestDrawField()
                    refreshStatsList()
                    true
                }
                android.view.DragEvent.ACTION_DRAG_ENDED -> { v.alpha = 1f; true }
                else -> false
            }
        }

        view.setOnDragListener { v, e ->
            when (e.action) {
                DragEvent.ACTION_DRAG_STARTED -> e.clipDescription?.label == "bench"
                DragEvent.ACTION_DROP -> {
                    val bIndex = e.localState as? Int ?: return@setOnDragListener false
                    val incoming = benchPlayers.getOrNull(bIndex) ?: return@setOnDragListener false
                    if (!incoming.canPlay(roleCode)) {
                        Toast.makeText(this, "No puede jugar en ${codeToNice(roleCode)}", Toast.LENGTH_SHORT).show()
                        return@setOnDragListener false
                    }
                    benchPlayers[bIndex] = playerSlots[i]
                    playerSlots[i] = incoming
                    pendingBenchIndex = null
                    pendingFieldIndex = null
                    requestDrawField()
                    findViewById<RecyclerView?>(R.id.rvBenchSelected)?.adapter?.notifyItemChanged(bIndex)
                    refreshStatsList()
                    true
                }
                else -> false
            }
        }
        if (p != null) {
            view.setOnLongClickListener {
                val clip = android.content.ClipData(
                    "field", arrayOf(android.content.ClipDescription.MIMETYPE_TEXT_PLAIN),
                    android.content.ClipData.Item(i.toString())
                )
                it.startDragAndDrop(clip, View.DragShadowBuilder(it), i, 0)
                true
            }
        }

        if (p != null) {
            img.setImageResource(p.image)
            name.text = p.nickname
            elem.setImageResource(p.element)
            if (p.name == captainName) img.setBackgroundResource(R.drawable.captain_border) else img.background = null

            // TAP swap campo ocupado
            view.setOnClickListener {
                val roleCode = toCode(formation.positions[i])
                pendingBenchIndex?.let { bIndex ->
                    val incoming = benchPlayers[bIndex] ?: run {
                        pendingBenchIndex = null
                        return@setOnClickListener
                    }
                    if (!incoming.canPlay(roleCode)) {
                        Toast.makeText(this, "No puede jugar en ${codeToNice(roleCode)}", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    benchPlayers[bIndex] = p
                    playerSlots[i] = incoming
                    pendingBenchIndex = null
                    requestDrawField()
                    findViewById<RecyclerView?>(R.id.rvBenchSelected)?.adapter?.notifyItemChanged(bIndex)
                    refreshStatsList()
                } ?: run {
                    pendingFieldIndex = i
                    Toast.makeText(this, "Toca un jugador del banquillo para intercambiar", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            name.text = codeToNice(toCode(formation.positions[i]))
            elem.setImageResource(0)
            img.setImageResource(0)
            img.background = null

            view.setOnClickListener {
                if (pendingBenchIndex != null) {
                    Toast.makeText(this, "El intercambio debe ser entre dos jugadores", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun benchSize(): Int = benchPlayers.count { it != null }

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
