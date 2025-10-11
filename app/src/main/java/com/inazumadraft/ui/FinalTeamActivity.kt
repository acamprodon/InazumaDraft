package com.inazumadraft.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.inazumadraft.R
import com.inazumadraft.data.formationCoordinates
import com.inazumadraft.model.Player

class FinalTeamActivity : AppCompatActivity() {

    private lateinit var fieldLayout: RelativeLayout
    private lateinit var recyclerFinalTeam: RecyclerView
    private lateinit var btnToggleView: Button
    private var showingStats = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_final_team)

        fieldLayout = findViewById(R.id.fieldLayout)
        recyclerFinalTeam = findViewById(R.id.recyclerFinalTeam)
        btnToggleView = findViewById(R.id.btnToggleView)

        val players = intent.getParcelableArrayListExtra<Player>("finalTeam")
        val formationName = intent.getStringExtra("formation") ?: "1-2-1"
        val captainName = intent.getStringExtra("captainName")

        if (players != null) {
            // Mostrar jugadores en el campo
            fieldLayout.post {
                showPlayersOnField(players, formationName, captainName)
            }

            // Configurar lista de estadísticas
            recyclerFinalTeam.layoutManager = LinearLayoutManager(this)
            recyclerFinalTeam.adapter = FinalTeamAdapter(players)

            // Botón alternar vista
            btnToggleView.setOnClickListener {
                showingStats = !showingStats
                if (showingStats) {
                    fieldLayout.visibility = View.GONE
                    recyclerFinalTeam.visibility = View.VISIBLE
                    btnToggleView.text = "Ver campo"
                } else {
                    fieldLayout.visibility = View.VISIBLE
                    recyclerFinalTeam.visibility = View.GONE
                    btnToggleView.text = "Ver estadísticas"
                }
            }
        }
    }

    private fun showPlayersOnField(players: List<Player>, formationName: String, captainName: String?) {
        val coordinates = formationCoordinates[formationName] ?: return

        players.forEachIndexed { index, player ->
            val (x, y) = coordinates.getOrNull(index) ?: return@forEachIndexed

            val playerView = layoutInflater.inflate(R.layout.item_player_field, fieldLayout, false)
            val imgPlayer = playerView.findViewById<ImageView>(R.id.imgPlayer)
            val txtName = playerView.findViewById<TextView>(R.id.txtPlayerName)
            val imgElement = playerView.findViewById<ImageView>(R.id.imgElement)

            imgPlayer.setImageResource(player.image)
            imgElement.setImageResource(player.element)
            txtName.text = player.name

            // Resalta al capitán
            if (player.name == captainName) {
                imgPlayer.setBackgroundResource(R.drawable.captain_border)
            }

            val d = resources.displayMetrics.density
            val w = (120 * d).toInt()
            val h = (150 * d).toInt()
            val params = RelativeLayout.LayoutParams(w, h)
            params.leftMargin = (fieldLayout.width * x - w / 2f).toInt()
            params.topMargin  = (fieldLayout.height * y - h / 2f).toInt()
            playerView.layoutParams = params

            fieldLayout.addView(playerView)
        }
    }
}
