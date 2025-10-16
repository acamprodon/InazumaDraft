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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.inazumadraft.ui.adapters.OptionAdapter
import com.inazumadraft.ui.adapters.FinalTeamAdapter
import com.inazumadraft.ui.adapters.BenchSelectedAdapter
import com.inazumadraft.R
import com.inazumadraft.data.formationCoordinates
import com.inazumadraft.data.formations
import com.inazumadraft.model.Player
import com.inazumadraft.model.canPlay
import com.inazumadraft.model.Tecnica
import com.inazumadraft.data.TecnicaRepository
import kotlin.math.max

class FinalTeamActivity : AppCompatActivity() {

    internal lateinit var fieldLayout: RelativeLayout
    private lateinit var btnToggleView: Button
    private lateinit var btnNewTeam: FloatingActionButton
    private lateinit var recyclerFinalTeam: RecyclerView

    internal val playerSlots = mutableListOf<Player?>()
    internal val benchPlayers = MutableList<Player?>(5) { null }
    internal var formationName: String = "4-4-2"
    private var captainName: String? = null

    private val slotViews: MutableList<View> = mutableListOf()
    private var drawPending = false

    private var isBenchOpen = false
    private var benchHandle: View? = null
    private var benchHotZone: View? = null

    internal var pendingBenchIndex: Int? = null
    internal var pendingFieldIndex: Int? = null

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

        playerSlots.clear(); playerSlots.addAll(team)
        intent.getParcelableArrayListExtra<Player>("benchPlayers")?.let { list ->
            for (i in 0 until 5) benchPlayers[i] = list.getOrNull(i)
        }

        fieldLayout.post { requestDrawField() }

        recyclerFinalTeam.layoutManager = LinearLayoutManager(this)
        refreshStatsList()

        btnToggleView.text = "VER ESTADÍSTICAS"
        btnToggleView.setOnClickListener {
            val isStatsVisible = recyclerFinalTeam.visibility == View.GONE

            if (isStatsVisible) {
                // 🔹 Mostrar estadísticas
                recyclerFinalTeam.visibility = View.VISIBLE
                fieldLayout.visibility = View.GONE
                refreshStatsList()
                btnToggleView.text = "VER CAMPO"

                // 👇 Ocultamos el botón y zona de banquillo
                benchHandle?.visibility = View.GONE
                benchHotZone?.visibility = View.GONE
            } else {
                // 🔹 Volver al campo
                recyclerFinalTeam.visibility = View.GONE
                fieldLayout.visibility = View.VISIBLE
                btnToggleView.text = "VER ESTADÍSTICAS"

                // 👇 Volvemos a mostrar acceso al banquillo
                benchHandle?.visibility = View.VISIBLE
                benchHotZone?.visibility = View.VISIBLE
            }
        }


