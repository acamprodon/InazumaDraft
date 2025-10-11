package com.inazumadraft.ui

import android.os.Bundle
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.inazumadraft.R
import com.inazumadraft.data.formationCoordinates
import com.inazumadraft.model.Player

class FinalTeamActivity : AppCompatActivity() {

    private lateinit var fieldLayout: RelativeLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_final_team)

        fieldLayout = findViewById(R.id.fieldLayout)

        val players = intent.getParcelableArrayListExtra<Player>("finalTeam")
        val formationName = intent.getStringExtra("formation") ?: "1-2-1"

        // 🧠 Espera a que el campo esté dibujado antes de colocar jugadores
        if (players != null) {
            fieldLayout.post {
                showPlayersOnField(players, formationName)
            }
        }
    }

    private fun showPlayersOnField(players: List<Player>, formationName: String) {
        val coordinates = formationCoordinates[formationName] ?: return

        players.forEachIndexed { index, player ->
            val (x, y) = coordinates.getOrNull(index) ?: return@forEachIndexed

            // Inflar el layout del jugador
            val playerView = layoutInflater.inflate(R.layout.item_player_field, fieldLayout, false)
            val imgPlayer = playerView.findViewById<ImageView>(R.id.imgPlayer)
            val txtName = playerView.findViewById<TextView>(R.id.txtPlayerName)

            imgPlayer.setImageResource(player.image)
            txtName.text = player.name

            // 🏅 Si es el capitán (el primero de la lista), poner borde dorado
            if (index == 0) {
                imgPlayer.setBackgroundResource(R.drawable.captain_border)
            }

            // Calcular posición proporcional al tamaño del campo
            val params = RelativeLayout.LayoutParams(200, 240)
            params.leftMargin = (fieldLayout.width * x - 100).toInt()
            params.topMargin = (fieldLayout.height * y - 120).toInt()
            playerView.layoutParams = params

            fieldLayout.addView(playerView)
        }
    }
}
