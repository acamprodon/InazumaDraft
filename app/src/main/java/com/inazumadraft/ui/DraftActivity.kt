package com.inazumadraft.ui

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.inazumadraft.R
import com.inazumadraft.data.Formation
import com.inazumadraft.data.PlayerRepository
import com.inazumadraft.data.formations
import com.inazumadraft.model.Player

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
                startActivity(intent)
            }
        }

        rvOptions.visibility = View.GONE
        fieldLayout.visibility = View.GONE
        findViewById<View>(R.id.formationButtonsLayout).bringToFront()
        formationLocked = false
    }

    // ---------- FORMATION ----------
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
    }

    private fun toCode(pos: String): String = when (pos.trim().lowercase()) {
        "portero", "pt" -> "PT"
        "defensa", "df" -> "DF"
        "centrocampista", "mc" -> "MC"
        "delantero", "dl" -> "DL"
        else -> pos.trim().uppercase()
    }

    private fun codeToLabel(code: String): String = when (code.uppercase()) {
        "PT" -> "Portero"
        "DF" -> "Defensa"
        "MC" -> "Centrocampista"
        "DL" -> "Delantero"
        else -> code
    }

    // ---------- CAPTAIN ----------
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
                    drawSlots()
                }
            }
        )
    }

    // ---------- BUILD FIELD ----------
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
        rowSpec = buildList { addAll(dlRows); addAll(mcRows); addAll(dfRows); addAll(ptRows) }

        slots.clear()
        repeat(nDL) { slots.add(Slot("DL")) }
        repeat(nMC) { slots.add(Slot("MC")) }
        repeat(nDF) { slots.add(Slot("DF")) }
        repeat(nPT) { slots.add(Slot("PT")) }

        captain?.let { cap ->
            val idx = slots.indexOfFirst { it.role.equals(cap.position, ignoreCase = true) && it.player == null }
            if (idx >= 0) {
                slots[idx].player = cap
                selectedPlayers.add(cap)
            }
        }
    }

    // ---------- DRAW FIELD ----------
    private fun drawSlots() {
        if (fieldLayout.width == 0 || fieldLayout.height == 0) {
            fieldLayout.post { drawSlots() }
            return
        }

        fieldLayout.removeAllViews()
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
            val t = if (rowsCount == 1) 0.5f else r / (rowsCount - 1f)
            topPad + t * (bottomPad - topPad)
        }

        var slotGlobalIndex = 0
        rowSpec.forEachIndexed { rowIdx, cols ->
            val n = cols
            val rowWidth = n * cardW + (n - 1) * hGap
            val startX = (fieldLayout.width - rowWidth) / 2f
            val yCenter = yCenters[rowIdx]

            repeat(n) { i ->
                val slot = slots[slotGlobalIndex]
                val slotView = layoutInflater.inflate(R.layout.item_player_field, fieldLayout, false)
                val img = slotView.findViewById<ImageView>(R.id.imgPlayer)
                val name = slotView.findViewById<TextView>(R.id.txtPlayerName)
                val elem = slotView.findViewById<ImageView>(R.id.imgElement)

                if (slot.player != null) {
                    val p = slot.player!!
                    img.setImageResource(p.image)
                    elem.setImageResource(p.element)
                    name.text = p.name
                    if (p == captain) img.setBackgroundResource(R.drawable.captain_border)
                } else {
                    img.setImageResource(0)
                    elem.setImageResource(0)
                    name.text = codeToLabel(slot.role)
                }

                val lp = RelativeLayout.LayoutParams(cardW.toInt(), cardH.toInt())
                lp.leftMargin = (startX + i * (cardW + hGap)).toInt()
                lp.topMargin = (yCenter - cardH / 2f).toInt()
                slotView.layoutParams = lp

                val indexForClick = slotGlobalIndex
                slotView.setOnClickListener {
                    if (slots[indexForClick].player == null) {
                        showOptionsForSlot(indexForClick)
                    }
                }

                fieldLayout.addView(slotView)
                slotGlobalIndex++
            }
        }

        val total = selectedFormation?.positions?.size ?: 0
        val filled = slots.count { it.player != null }
        btnNext.visibility = if (filled == total) View.VISIBLE else View.GONE
    }

    // ---------- PLAYER PICKER ----------
    private fun showOptionsForSlot(slotIndex: Int) {
        val slot = slots[slotIndex]
        val role = slot.role.uppercase()

        val options = PlayerRepository.players
            .filter {
                it.position.equals(role, ignoreCase = true) &&
                        it != captain &&
                        it !in selectedPlayers &&
                        it !in slots.mapNotNull { s -> s.player }
            }
            .shuffled()
            .take(4)

        val dialogView = layoutInflater.inflate(R.layout.layout_player_picker, null)
        val dialog = android.app.Dialog(this, R.style.CenterDialogTheme)
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
                fieldLayout.post { drawSlots() }
            },
            onLongClick = { player ->
                showPreviewOnLongPress(slotIndex, player, dialog, options)
            }
        )

        dialog.show()
    }

    // ---------- PREVIEW DINÁMICO ----------
    private fun showPreviewOnLongPress(
        slotIndex: Int,
        player: Player,
        dialog: android.app.Dialog,
        currentOptions: List<Player>
    ) {
        val slot = slots[slotIndex]
        val backupSlots = slots.map { it.copy() }

        // Cerramos el diálogo
        dialog.dismiss()

        // Mostrar el jugador en el campo real
        slots[slotIndex].player = player
        drawSlots()

        // Borde dorado temporal
        fieldLayout.post {
            val slotView = fieldLayout.getChildAt(slotIndex)
            slotView?.findViewById<ImageView>(R.id.imgPlayer)
                ?.setBackgroundResource(R.drawable.captain_border)
        }

        // Activar overlay (detectar cuando suelta el dedo)
        overlayPreview.visibility = View.VISIBLE
        overlayPreview.alpha = 0f
        overlayPreview.animate().alpha(0.25f).setDuration(100).start()

        overlayPreview.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                // Restaurar campo original
                for (i in slots.indices) {
                    slots[i].player = backupSlots[i].player
                }
                drawSlots()

                overlayPreview.animate().alpha(0f).setDuration(100).withEndAction {
                    overlayPreview.visibility = View.GONE
                    reopenSamePlayerPicker(slotIndex, currentOptions)
                }.start()

                overlayPreview.setOnTouchListener(null)
            }
            true
        }
    }

    private fun reopenSamePlayerPicker(slotIndex: Int, sameOptions: List<Player>) {
        val slot = slots[slotIndex]

        val dialogView = layoutInflater.inflate(R.layout.layout_player_picker, null)
        val dialog = android.app.Dialog(this, R.style.CenterDialogTheme)
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
                fieldLayout.post { drawSlots() }
            },
            onLongClick = { player ->
                showPreviewOnLongPress(slotIndex, player, dialog, sameOptions)
            }
        )

        dialog.show()
    }

    // ---------- BUILD FINAL TEAM ----------
    private fun buildFinalTeamFromSlots(formation: Formation): List<Player> {
        val result = mutableListOf<Player>()
        val tmp = slots.toMutableList()
        formation.positions.forEach { pos ->
            val idx = tmp.indexOfFirst { it.role.equals(pos, ignoreCase = true) && it.player != null }
            if (idx >= 0) {
                result.add(tmp[idx].player!!)
                tmp.removeAt(idx)
            }
        }
        return result
    }
}
