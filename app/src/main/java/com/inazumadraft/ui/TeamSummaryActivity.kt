package com.inazumadraft.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.inazumadraft.R
import com.inazumadraft.model.Player

class TeamSummaryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_team_summary)

        // Solo el 11 titular, sin banquillo
        val fieldPlayers = intent.getParcelableArrayListExtra<Player>("finalTeam") ?: arrayListOf()

        if (fieldPlayers.isEmpty()) return

        // Calculamos las medias solo con esos 11 jugadores
        val avgKick = fieldPlayers.map { it.kick }.average()
        val avgControl = fieldPlayers.map { it.control }.average()
        val avgDefense = fieldPlayers.map { it.defense }.average()
        val avgSpeed = fieldPlayers.map { it.speed }.average()

        // Se añade la mitad de la velocidad a cada estadística principal
        val totalAttack = avgKick + (avgSpeed / 2)
        val totalControl = avgControl + (avgSpeed / 2)
        val totalDefense = avgDefense + (avgSpeed / 2)

        // Puntuación general = media de las tres totales
        val teamScore = ((totalAttack + totalControl + totalDefense) / 3).toInt()

        // Mostrar resultados redondeados
        findViewById<TextView>(R.id.txtAttack).text = "Ataque: ${totalAttack.toInt()}"
        findViewById<TextView>(R.id.txtControl).text = "Control: ${totalControl.toInt()}"
        findViewById<TextView>(R.id.txtDefense).text = "Defensa: ${totalDefense.toInt()}"
        findViewById<TextView>(R.id.txtScore).text = "Puntuación del equipo: $teamScore"

        findViewById<Button>(R.id.btnBack).setOnClickListener {
            val intent = Intent(this, FinalTeamActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }

    }

}
