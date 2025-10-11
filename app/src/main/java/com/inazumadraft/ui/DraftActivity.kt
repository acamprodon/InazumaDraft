package com.inazumadraft.ui

import androidx.recyclerview.widget.GridLayoutManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.inazumadraft.R
import com.inazumadraft.data.*
import com.inazumadraft.model.Player

class DraftActivity : AppCompatActivity() {

    private lateinit var fieldLayout: RelativeLayout
    private lateinit var btnFormation1: Button
    private lateinit var btnFormation2: Button
    private lateinit var btnFormation3: Button
    private lateinit var rvOptions: RecyclerView  // 👈 Asegúrate de tener ESTA línea
    private lateinit var btnNext: Button
    private lateinit var roundTitle: TextView

    private var selectedFormation: Formation? = null
    private val selectedPlayers: MutableList<Player> = mutableListOf()
    private var captain: Player? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_draft)

        // 🔹 Enlazamos vistas (aquí rvOptions ya existe)
        fieldLayout = findViewById(R.id.fieldLayout)
        btnFormation1 = findViewById(R.id.btnFormation1)
        btnFormation2 = findViewById(R.id.btnFormation2)
        btnFormation3 = findViewById(R.id.btnFormation3)
        rvOptions = findViewById(R.id.rvOptions) // 👈 IMPORTANTE
        btnNext = findViewById(R.id.btnNext)
        roundTitle = findViewById(R.id.roundTitle)

        rvOptions.layoutManager = GridLayoutManager(this, 2)

        btnFormation1.setOnClickListener { selectFormation(0) }
        btnFormation2.setOnClickListener { selectFormation(1) }
        btnFormation3.setOnClickListener { selectFormation(2) }

        btnNext.setOnClickListener {
            if (selectedPlayers.size == 5 && captain != null) {
                val intent = Intent(this, FinalTeamActivity::class.java)
                intent.putParcelableArrayListExtra("finalTeam", ArrayList(selectedPlayers))
                intent.putExtra("formation", selectedFormation!!.name)
                startActivity(intent)
            }
        }
    }

    private fun selectFormation(index: Int) {
        selectedFormation = formations[index]
        roundTitle.text = "Selecciona jugadores para la formación ${selectedFormation!!.name}"
        selectedPlayers.clear()
        captain = null
        showPlayerOptions()
    }

    private fun showPlayerOptions() {
        rvOptions.visibility = View.VISIBLE
        btnNext.visibility = View.GONE

        val remainingPositions = selectedFormation!!.positions.subList(
            selectedPlayers.size,
            selectedFormation!!.positions.size
        )

        val positionToPick = remainingPositions.first()
        val options = PlayerRepository.players.filter {
            it.position == positionToPick && it !in selectedPlayers
        }

        rvOptions.adapter = OptionAdapter(options) { player ->
            selectedPlayers.add(player)
            if (selectedPlayers.size == selectedFormation!!.positions.size) {
                roundTitle.text = "Elige el capitán"
                showCaptainSelection()
            } else {
                showPlayerOptions()
            }
        }
    }

    private fun showCaptainSelection() {
        rvOptions.adapter = OptionAdapter(selectedPlayers) { player ->
            captain = player
            btnNext.visibility = View.VISIBLE
            rvOptions.visibility = View.GONE
            roundTitle.text = "Capitán seleccionado: ${captain!!.name}"
        }
    }
}
