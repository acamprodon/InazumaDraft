package com.inazumadraft.ui

import android.app.Dialog
import android.content.ClipData
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.DragEvent
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
    private val slots = mutableListOf<Slot>()
    private val selectedPlayers = mutableListOf<Player>()
    private var captain: Player? = null
    private var rowSpec: List<Int> = emptyList()

    private var isPreviewing = false

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
    }

    // ---------- FORMACIONES ----------
    private fun refreshVisibleFormations() {
        visibleFormations = formations.shuffled().take(4)
        btnFormation1.text = visibleFormations.getOrNull(0)?.name ?: "—"
        btnFormation2.text = visibleFormations.getOrNull(1)?.name ?: "—"
        btnFormation3.text = visibleFormations.getOrNull(2)?.name ?: "—"
        btnFormation4.text = visibleFormations.getOrNull(3)?.name ?: "—"
        roundTitle.text = "Elige una formación"
    }

    private fun selectFormationByIndex(index: Int) {
        if (formationLocked) return
        val f = visibleFormations.getOrNull(index) ?: return
        selectedFormation = f
        formationLocked = true
        roundTitle.text = "Elige tu capitán (${f.name})"
        showCaptainOptions()
        findViewById<View>(R.id.formationButtonsLayout).visibility = View.GONE
    }

    // ---------- CAPITÁN ----------
    private fun showCaptainOptions() {
        rvOptions.visibility = View.VISIBLE
        rvOptions.layoutManager = GridLayoutManager(this, 2)
        rvOptions.adapter = OptionAdapter(
            players = PlayerRepository.players.shuffled().take(4),
            onClick = { player ->
                captain = player
                roundTitle.text = "Capitán: ${player.name} — Rellena la alineación"
                buildSlotsTemplateAndPlaceCaptain()
                rvOptions.visibility = View.GONE
                fieldLayout.visibility = View.VISIBLE
                drawSlots()
            }
        )

    }

    // ---------- GENERA CAMPOS ----------
    private fun buildSlotsTemplateAndPlaceCaptain() {
        val formation = selectedFormation ?: return
        val codes = formation.positions.map { it.uppercase() }
        slots.clear()
        codes.forEach { slots.add(Slot(it)) }

        captain?.let { cap ->
            val idx = slots.indexOfFirst { it.role == cap.position && it.player == null }
            if (idx >= 0) slots[idx].player = cap
        }
    }

    // ---------- DIBUJA Y PERMITE ARRASTRAR ----------
    private fun drawSlots() {
        fieldLayout.removeAllViews()
        val d = resources.displayMetrics.density
        val cardW = (100f * d).toInt()
        val cardH = (125f * d).toInt()

        slots.forEachIndexed { index, slot ->
            val slotView = layoutInflater.inflate(R.layout.item_player_field, fieldLayout, false)
            val img = slotView.findViewById<ImageView>(R.id.imgPlayer)
            val name = slotView.findViewById<TextView>(R.id.txtPlayerNickname)
            val elem = slotView.findViewById<ImageView>(R.id.imgElement)

            if (slot.player != null) {
                val p = slot.player!!
                img.setImageResource(p.image)
                name.text = p.nickname
                elem.setImageResource(p.element)
                if (p == captain) img.setBackgroundResource(R.drawable.captain_border)
            } else {
                img.setImageResource(0)
                elem.setImageResource(0)
                name.text = slot.role
            }

            val lp = RelativeLayout.LayoutParams(cardW, cardH)
            lp.leftMargin = (index % 4) * (cardW + 10)
            lp.topMargin = (index / 4) * (cardH + 10)
            slotView.layoutParams = lp

            // ----- LONG CLICK PARA ARRASTRAR -----
            if (slot.player != null) {
                slotView.setOnLongClickListener {
                    val clipData = ClipData.newPlainText("fromIndex", index.toString())
                    val shadow = View.DragShadowBuilder(it)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        it.startDragAndDrop(clipData, shadow, index, 0)
                    } else {
                        @Suppress("DEPRECATION")
                        it.startDrag(clipData, shadow, index, 0)
                    }
                    it.alpha = 0.5f
                    true
                }
            }

            // ----- DRAG LISTENER -----
            slotView.setOnDragListener { v, event ->
                when (event.action) {
                    DragEvent.ACTION_DRAG_ENTERED -> { v.alpha = 0.7f; true }
                    DragEvent.ACTION_DRAG_EXITED -> { v.alpha = 1f; true }
                    DragEvent.ACTION_DROP -> {
                        val fromIndex = event.localState as? Int
                        if (fromIndex != null && fromIndex != index) {
                            val tmp = slots[fromIndex].player
                            slots[fromIndex].player = slots[index].player
                            slots[index].player = tmp
                            drawSlots()
                        }
                        true
                    }
                    DragEvent.ACTION_DRAG_ENDED -> { v.alpha = 1f; true }
                    else -> true
                }
            }

            fieldLayout.addView(slotView)
        }
    }

    private fun buildFinalTeamFromSlots(formation: Formation): List<Player> =
        slots.mapNotNull { it.player }
}
