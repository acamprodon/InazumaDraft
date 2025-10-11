package com.inazumadraft.ui

import android.os.Bundle
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import com.inazumadraft.R
import com.inazumadraft.data.formationCoordinates
import com.inazumadraft.model.Player
import kotlin.math.min

class FinalTeamActivity : AppCompatActivity() {

    private lateinit var fieldLayout: RelativeLayout
    private val finalTeam: MutableList<Player> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_final_team)

        fieldLayout = findViewById(R.id.fieldLayout)

        // Recoge datos del Intent
        intent.getParcelableArrayListExtra<Player>("finalTeam")?.let { finalTeam.addAll(it) }
        val formationName = intent.getStringExtra("formationName").orEmpty()

        // Dibuja cuando el layout ya tiene medidas
        fieldLayout.post {
            drawTeamOnField(formationName)
        }
    }

    private fun drawTeamOnField(formationName: String) {
        val coords = formationCoordinates[formationName] ?: return
        fieldLayout.removeAllViews()
        fieldLayout.setBackgroundResource(R.drawable.football_field)

        val count = min(finalTeam.size, coords.size)

        for (i in 0 until count) {
            val player = finalTeam[i]
            val (xf, yf) = coords[i]

            val img = ImageView(this).apply {
                layoutParams = RelativeLayout.LayoutParams(140, 140)
                setImageResource(player.image)
            }

            // Posición proporcional al tamaño del campo
            img.x = fieldLayout.width * xf - 70
            img.y = fieldLayout.height * yf - 70

            fieldLayout.addView(img)
        }
    }
}
