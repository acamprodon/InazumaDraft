package com.inazumadraft.ui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.inazumadraft.R
import com.inazumadraft.model.Player

class TeamSummaryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_team_summary)

        val players = intent.getParcelableArrayListExtra<Player>("finalTeam") ?: arrayListOf()

        val avgAttack = players.map { it.kick }.average().toInt()
        val avgControl = players.map { it.control }.average().toInt()
        val avgDefense = players.map { it.defense }.average().toInt()

        val teamScore = ((avgAttack * 0.4) + (avgControl * 0.3) + (avgDefense * 0.3)).toInt()

        findViewById<TextView>(R.id.txtAttack).text = "Ataque: $avgAttack"
        findViewById<TextView>(R.id.txtControl).text = "Control: $avgControl"
        findViewById<TextView>(R.id.txtDefense).text = "Defensa: $avgDefense"
        findViewById<TextView>(R.id.txtScore).text = "Puntuación del equipo: $teamScore"
    }
}
