package com.inazumadraft.ui

import android.content.ClipData
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.DragEvent
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
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

    private val benchPlayers = MutableList<Player?>(5) { null } // 5 huecos
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

        val team = intent.getParcelableArrayListExtra<Player>("finalTeam") ?: arrayListOf()
        formationName = intent.getStringExtra("formation") ?: "4-4-2"
        captainName = intent.getStringExtra("captainName")

        playerSlots.clear()
        playerSlots.addAll(team)

        fieldLayout.post { drawTemplateAndFill() }

        recyclerFinalTeam.layoutManager = LinearLayoutManager(this)
        recyclerFinalTeam.adapter = FinalTeamAdapter(team)

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

        btnNewTeam.setOnClickListener {
            val intent = Intent(this@FinalTeamActivity, DraftActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent); finish()
        }

        setupBenchPanel()
    }

    // ----------------- HELPERS BANQUILLO -----------------
    private fun benchCount(): Int = benchPlayers.count { it != null }

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
        val dialog = android.app.Dialog(this, R.style.CenterDialogTheme)
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
                findViewById<RecyclerView?>(R.id.rvBenchSelected)?.adapter?.notifyDataSetChanged()
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
        scrim.setOnClickListener {
            drawer.animate().translationX(drawer.width.toFloat()).setDuration(220).withEndAction {
                scrim.visibility = View.GONE
                root.visibility = View.GONE
                onClose()
            }.start()
        }

        val d = handleParent.resources.displayMetrics.density
        val handle = Button(handleParent.context).apply {
            text = "BANQUILLO"
            rotation = -90f
            setAllCaps(true)
            setBackgroundColor(0xff_ff_cc_00.toInt())
            setTextColor(0xff_00_00_00.toInt())
            elevation = 16f
            tag = "bench_handle"
            setOnClickListener {
                root.visibility = View.VISIBLE
                root.bringToFront()
                scrim.visibility = View.VISIBLE
                drawer.animate().translationX(0f).setDuration(220).withStartAction { onOpen() }.start()
            }
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

    // ----------------- BANQUILLO (drawer+listas) -----------------
    private fun setupBenchPanel() {
        val root   = findViewById<View?>(R.id.benchPanel) ?: return
        val drawer = findViewById<View?>(R.id.benchDrawer) ?: return
        val scrim  = findViewById<View?>(R.id.benchScrim) ?: return
        val rvSel  = findViewById<RecyclerView?>(R.id.rvBenchSelected) ?: return
        val rvOpts = findViewById<RecyclerView?>(R.id.rvBenchOptions) ?: return
        val btnClose = findViewById<Button?>(R.id.btnCloseBench) ?: return
        val txtTitle = findViewById<TextView?>(R.id.txtBenchTitle) ?: return

        fun updateTitle() { txtTitle.text = "Banquillo (${benchCount()}/5)" }
        updateTitle()

        rvSel.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvSel.adapter = com.inazumadraft.ui.adapters.BenchSelectedAdapter(
            benchPlayers = benchPlayers,
            onChanged = { updateTitle(); drawTemplateAndFill() },
            onDropFromField = { toIndex, fromFieldIndex ->
                val p = playerSlots.getOrNull(fromFieldIndex) ?: return@BenchSelectedAdapter
                val replaced = benchPlayers[toIndex]
                benchPlayers[toIndex] = p
                playerSlots[fromFieldIndex] = replaced
                rvSel.adapter?.notifyDataSetChanged()
                updateTitle()
                drawTemplateAndFill()
            },
            onClickSlot = { index -> showBenchOptionsForSlotFinal(index) }
        )

        // No usamos la grilla inferior (se rellena tocando slots)
        rvOpts.layoutManager = GridLayoutManager(this, 2)
        rvOpts.adapter = OptionAdapter(
            emptyList(),
            onClick = { _: com.inazumadraft.model.Player -> }
        )


        btnClose.setOnClickListener {
            drawer.animate().translationX(drawer.width.toFloat()).setDuration(200).withEndAction {
                scrim.visibility = View.GONE
                root.visibility = View.GONE
            }.start()
        }

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
    }

    // ----------------- CAMPO + DnD -----------------
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

    private fun drawTemplateAndFill() {
        fieldLayout.removeAllViews()
        val formation = formations.firstOrNull { it.name == formationName } ?: return

        val d = resources.displayMetrics.density
        var cardW = 100f * d
        var cardH = cardW * 1.25f
        val hGap = 8f * d
        val maxCols = 4
        val rowWidthNeeded = maxCols * cardW + (maxCols - 1) * hGap
        if (rowWidthNeeded > fieldLayout.width) { cardW = (fieldLayout.width - (maxCols - 1) * hGap) / maxCols; cardH = cardW * 1.25f }

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

        // Fallback por filas (si no hay coords)
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

        val p = playerSlots.getOrNull(index)
        if (p != null) {
            view.setOnLongClickListener {
                val clipData = ClipData.newPlainText("fromIndex", index.toString())
                val shadow = View.DragShadowBuilder(it)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) it.startDragAndDrop(clipData, shadow, index, 0)
                else @Suppress("DEPRECATION") it.startDrag(clipData, shadow, index, 0)
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
                    val fromBench = event.clipDescription?.label == "benchPlayer"
                    if (fromBench) {
                        val benchIdx = event.localState as Int
                        val player = benchPlayers.getOrNull(benchIdx) ?: return@setOnDragListener true
                        val bp = player ?: return@setOnDragListener true
                        if (!bp.canPlay(dstRoleCode)) { shakeView(v); return@setOnDragListener true }
                        val replaced = playerSlots[index]
                        playerSlots[index] = bp
                        benchPlayers[benchIdx] = replaced
                        findViewById<RecyclerView?>(R.id.rvBenchSelected)?.adapter?.notifyDataSetChanged()
                        drawTemplateAndFill()
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
                            drawTemplateAndFill()
                        } else shakeView(v)
                    }
                    true
                }
                DragEvent.ACTION_DRAG_ENDED -> { v.alpha = 1f; true }
                else -> false
            }
        }
    }

    private fun shakeView(v: View) {
        v.animate().translationX(12f).setDuration(40).withEndAction {
            v.animate().translationX(-10f).setDuration(40).withEndAction {
                v.animate().translationX(0f).setDuration(40).start()
            }.start()
        }.start()
    }
}
