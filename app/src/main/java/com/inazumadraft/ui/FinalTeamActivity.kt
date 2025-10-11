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
        val captainName = intent.getStringExtra("captainName")

        if (players != null) {
            // Esperamos a que el layout tenga medidas
            fieldLayout.post {
                showPlayersOnField(players, formationName, captainName)
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

            imgPlayer.setImageResource(player.image)
            txtName.text = player.name

            // Resalta capitán por nombre
            if (player.name == captainName) {
                imgPlayer.setBackgroundResource(R.drawable.captain_border)
            }

            val params = RelativeLayout.LayoutParams(200, 240)
            params.leftMargin = (fieldLayout.width * x - 100).toInt()
            params.topMargin = (fieldLayout.height * y - 120).toInt()
            playerView.layoutParams = params

            fieldLayout.addView(playerView)
        }
    }
}
