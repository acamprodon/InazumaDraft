package com.inazumadraft.ui

import android.content.ClipData
import android.content.Intent
import android.os.Build
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

        recyclerFinalTeam.layoutManager = LinearLayoutManager(this)
        recyclerFinalTeam.adapter = FinalTeamAdapter(team)

        fieldLayout.post { drawTemplateAndFill(formationName, captainName) }

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
            val intent = Intent(this, DraftActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun drawTemplateAndFill(formationName: String, captainName: String?) {
        fieldLayout.removeAllViews()
        val formation = formations.firstOrNull { it.name == formationName } ?: return
        val coords = formationCoordinates[formationName] ?: return
        val d = resources.displayMetrics.density
        val cardW = (100f * d).toInt()
        val cardH = (125f * d).toInt()

        coords.forEachIndexed { index, (x, y) ->
            val view = layoutInflater.inflate(R.layout.item_player_field, fieldLayout, false)
            val img = view.findViewById<ImageView>(R.id.imgPlayer)
            val name = view.findViewById<TextView>(R.id.txtPlayerNickname)
            val elem = view.findViewById<ImageView>(R.id.imgElement)

            val p = playerSlots.getOrNull(index)
            if (p != null) {
                img.setImageResource(p.image)
                elem.setImageResource(p.element)
                name.text = p.nickname
                if (p.name == captainName) img.setBackgroundResource(R.drawable.captain_border)
            } else {
                img.setImageResource(0)
                name.text = "Vacío"
                elem.setImageResource(0)
            }

            val lp = RelativeLayout.LayoutParams(cardW, cardH)
            lp.leftMargin = (fieldLayout.width * x - cardW / 2).toInt()
            lp.topMargin = (fieldLayout.height * y - cardH / 2).toInt()
            view.layoutParams = lp

            // ----- LONG CLICK PARA ARRASTRAR -----
            if (p != null) {
                view.setOnLongClickListener {
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
            view.setOnDragListener { v, event ->
                when (event.action) {
                    DragEvent.ACTION_DRAG_ENTERED -> { v.alpha = 0.7f; true }
                    DragEvent.ACTION_DRAG_EXITED -> { v.alpha = 1f; true }
                    DragEvent.ACTION_DROP -> {
                        val from = event.localState as? Int
                        if (from != null && from != index) {
                            val tmp = playerSlots[from]
                            playerSlots[from] = playerSlots[index]
                            playerSlots[index] = tmp
                            drawTemplateAndFill(formationName, captainName)
                        }
                        true
                    }
                    DragEvent.ACTION_DRAG_ENDED -> { v.alpha = 1f; true }
                    else -> true
                }
            }

            fieldLayout.addView(view)
        }
    }
}
