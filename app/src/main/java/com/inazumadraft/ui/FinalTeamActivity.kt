package com.inazumadraft.ui

import android.content.Intent
import android.os.Bundle
import android.view.DragEvent
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.inazumadraft.R
import com.inazumadraft.data.formations
import com.inazumadraft.data.formationCoordinates
import com.inazumadraft.model.Player

class FinalTeamActivity : AppCompatActivity() {

    private lateinit var fieldLayout: RelativeLayout
    private lateinit var btnToggleView: Button
    private lateinit var btnNewTeam: FloatingActionButton
    private lateinit var recyclerFinalTeam: RecyclerView

    private val playerSlots = mutableListOf<Player?>()
    private var draggedIndex: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_final_team)

        fieldLayout = findViewById(R.id.fieldLayout)
        btnToggleView = findViewById(R.id.btnToggleView)
        recyclerFinalTeam = findViewById(R.id.recyclerFinalTeam)
        btnNewTeam = findViewById(R.id.btnNewTeam)

        val team = intent.getParcelableArrayListExtra<Player>("finalTeam") ?: arrayListOf()
        val formationName = intent.getStringExtra("formation") ?: "4-4-2"
        val captainName = intent.getStringExtra("captainName")

        playerSlots.clear()
        playerSlots.addAll(team)

        // Dibujar campo con jugadores
        fieldLayout.post { drawTemplateAndFill(formationName, captainName) }

        recyclerFinalTeam.layoutManager = LinearLayoutManager(this)
        recyclerFinalTeam.adapter = FinalTeamAdapter(team)

        // Alternar vista
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

        // Nuevo equipo
        btnNewTeam.setOnClickListener {
            val intent = Intent(this@FinalTeamActivity, DraftActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
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
        "PT" -> "Portero"
        "DF" -> "Defensa"
        "MC" -> "Centrocampista"
        "DL" -> "Delantero"
        else -> code
    }

    // --- DIBUJA CAMPO + DRAG & DROP ---
    private fun drawTemplateAndFill(formationName: String, captainName: String?) {
        fieldLayout.removeAllViews()
        val formation = formations.firstOrNull { it.name == formationName } ?: return
        val coords = formationCoordinates[formationName] ?: return

        val d = resources.displayMetrics.density
        var cardW = 100f * d
        var cardH = cardW * 1.25f
        val hGap = 8f * d
        val maxCols = 4
        val rowWidthNeeded = maxCols * cardW + (maxCols - 1) * hGap
        if (rowWidthNeeded > fieldLayout.width) {
            cardW = (fieldLayout.width - (maxCols - 1) * hGap) / maxCols
            cardH = cardW * 1.25f
        }

        coords.forEachIndexed { index, (x, yRaw) ->
            val topBand = 0.08f
            val bottomBand = 0.96f
            val y = (topBand + yRaw * (bottomBand - topBand)).coerceIn(0f, 1f)

            val view = layoutInflater.inflate(R.layout.item_player_field, fieldLayout, false)
            val img = view.findViewById<ImageView>(R.id.imgPlayer)
            val name = view.findViewById<TextView>(R.id.txtPlayerNickname)
            val elem = view.findViewById<ImageView>(R.id.imgElement)

            val player = playerSlots.getOrNull(index)
            if (player != null) {
                img.setImageResource(player.image)
                name.text = player.nickname
                elem.setImageResource(player.element)
                if (player.name == captainName) img.setBackgroundResource(R.drawable.captain_border)
            } else {
                img.setImageResource(0)
                elem.setImageResource(0)
                name.text = codeToNice(toCode(formation.positions[index]))
            }

            val lp = RelativeLayout.LayoutParams(cardW.toInt(), cardH.toInt())
            lp.leftMargin = (fieldLayout.width * x - cardW / 2f).toInt()
            lp.topMargin = (fieldLayout.height * y - cardH / 2f).toInt()
            view.layoutParams = lp

            // --- DRAG & DROP CONFIG ---
            if (player != null) {
                view.setOnLongClickListener {
                    draggedIndex = index
                    val shadow = View.DragShadowBuilder(it)
                    it.startDragAndDrop(null, shadow, null, 0)
                    it.alpha = 0.5f
                    true
                }
            }

            view.setOnDragListener { v, event ->
                when (event.action) {
                    DragEvent.ACTION_DRAG_STARTED -> true
                    DragEvent.ACTION_DRAG_ENTERED -> {
                        v.alpha = 0.7f
                        true
                    }
                    DragEvent.ACTION_DRAG_EXITED -> {
                        v.alpha = 1f
                        true
                    }
                    DragEvent.ACTION_DROP -> {
                        val from = draggedIndex
                        if (from != null && from != index) {
                            val temp = playerSlots[from]
                            playerSlots[from] = playerSlots[index]
                            playerSlots[index] = temp
                            draggedIndex = null
                            drawTemplateAndFill(formationName, captainName)
                        }
                        true
                    }
                    DragEvent.ACTION_DRAG_ENDED -> {
                        v.alpha = 1f
                        draggedIndex = null
                        true
                    }
                    else -> false
                }
            }

            fieldLayout.addView(view)
        }
    }
}
