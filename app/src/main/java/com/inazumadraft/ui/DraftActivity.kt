package com.inazumadraft.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.inazumadraft.R
import com.inazumadraft.data.Formation
import com.inazumadraft.data.PlayerRepository
import com.inazumadraft.data.formations
import com.inazumadraft.model.Player

class DraftActivity : AppCompatActivity() {

    private lateinit var fieldLayout: RelativeLayout
    private lateinit var btnFormation1: Button
    private lateinit var btnFormation2: Button
    private lateinit var btnFormation3: Button
    private lateinit var btnFormation4: Button
    private lateinit var rvOptions: RecyclerView
    private lateinit var btnNext: Button
    private lateinit var roundTitle: TextView

    private var visibleFormations: List<Formation> = emptyList()
    private var selectedFormation: Formation? = null

    private val selectedPlayers: MutableList<Player> = mutableListOf()
    private var captain: Player? = null

    // Posiciones pendientes (en códigos PT/DF/MC/DL) tras elegir capitán
    private var pendingPositions: MutableList<String> = mutableListOf()

    // 🔒 Candado: cuando es true ya no se puede cambiar de formación
    private var formationLocked: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_draft)

        // Vistas
        fieldLayout = findViewById(R.id.fieldLayout)
        btnFormation1 = findViewById(R.id.btnFormation1)
        btnFormation2 = findViewById(R.id.btnFormation2)
        btnFormation3 = findViewById(R.id.btnFormation3)
        btnFormation4 = findViewById(R.id.btnFormation4)
        rvOptions = findViewById(R.id.rvOptions)
        btnNext = findViewById(R.id.btnNext)
        roundTitle = findViewById(R.id.roundTitle)

        // Cuadrícula 2x2 para ver 4 opciones sin scroll
        rvOptions.layoutManager = GridLayoutManager(this, 2)

        // 4 formaciones aleatorias visibles
        refreshVisibleFormations()

        // Listeners (respetan el candado)
        btnFormation1.setOnClickListener { selectFormationByIndex(0) }
        btnFormation2.setOnClickListener { selectFormationByIndex(1) }
        btnFormation3.setOnClickListener { selectFormationByIndex(2) }
        btnFormation4.setOnClickListener { selectFormationByIndex(3) }

        // Confirmar equipo
        btnNext.setOnClickListener {
            val formation = selectedFormation ?: return@setOnClickListener
            if (captain != null && selectedPlayers.size == pendingPositions.size) {
                val orderedTeam = buildFinalTeamOrdered(formation, captain!!, selectedPlayers)
                val intent = Intent(this, FinalTeamActivity::class.java)
                intent.putParcelableArrayListExtra("finalTeam", ArrayList(orderedTeam))
                intent.putExtra("formation", formation.name)
                intent.putExtra("captainName", captain!!.name)
                startActivity(intent)
            }
        }
    }

    // --- Formaciones visibles (4 aleatorias) ---
    private fun refreshVisibleFormations() {
        visibleFormations = if (formations.size >= 4) formations.shuffled().take(4) else formations
        btnFormation1.text = visibleFormations.getOrNull(0)?.name ?: "—"
        btnFormation2.text = visibleFormations.getOrNull(1)?.name ?: "—"
        btnFormation3.text = visibleFormations.getOrNull(2)?.name ?: "—"
        btnFormation4.text = visibleFormations.getOrNull(3)?.name ?: "—"

        // Reset estado visual
        rvOptions.visibility = View.GONE
        btnNext.visibility = View.GONE
        fieldLayout.visibility = View.GONE
        roundTitle.text = "Elige una formación"
        selectedPlayers.clear()
        captain = null
        pendingPositions.clear()

        // Asegura que los botones estén habilitados cuando entras por primera vez
        setFormationButtonsEnabled(true)
    }

    private fun selectFormationByIndex(i: Int) {
        // Si ya se eligió una, ignora nuevos clics
        if (formationLocked) return

        val f = visibleFormations.getOrNull(i) ?: return
        selectedFormation = f
        selectedPlayers.clear()
        captain = null
        pendingPositions.clear()

        rvOptions.visibility = View.GONE
        btnNext.visibility = View.GONE
        fieldLayout.visibility = View.GONE

        roundTitle.text = "Elige tu capitán (${f.name})"
        showCaptainOptions()

        // 🔒 Activa candado y deshabilita botones
        formationLocked = true
        setFormationButtonsEnabled(false)

        // (Opcional) resaltar el botón escogido
        highlightChosenButton(i)
    }

    private fun setFormationButtonsEnabled(enabled: Boolean) {
        val alpha = if (enabled) 1f else 0.5f
        listOf(btnFormation1, btnFormation2, btnFormation3, btnFormation4).forEach {
            it.isEnabled = enabled
            it.alpha = alpha
        }
    }

    private fun highlightChosenButton(index: Int) {
        // Baja un poco la opacidad de los no elegidos para dejar claro cuál se escogió
        listOf(btnFormation1, btnFormation2, btnFormation3, btnFormation4).forEachIndexed { idx, b ->
            b.alpha = if (idx == index) 1f else 0.4f
        }
    }

    // --- Helpers de posiciones ---
    private fun toCode(pos: String): String = when (pos.trim().lowercase()) {
        "portero", "pt" -> "PT"
        "defensa", "df" -> "DF"
        "centrocampista", "mc" -> "MC"
        "delantero", "dl" -> "DL"
        else -> pos.trim().uppercase()
    }

    private fun codeToLabel(code: String): String = when (code.uppercase()) {
        "PT" -> "Portero"
        "DF" -> "Defensa"
        "MC" -> "Centrocampista"
        "DL" -> "Delantero"
        else -> code
    }

    // --- 1) Selección de capitán (4 opciones aleatorias sin filtrar por posición) ---
    private fun showCaptainOptions() {
        rvOptions.visibility = View.VISIBLE
        btnNext.visibility = View.GONE

        val options = PlayerRepository.players.shuffled().take(4)
        rvOptions.adapter = OptionAdapter(options) { player ->
            captain = player

            val formation = selectedFormation ?: return@OptionAdapter
            pendingPositions = formation.positions.map { toCode(it) }.toMutableList()
            // Quita SOLO UNA ocurrencia del rol del capitán para no repetir posición
            pendingPositions.remove(captain!!.position.uppercase())

            roundTitle.text = "Capitán: ${player.name}\nAhora elige tus jugadores (${pendingPositions.size} restantes)"
            showNextPositionOptions()
        }
    }

    // --- 2) Rondas por posición: siempre 4 opciones ---
    private fun showNextPositionOptions() {
        rvOptions.visibility = View.VISIBLE
        btnNext.visibility = View.GONE

        if (pendingPositions.isEmpty()) {
            rvOptions.visibility = View.GONE
            btnNext.visibility = View.VISIBLE
            return
        }

        val nextIndex = selectedPlayers.size
        val positionCode = pendingPositions[nextIndex] // PT/DF/MC/DL

        val options = PlayerRepository.players
            .filter {
                it.position.equals(positionCode, ignoreCase = true) &&
                        it !in selectedPlayers &&
                        it != captain
            }
            .shuffled()
            .take(4)

        roundTitle.text = "Elige: ${codeToLabel(positionCode)} (${nextIndex + 1}/${pendingPositions.size})"

        rvOptions.adapter = OptionAdapter(options) { player ->
            selectedPlayers.add(player)
            if (selectedPlayers.size == pendingPositions.size) {
                roundTitle.text = "Equipo completo. Capitán: ${captain!!.name}"
                rvOptions.visibility = View.GONE
                btnNext.visibility = View.VISIBLE
            } else {
                showNextPositionOptions()
            }
        }
    }

    // --- Construye el equipo en el orden de la formación (capitán en su sitio real) ---
    private fun buildFinalTeamOrdered(
        formation: Formation,
        captain: Player,
        others: List<Player>
    ): List<Player> {
        val result = mutableListOf<Player>()
        val used = mutableSetOf<Player>()
        val pool = others.toMutableList()

        formation.positions.forEach { posName ->
            val code = toCode(posName)
            val chosen = when {
                captain.position.equals(code, ignoreCase = true) && !used.contains(captain) -> captain
                else -> {
                    val idx = pool.indexOfFirst {
                        it.position.equals(code, ignoreCase = true) && !used.contains(it)
                    }
                    if (idx >= 0) pool.removeAt(idx) else null
                }
            }
            if (chosen != null) {
                result.add(chosen)
                used.add(chosen)
            }
        }

        // Relleno de seguridad
        if (result.size < formation.positions.size) {
            (listOf(captain) + others)
                .filterNot { it in result }
                .take(formation.positions.size - result.size)
                .forEach { result.add(it) }
        }

        return result
    }
}