        btnNewTeam.setOnClickListener {
            val dialog = android.app.AlertDialog.Builder(this)
                .setTitle("¿Reiniciar equipo?")
                .setMessage("Perderás tu equipo actual y volverás a hacer un nuevo draft.")
                .setPositiveButton("Sí") { _, _ ->
                    // Cierra esta pantalla y vuelve al DraftActivity
                    val intent = Intent(this, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Cancelar", null)
                .create()

            dialog.show()
        }
        findViewById<Button>(R.id.btnFinishTeam).setOnClickListener {
            val intent = Intent(this, TeamSummaryActivity::class.java)
            val team = ArrayList(playerSlots.filterNotNull())
            intent.putParcelableArrayListExtra("finalTeam", team)
            startActivity(intent)
            val teamWithTecnica = team.associateWith { assignRandomTechniquesToPlayer(it)}
            val unlockedCombined = getUnlockedCombinedTecnica(team)
        }

        setupBenchPanel()
    }
    fun getUnlockedCombinedTecnica(team: List<Player>): List<Tecnica> {
        val nicknames = team.map { it.nickname }
        return TecnicaRepository.tecnicas.filter { tech ->
            tech.combined && tech.players.all { it in nicknames }
        }
    }

    private fun refreshStatsList() {
        val combined = ArrayList<Player>().apply {
            addAll(playerSlots.filterNotNull()); addAll(benchPlayers.filterNotNull())
        }
        recyclerFinalTeam.adapter = FinalTeamAdapter(combined)
    }

    // ---------------- Drawer banquillo ----------------

    private fun openBenchDrawer(root: View, drawer: View, scrim: View) {
        root.visibility = View.VISIBLE; root.bringToFront()
        scrim.visibility = View.VISIBLE
        drawer.animate().translationX(0f).setDuration(220).start()
        isBenchOpen = true
    }
    private fun closeBenchDrawer(root: View, drawer: View, scrim: View) {
        drawer.animate().translationX(drawer.width.toFloat()).setDuration(200)
            .withEndAction { scrim.visibility = View.GONE; root.visibility = View.GONE }.start()
        isBenchOpen = false
    }

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
        scrim.setOnClickListener { closeBenchDrawer(root, drawer, scrim) }

        if (benchHandle == null) {
            val d = resources.displayMetrics.density
            benchHandle = Button(this).apply {
                text = "BANQUILLO"; rotation = -90f; setAllCaps(true)
                setBackgroundColor(0xff_ff_cc_00.toInt()); setTextColor(0xff_00_00_00.toInt()); elevation = 16f
                setOnClickListener { openBenchDrawer(root, drawer, scrim) }
            }
            val lp = FrameLayout.LayoutParams((44 * d).toInt(), (120 * d).toInt()).apply {
                gravity = Gravity.END or Gravity.CENTER_VERTICAL; marginEnd = (4 * d).toInt()
            }
            addContentView(benchHandle, lp)
        }
        benchHandle?.visibility = View.VISIBLE
        benchHandle?.bringToFront()

        if (benchHotZone == null) {
            val d = resources.displayMetrics.density
            benchHotZone = View(this)
            val lp = FrameLayout.LayoutParams((28 * d).toInt(), FrameLayout.LayoutParams.MATCH_PARENT).apply {
                gravity = Gravity.END
            }
            addContentView(benchHotZone, lp)
        }

        rvSel.layoutManager = LinearLayoutManager(this)
        rvSel.itemAnimator = null
        rvSel.adapter = BenchSelectedAdapter(
            benchPlayers = benchPlayers,
            onTapSlot = { index -> handleBenchTapFinal(index) },
            onChanged = {
                txtTitle.text = "Banquillo (${benchSize()}/5)"
                refreshStatsList()
            },
            onDropFromField = { benchIndex: Int, fieldIndex: Int ->
                val fieldPlayer = playerSlots.getOrNull(fieldIndex)
                if (fieldPlayer == null) {
                    false
                } else {
                    val prevBench = benchPlayers[benchIndex]
                    benchPlayers[benchIndex] = fieldPlayer
                    playerSlots[fieldIndex] = prevBench

                    requestDrawField()
                    findViewById<RecyclerView?>(R.id.rvBenchSelected)
                        ?.adapter?.notifyItemChanged(benchIndex)
                    refreshStatsList()
                    true
                }
            }
        )

        rvOpts.layoutManager = GridLayoutManager(this, 2)
        rvOpts.adapter = OptionAdapter(players = emptyList(), onClick = { })


        txtTitle.text = "Banquillo (${benchSize()}/5)"
    }

