package com.inazumadraft.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.inazumadraft.R
import com.inazumadraft.data.Formation
import com.inazumadraft.data.formationCoordinates
import com.inazumadraft.data.formations
import com.inazumadraft.data.PlayerRepository
import com.inazumadraft.model.Player
import kotlin.math.min

class DraftActivity : AppCompatActivity() {

    private lateinit var fieldLayout: RelativeLayout
    private lateinit var btnFormation1: Button
    private lateinit var btnFormation2: Button
    private lateinit var btnFormation3: Button
    private lateinit var btnNext: Button
    private lateinit var roundTitle: TextView
    private lateinit var rvOptions: RecyclerView

    private var selectedFormation: Formation? = null
    private var captain: Player? = null

    // Representa los jugadores colocados por slot (mismo tamaño que positions)
    private var occupiedSlots: MutableList<Player?> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_draft)

        fieldLayout   = findViewById(R.id.fieldLayout)
        btnFormation1 = findViewById(R.id.btnFormation1)
        btnFormation2 = findViewById(R.id.btnFormation2)
        btnFormation3 = findViewById(R.id.btnFormation3)
        btnNext       = findViewById(R.id.btnNext)
        roundTitle    = findViewById(R.id.roundTitle)

        // RecyclerView de opciones (añadido dinámicamente bajo los botones de formación)
        rvOptions = RecyclerView(this).apply {
            layoutManager = GridLayoutManager(this@DraftActivity, 2)
            visibility = View.GONE
        }
        val root = findViewById<RelativeLayout>(R.id.draftLayout)
        val params = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            addRule(RelativeLayout.BELOW, R.id.formationButtonsLayout)
            topMargin = 16
        }
        root.addView(rvOptions, params)

        btnFormation1.setOnClickListener { selectFormation(0) }
        btnFormation2.setOnClickListener { selectFormation(1) }
        btnFormation3.setOnClickListener { selectFormation(2) }

        btnNext.setOnClickListener {
            val finalTeamOrdered = ArrayList(occupiedSlots.filterNotNull()) // orden por slot
            val intent = Intent(this, FinalTeamActivity::class.java)
            intent.putParcelableArrayListExtra("finalTeam", finalTeamOrdered)
            intent.putExtra("formationName", selectedFormation?.name ?: "")
            startActivity(intent)
        }

    }

    /** ===================== LÓGICA PRINCIPAL ===================== **/

    private fun selectFormation(index: Int) {
        selectedFormation = formations[index]
        captain = null

        // Inicializa slots vacíos acorde a la formación elegida
        val size = selectedFormation!!.positions.size
        occupiedSlots = MutableList(size) { null }

        roundTitle.text = "Elige tu capitán (4 opciones aleatorias)"
        showCaptainOptions()
    }

    /** 1) Elegir capitán entre 4 opciones aleatorias */
    private fun showCaptainOptions() {
        rvOptions.visibility = View.VISIBLE
        fieldLayout.visibility = View.GONE
        btnNext.visibility = View.GONE

        val options = PlayerRepository.getRandomPlayers(4)
        rvOptions.adapter = OptionAdapter(options) { chosen ->
            captain = chosen
            placeCaptainAtHisPosition(chosen)
        }
    }

    /** Coloca al capitán en el slot cuya posición coincide con su posición natural */
    private fun placeCaptainAtHisPosition(captain: Player) {
        val formation = selectedFormation ?: return
        val coords = formationCoordinates[formation.name] ?: return

        // Busca el primer índice de slot cuya posición coincida con la del capitán
        val slotIndex = firstMatchingSlotIndex(formation, captain.position)
            ?: 0 // fallback: si no encuentra, lo pone en el 0 (pero ya no debería pasar)

        // Marca slot y dibuja
        occupiedSlots[slotIndex] = captain

        // Muestra campo, slots y capitán en su sitio
        drawFieldWithSlots(formation, coords) {
            drawPlayerAtSlot(captain, slotIndex, coords)
            roundTitle.text = "Capitán: ${captain.name}. Ahora rellena las posiciones."
            // Empieza ciclo de selección por slots vacíos
            showNextPositionSelection()
        }
    }

    /** 2) Selección de jugadores por el siguiente slot vacío, mostrando 4 candidatos */
    private fun showNextPositionSelection() {
        val formation = selectedFormation ?: return
        val coords = formationCoordinates[formation.name] ?: return

        val nextSlot = occupiedSlots.indexOfFirst { it == null }
        if (nextSlot == -1) {
            // Equipo completo
            btnNext.visibility = View.VISIBLE
            rvOptions.visibility = View.GONE
            roundTitle.text = "Equipo completo. Pulsa confirmar."
            return
        }

        val requiredPosition = formation.positions[nextSlot]
        roundTitle.text = "Elige un ${requiredPosition}"

        // Candidatos que coindicen con la posición del slot y no estén ya colocados
        val candidates = PlayerRepository.players
            .filter { matchesPosition(it.position, requiredPosition) && it !in occupiedSlots.filterNotNull() }
            .shuffled()
            .take(4)

        if (candidates.isEmpty()) {
            // Si no hay candidatos, no crasheamos; pasamos al siguiente slot o informamos
            rvOptions.visibility = View.GONE
            roundTitle.text = "No hay jugadores para $requiredPosition. (Revisa posiciones en PlayerRepository)"
            return
        }

        rvOptions.visibility = View.VISIBLE
        rvOptions.adapter = OptionAdapter(candidates) { chosen ->
            occupiedSlots[nextSlot] = chosen
            drawPlayerAtSlot(chosen, nextSlot, coords)
            showNextPositionSelection()
        }
    }

    /** ===================== DIBUJO DEL CAMPO ===================== **/

    private fun drawFieldWithSlots(
        formation: Formation,
        coords: List<Pair<Float, Float>>,
        afterDraw: () -> Unit = {}
    ) {
        fieldLayout.visibility = View.VISIBLE
        fieldLayout.setBackgroundResource(R.drawable.football_field)
        fieldLayout.removeAllViews()

        // Dibuja todos los slots vacíos
        fieldLayout.post {
            val count = min(formation.positions.size, coords.size)
            for (i in 0 until count) {
                val (x, y) = coords[i]
                val slot = ImageView(this).apply {
                    layoutParams = RelativeLayout.LayoutParams(130, 130)
                    setImageResource(R.drawable.empty_slot) // crea este drawable (círculo gris)
                    this.x = fieldLayout.width * x - 65
                    this.y = fieldLayout.height * y - 65
                }
                fieldLayout.addView(slot)
            }
            afterDraw()
        }
    }

    private fun drawPlayerAtSlot(player: Player, slotIndex: Int, coords: List<Pair<Float, Float>>) {
        if (slotIndex !in coords.indices) return
        fieldLayout.post {
            val (x, y) = coords[slotIndex]
            val img = ImageView(this).apply {
                layoutParams = RelativeLayout.LayoutParams(
                    if (occupiedSlots[slotIndex] == captain) 150 else 130,
                    if (occupiedSlots[slotIndex] == captain) 150 else 130
                )
                setImageResource(player.image)
                this.x = fieldLayout.width * x - (layoutParams.width / 2f)
                this.y = fieldLayout.height * y - (layoutParams.height / 2f)
            }
            fieldLayout.addView(img)
        }
    }

    /** ===================== HELPERS ===================== **/

    /** Devuelve el primer índice de slot cuya posición coincide con la posición pedida */
    private fun firstMatchingSlotIndex(formation: Formation, position: String): Int? {
        formation.positions.forEachIndexed { index, required ->
            if (occupiedSlots[index] == null && matchesPosition(position, required)) {
                return index
            }
        }
        return null
    }

    /**
     * Normaliza equivalencias de posición (p.ej. "Mediocampista" == "Centrocampista")
     * y comparación case-insensitive.
     */
    private fun matchesPosition(playerPos: String, requiredPos: String): Boolean {
        val p = normalize(playerPos)
        val r = normalize(requiredPos)
        if (p == r) return true

        // equivalencia Mediocampista <-> Centrocampista
        val midSet = setOf("mediocampista", "centrocampista")
        if (midSet.contains(p) && midSet.contains(r)) return true

        return false
    }

    private fun normalize(s: String): String = s.trim().lowercase()
}
