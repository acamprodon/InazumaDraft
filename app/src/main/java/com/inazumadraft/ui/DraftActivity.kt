package com.inazumadraft.ui

import androidx.recyclerview.widget.GridLayoutManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.inazumadraft.R
import com.inazumadraft.data.Formation
import com.inazumadraft.data.formations
import com.inazumadraft.data.PlayerRepository
import com.inazumadraft.model.Player

class DraftActivity : AppCompatActivity() {

    private lateinit var fieldLayout: RelativeLayout
    private lateinit var btnFormation1: Button
    private lateinit var btnFormation2: Button
    private lateinit var btnFormation3: Button
    private lateinit var rvOptions: RecyclerView
    private lateinit var btnNext: Button
    private lateinit var roundTitle: TextView

    private var selectedFormation: Formation? = null
    private val selectedPlayers: MutableList<Player> = mutableListOf()
    private var captain: Player? = null

    // ✅ Posiciones pendientes tras elegir capitán (en códigos PT/DF/MC/DL)
    private var pendingPositions: MutableList<String> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_draft)

        fieldLayout = findViewById(R.id.fieldLayout)
        btnFormation1 = findViewById(R.id.btnFormation1)
        btnFormation2 = findViewById(R.id.btnFormation2)
        btnFormation3 = findViewById(R.id.btnFormation3)
        rvOptions = findViewById(R.id.rvOptions)
        btnNext = findViewById(R.id.btnNext)
        roundTitle = findViewById(R.id.roundTitle)

        rvOptions.layoutManager = GridLayoutManager(this, 2)

        btnFormation1.setOnClickListener { selectFormation(0) }
        btnFormation2.setOnClickListener { selectFormation(1) }
        btnFormation3.setOnClickListener { selectFormation(2) }

        btnNext.setOnClickListener {
            val formation = selectedFormation ?: return@setOnClickListener
            // Validación: capitán + todos los pendientes
            if (captain != null && selectedPlayers.size == pendingPositions.size) {
                val intent = Intent(this, FinalTeamActivity::class.java)

                // Construye el equipo final en el orden de la formación original
                val orderedTeam = buildFinalTeamOrdered(formation, captain!!, selectedPlayers)

                intent.putParcelableArrayListExtra("finalTeam", ArrayList(orderedTeam))
                intent.putExtra("formation", formation.name)
                intent.putExtra("captainName", captain!!.name)
                startActivity(intent)
            }
        }
    }

    private fun selectFormation(index: Int) {
        selectedFormation = formations[index]
        selectedPlayers.clear()
        captain = null
        pendingPositions.clear()
        roundTitle.text = "Elige tu capitán"
        showCaptainOptions()
    }

    // ---- Helpers de posiciones ----
    private fun toCode(pos: String): String = when (pos.trim().lowercase()) {
        "portero", "pt" -> "PT"
        "defensa", "df" -> "DF"
        "centrocampista", "mc" -> "MC"
        "delantero", "dl" -> "DL"
        else -> pos.trim().uppercase()
    }

    // 🟡 PRIMERA RONDA: Elección de capitán
    private fun showCaptainOptions() {
        rvOptions.visibility = View.VISIBLE
        btnNext.visibility = View.GONE

        val options = PlayerRepository.players.shuffled().take(4)
        rvOptions.adapter = com.inazumadraft.ui.OptionAdapter(options) { player ->
            captain = player
            // Construir posiciones pendientes = formación - (una ocurrencia de la posición del capitán)
            val formation = selectedFormation ?: return@OptionAdapter
            pendingPositions = formation.positions.map { toCode(it) }.toMutableList()
            // Quita UNA sola ocurrencia del rol del capitán
            pendingPositions.remove(captain!!.position.uppercase())

            roundTitle.text = "Capitán: ${player.name}\nAhora elige tus jugadores"
            showNextPositionOptions()
        }
    }

    // ⚽ Siguientes rondas: por la siguiente posición pendiente
    private fun showNextPositionOptions() {
        rvOptions.visibility = View.VISIBLE
        btnNext.visibility = View.GONE

        if (pendingPositions.isEmpty()) {
            // Nada pendiente (no debería ocurrir aquí)
            rvOptions.visibility = View.GONE
            btnNext.visibility = View.VISIBLE
            return
        }

        // La siguiente posición a cubrir es la de índice == selectedPlayers.size
        val nextIndex = selectedPlayers.size
        val positionCode = pendingPositions[nextIndex] // PT/DF/MC/DL

        // Filtra opciones válidas para esa posición
        val options = PlayerRepository.players
            .filter {
                it.position.equals(positionCode, ignoreCase = true) &&
                        it !in selectedPlayers &&
                        it != captain
            }
            .shuffled()
            .take(4)

        roundTitle.text = "Elige: $positionCode (${nextIndex + 1}/${pendingPositions.size})"

        rvOptions.adapter = com.inazumadraft.ui.OptionAdapter(options) { player ->
            selectedPlayers.add(player)

            if (selectedPlayers.size == pendingPositions.size) {
                // ✅ Equipo completo: capitán + todos los pendientes
                roundTitle.text = "Equipo completo. Capitán: ${captain!!.name}"
                rvOptions.visibility = View.GONE
                btnNext.visibility = View.VISIBLE
            } else {
                showNextPositionOptions()
            }
        }
    }

    // Construye el equipo en el orden de la formación (incluyendo capitán en su sitio)
    private fun buildFinalTeamOrdered(
        formation: Formation,
        captain: Player,
        others: List<Player>
    ): List<Player> {
        val result = mutableListOf<Player>()
        val used = mutableSetOf<Player>()
        val othersMutable = others.toMutableList()

        formation.positions.forEach { posName ->
            val code = toCode(posName)

            val chosen = when {
                captain.position.equals(code, ignoreCase = true) && !used.contains(captain) -> {
                    captain
                }
                else -> {
                    val idx = othersMutable.indexOfFirst {
                        it.position.equals(code, ignoreCase = true) && !used.contains(it)
                    }
                    if (idx >= 0) othersMutable.removeAt(idx) else null
                }
            }

            if (chosen != null) {
                result.add(chosen)
                used.add(chosen)
            }
        }

        // Por seguridad: si faltara alguno, rellena con restantes
        if (result.size < formation.positions.size) {
            (listOf(captain) + others)
                .filterNot { it in result }
                .take(formation.positions.size - result.size)
                .forEach { result.add(it) }
        }

        return result
    }
}