    private fun showBenchOptionsForSlotFinal(slotIndex: Int) {
        val usados = mutableSetOf<Player>().apply {
            addAll(playerSlots.filterNotNull()); addAll(benchPlayers.filterNotNull())
        }
        val options = com.inazumadraft.data.PlayerRepository.players.filter { it !in usados }.shuffled().take(4)

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
                findViewById<TextView?>(R.id.txtBenchTitle)?.text = "Banquillo (${benchSize()}/5)"
                refreshStatsList()
                dialog.dismiss()
            }
        )
        attachPickerPreviewLive(
            rv = rvPicker,
            dialog = dialog,
            options = options
        ) { candidate ->
            val prev = benchPlayers[slotIndex]
            benchPlayers[slotIndex] = candidate
            requestDrawField()

            // devolvemos cómo revertir la vista previa
            return@attachPickerPreviewLive {
                benchPlayers[slotIndex] = prev
                requestDrawField()
                findViewById<RecyclerView?>(R.id.rvBenchSelected)
                    ?.adapter?.notifyItemChanged(slotIndex)
                findViewById<TextView?>(R.id.txtBenchTitle)
                    ?.text = "Banquillo (${benchSize()}/5)"
                refreshStatsList()
            }
        }

        dialog.show()
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.92f).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun handleBenchTapFinal(index: Int) {
        val benchPlayer = benchPlayers[index]
        pendingFieldIndex?.let { fIndex ->
            val fieldPlayer = playerSlots.getOrNull(fIndex)
            if (fieldPlayer == null) { pendingFieldIndex = null; return }
            if (benchPlayer == null) { pendingFieldIndex = null; showBenchOptionsForSlotFinal(index); return }

            // ✅ validar que el del banquillo puede jugar en el rol del slot del campo
            val formation = formations.first { it.name == formationName }
            val roleCode = toCode(formation.positions[fIndex])
            if (!benchPlayer.canPlay(roleCode)) {
                Toast.makeText(this, "No puede jugar en ${codeToNice(roleCode)}", Toast.LENGTH_SHORT).show()
                pendingFieldIndex = null
                return
            }

            benchPlayers[index] = fieldPlayer
            playerSlots[fIndex] = benchPlayer
            pendingFieldIndex = null; pendingBenchIndex = null
            requestDrawField()
            findViewById<RecyclerView?>(R.id.rvBenchSelected)?.adapter?.notifyItemChanged(index)
            refreshStatsList()
            return
        }

        if (benchPlayer != null) { pendingBenchIndex = index; Toast.makeText(this, "Toca un jugador del campo o arrástralo", Toast.LENGTH_SHORT).show() }
        else showBenchOptionsForSlotFinal(index)
    }

    // ---------------- Campo + drag unificado ----------------

    private fun requestDrawField() {
        if (drawPending) return
        drawPending = true
        fieldLayout.post { drawPending = false; drawTemplateAndFillInternal() }
    }

    private fun drawTemplateAndFillInternal() {
        if (fieldLayout.width == 0 || fieldLayout.height == 0) { fieldLayout.post { drawTemplateAndFillInternal() }; return }
        val d = resources.displayMetrics.density
        var cardW = 100f * d; var cardH = cardW * 1.25f; val hGap = 16f * d

        val formation = formations.firstOrNull { it.name == formationName } ?: return
        val coords = formationCoordinates[formationName]
        val totalSlots = formation.positions.size

        while (slotViews.size < totalSlots) { val v = layoutInflater.inflate(R.layout.item_player_field, fieldLayout, false); slotViews.add(v); fieldLayout.addView(v) }
        while (slotViews.size > totalSlots) { val last = slotViews.removeAt(slotViews.lastIndex); fieldLayout.removeView(last) }

        if (coords != null && coords.size == totalSlots) {
            val topBand = 0.05f; val bottomBand = 0.90f; fun mapY(y: Float) = (topBand + y * (bottomBand - topBand)).coerceIn(0f, 1f)
            formation.positions.forEachIndexed { i, _ ->
                val (x, raw) = coords[i]; val y = mapY(raw)
                val v = slotViews[i]; bindSlotView(v, i)
                val lp = RelativeLayout.LayoutParams(cardW.toInt(), cardH.toInt())
                if (toCode(formation.positions[i]) == "PT") {
                    val xPx = fieldLayout.width * 0.50f; val yPx = fieldLayout.height * 0.965f
                    lp.leftMargin = (xPx - cardW / 2f).toInt(); lp.topMargin = (yPx - cardH / 2f).toInt()
                } else {
                    lp.leftMargin = (fieldLayout.width * x - cardW / 2f).toInt()
                    lp.topMargin  = (fieldLayout.height * y - cardH / 2f).toInt()
                }
                v.layoutParams = lp
            }
        } else {
            // fallback por filas (omitido por brevedad)
            formation.positions.forEachIndexed { i, _ -> bindSlotView(slotViews[i], i) }
        }
    }

    private fun bindSlotView(view: View, i: Int) {
        val img = view.findViewById<ImageView>(R.id.imgPlayer)
        val name = view.findViewById<TextView>(R.id.txtPlayerNickname)
        val elem = view.findViewById<ImageView>(R.id.imgElement)

        val formation = formations.first { it.name == formationName }
        val p = playerSlots.getOrNull(i)

        if (p != null) {
            // Long-press para iniciar drag desde CAMPO
            view.setOnLongClickListener {
                val clip = ClipData(
                    "field",
                    arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN),
                    ClipData.Item(i.toString())
                )
                it.startDragAndDrop(clip, View.DragShadowBuilder(it), i, 0)
                true
            }
        } else view.setOnLongClickListener(null)

        // Listener unificado de DROP
        view.setOnDragListener { v, e ->
            when (e.action) {
                DragEvent.ACTION_DRAG_STARTED -> true
                DragEvent.ACTION_DRAG_ENTERED -> { v.alpha = 0.85f; true }
                DragEvent.ACTION_DRAG_EXITED  -> { v.alpha = 1f;    true }
                DragEvent.ACTION_DROP -> {
                    v.alpha = 1f
                    val label = e.clipDescription?.label?.toString()
                    if (label == "field") {
                        val fromIndex = e.localState as? Int ?: return@setOnDragListener false
                        val toIndex = i; if (fromIndex == toIndex) return@setOnDragListener false
                        val src = playerSlots.getOrNull(fromIndex)
                        val dst = playerSlots.getOrNull(toIndex)
                        val roleFrom = toCode(formation.positions[fromIndex])
                        val roleTo   = toCode(formation.positions[toIndex])
                        if (src == null || dst == null) {
                            Toast.makeText(this, "Solo entre jugadores", Toast.LENGTH_SHORT).show(); return@setOnDragListener false
                        }
                        if (!src.canPlay(roleTo) || !dst.canPlay(roleFrom)) {
                            Toast.makeText(this, "No pueden intercambiar sus posiciones", Toast.LENGTH_SHORT).show(); return@setOnDragListener false
                        }
                        playerSlots[fromIndex] = dst; playerSlots[toIndex] = src
                        requestDrawField(); refreshStatsList(); true
                    } else if (label == "bench") {
                        val bIndex = e.localState as? Int ?: return@setOnDragListener false
                        val incoming = benchPlayers.getOrNull(bIndex) ?: return@setOnDragListener false
                        val roleCode = toCode(formation.positions[i])
                        if (!incoming.canPlay(roleCode)) {
                            Toast.makeText(this, "No puede jugar en ${codeToNice(roleCode)}", Toast.LENGTH_SHORT).show()
                            return@setOnDragListener false
                        }
                        val prev = playerSlots[i]
                        benchPlayers[bIndex] = prev; playerSlots[i] = incoming
                        pendingBenchIndex = null; pendingFieldIndex = null
                        requestDrawField(); findViewById<RecyclerView?>(R.id.rvBenchSelected)?.adapter?.notifyItemChanged(bIndex); refreshStatsList(); true
                    } else false
                }
                DragEvent.ACTION_DRAG_ENDED -> { v.alpha = 1f; true }
                else -> false
            }
        }

        if (p != null) {
            img.setImageResource(p.image); name.text = p.nickname; elem.setImageResource(p.element)
            if (p.name == captainName) img.setBackgroundResource(R.drawable.captain_border) else img.background = null
            view.setOnClickListener {
                pendingBenchIndex?.let { bIndex ->
                    val incoming = benchPlayers[bIndex] ?: return@setOnClickListener
                    val roleI = toCode(formation.positions[i])
                    if (!incoming.canPlay(roleI)) {
                        Toast.makeText(this, "No puede jugar en ${codeToNice(roleI)}", Toast.LENGTH_SHORT).show(); return@setOnClickListener
                    }
                    val out = playerSlots[i]; playerSlots[i] = incoming; benchPlayers[bIndex] = out
                    pendingBenchIndex = null; requestDrawField()
                    findViewById<RecyclerView?>(R.id.rvBenchSelected)?.adapter?.notifyItemChanged(bIndex); refreshStatsList()
                } ?: run {
                    pendingFieldIndex = i
                    Toast.makeText(this, "Arrastra sobre otro jugador para intercambiar", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            name.text = codeToNice(toCode(formation.positions[i])); img.setImageResource(0); elem.setImageResource(0); img.background = null
            view.setOnClickListener {
                if (pendingBenchIndex != null) Toast.makeText(this, "Intercambio solo entre jugadores", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun benchSize(): Int = benchPlayers.count { it != null }

    // ----------- helpers preview (mismo patrón que en Draft) -----------

    private fun startLivePreview(dialog: Dialog, onClose: () -> Unit) {
        dialog.hide()
        val root = findViewById<ViewGroup>(android.R.id.content)
        val overlay = FrameLayout(this).apply {
            setBackgroundColor(0x66000000); isClickable = true; isFocusable = true
            val btn = com.google.android.material.button.MaterialButton(context).apply {
                text = "VOLVER AL PICK"; setOnClickListener { onClose(); root.removeView(this@apply); dialog.show() }
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
        requestDrawField()
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
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean { detector.onTouchEvent(e); return false }
            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        })
    }
    fun assignRandomTechniquesToPlayer(player: Player): List<Tecnica> {
        val allTechs = TecnicaRepository.tecnicas
            .filter { !it.combined && it.players.contains(player.nickname) }

        return allTechs.shuffled().take(2)
    }
    internal fun toCode(pos: String): String = when (pos.trim().lowercase()) {
        "portero", "pt" -> "PT"
        "defensa", "df" -> "DF"
        "centrocampista", "mc" -> "MC"
        "delantero", "dl" -> "DL"
        else -> pos.trim().uppercase()
    }
    internal fun codeToNice(code: String): String = when (code.uppercase()) {
        "PT" -> "Portero"; "DF" -> "Defensa"; "MC" -> "Centrocampista"; "DL" -> "Delantero"; else -> code
    }
    override fun onResume() {
        super.onResume()

        // 🔁 Redibuja el campo y actualiza la lista de estadísticas
        requestDrawField()
        refreshStatsList()
    }

}
