package com.inazumadraft.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.inazumadraft.R
import com.inazumadraft.model.Player
import com.inazumadraft.utils.Tecnicautils

class TeamSummaryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_team_summary)

        val team = intent.getParcelableArrayListExtra<Player>("finalTeam") ?: arrayListOf()

        val txtFinalScore = findViewById<TextView>(R.id.txtFinalScore)
        val txtStats = findViewById<TextView>(R.id.txtStats)
        val txtTechniquesList = findViewById<TextView>(R.id.txtTechniquesList)
        val summaryScroll = findViewById<ScrollView>(R.id.summaryScroll)
        val techniquesScroll = findViewById<ScrollView>(R.id.techniquesScroll)

        // 🔹 Calcular medias base del equipo
        val avgAttack = team.map { it.kick }.average()
        val avgControl = team.map { it.control }.average()
        val avgDefense = team.map { it.defense }.average()
        val avgSpeed = team.map { it.speed }.average()

        val baseScore = (avgAttack + avgControl + avgDefense + (avgSpeed / 2)) / 3

        // 🔹 Calcular técnicas y bonus
        val bonus = Tecnicautils.calculateTechniqueBonus(team)
        val finalScore = baseScore + (bonus / 10.0) // peso del bonus
        val combined = Tecnicautils.getCombinedTecnica(team)

        // 🔹 Mostrar puntuación final y stats
        txtFinalScore.text = "Puntuación final del equipo: ${"%.1f".format(finalScore)}"
        txtStats.text = """
            🏋️‍♂️ Estadísticas base:
            Ataque: ${"%.1f".format(avgAttack)}
            Control: ${"%.1f".format(avgControl)}
            Defensa: ${"%.1f".format(avgDefense)}
            Velocidad: ${"%.1f".format(avgSpeed)}

            🎯 Bonus por técnicas: +$bonus
        """.trimIndent()

        // 🔹 Generar lista de técnicas (individuales + combinadas)
        val techniquesText = buildString {
            append("⚡ Técnicas individuales:\n")
            team.forEach { player ->
                val techs = Tecnicautils.getIndividualTecnica(player)
                if (techs.isNotEmpty()) {
                    append("${player.nickname}:\n")
                    techs.forEach { append("   • ${it.name} (${it.type}) - ${it.power}\n") }
                }
            }

            append("\n🤝 Técnicas combinadas:\n")
            if (combined.isEmpty()) append("   Ninguna desbloqueada\n")
            else combined.forEach { t ->
                append("   • ${t.name} (${t.players.joinToString(", ")}) - ${t.power}\n")
            }
        }

        txtTechniquesList.text = techniquesText

        // 🔹 Botón "Ver Técnicas"
        findViewById<Button>(R.id.btnViewTechniques).setOnClickListener {
            summaryScroll.visibility = View.GONE
            techniquesScroll.visibility = View.VISIBLE
        }

        // 🔹 Botón "Volver" dentro del panel de técnicas
        findViewById<Button>(R.id.btnCloseTechniques).setOnClickListener {
            techniquesScroll.visibility = View.GONE
            summaryScroll.visibility = View.VISIBLE
        }

        // 🔹 Botón "Volver al inicio"
        findViewById<Button>(R.id.btnBack).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
    }
}
