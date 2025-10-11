package com.inazumadraft.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.inazumadraft.R
import com.inazumadraft.data.formationCoordinates
import com.inazumadraft.model.Player
import kotlin.math.max
import kotlin.math.min

class FinalTeamActivity : AppCompatActivity() {

    private lateinit var fieldLayout: RelativeLayout
    private lateinit var recyclerFinalTeam: RecyclerView
    private lateinit var btnToggleView: Button
    private var showingStats = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_final_team)

        fieldLayout = findViewById(R.id.fieldLayout)
        recyclerFinalTeam = findViewById(R.id.recyclerFinalTeam)
        btnToggleView = findViewById(R.id.btnToggleView)

        val players = intent.getParcelableArrayListExtra<Player>("finalTeam")
        val formationName = intent.getStringExtra("formation") ?: "4-4-2"
        val captainName = intent.getStringExtra("captainName")

        if (players != null) {
            fieldLayout.post {
                // Limpia y pinta
                fieldLayout.removeAllViews()
                showPlayersOnField(players, formationName, captainName)
            }

            recyclerFinalTeam.layoutManager = LinearLayoutManager(this)
            recyclerFinalTeam.adapter = FinalTeamAdapter(players)

            btnToggleView.setOnClickListener {
                showingStats = !showingStats
                if (showingStats) {
                    fieldLayout.visibility = View.GONE
                    recyclerFinalTeam.visibility = View.VISIBLE
                    btnToggleView.text = "Ver campo"
                } else {
                    fieldLayout.visibility = View.VISIBLE
                    recyclerFinalTeam.visibility = View.GONE
                    btnToggleView.text = "Ver estadísticas"
                }
            }
        }
    }

    /**
     * Versión sin solapes:
     *  - Agrupa por filas (bucket por Y)
     *  - Calcula ancho que cabe por fila
     *  - Ignora X original para la colocación final y reparte en slots equiespaciados (manteniendo orden izq→der)
     */
    private fun showPlayersOnField(playersIn: List<Player>, formationName: String, captainName: String?) {
        val allCoords = formationCoordinates[formationName] ?: return

        // Seguridad: nunca acceder fuera de rango
        val count = min(playersIn.size, allCoords.size)
        if (count == 0) return
        val players = playersIn.take(count)
        val coords = allCoords.take(count)

        // 1) Agrupar por filas usando Y con tolerancia
        data class Pt(val index: Int, val x: Float, val y: Float)
        val eps = 0.045f // tolerancia para “misma línea”
        val rows = mutableMapOf<Int, MutableList<Pt>>() // bucketY -> puntos

        coords.forEachIndexed { i, (x, y) ->
            val bucket = (y / eps).toInt()
            rows.getOrPut(bucket) { mutableListOf() }.add(Pt(i, x, y))
        }

        // 2) Parámetros de tamaño
        val d = resources.displayMetrics.density
        val minW = 80f * d      // ancho mínimo legible
        val maxW = 130f * d     // ancho máximo estético
        val aspect = 1.25f      // alto = ancho * aspect
        val hMargin = 6f * d    // margen horizontal entre cards

        // 3) Para cada fila: ordenar por X (solo para mantener orden) y colocar en slots
        rows.toSortedMap().values.forEach { row ->
            row.sortBy { it.x }

            val n = max(1, row.size)
            val totalMargins = hMargin * (n + 1)
            var w = (fieldLayout.width - totalMargins) / n.toFloat()
            w = min(maxW, max(minW, w))
            val h = w * aspect

            // y físico de la fila = promedio de sus Y
            val yPx = (fieldLayout.height * (row.map { it.y }.average()).toFloat())

            for (i in 0 until n) {
                // Centro X del slot i (equispaciado)
                val slotCenterX = ((i + 1) / (n + 1f)) * fieldLayout.width

                val p = row[i]
                if (p.index !in players.indices) continue
                val player = players[p.index]

                val playerView = layoutInflater.inflate(R.layout.item_player_field, fieldLayout, false)
                val imgPlayer = playerView.findViewById<ImageView>(R.id.imgPlayer)
                val txtName = playerView.findViewById<TextView>(R.id.txtPlayerName)
                val imgElem = playerView.findViewById<ImageView>(R.id.imgElement)

                imgPlayer.setImageResource(player.image)
                txtName.text = player.name
                imgElem.setImageResource(player.element)

                if (player.name == captainName) {
                    imgPlayer.setBackgroundResource(R.drawable.captain_border)
                }

                val params = RelativeLayout.LayoutParams(w.toInt(), h.toInt())
                params.leftMargin = (slotCenterX - w / 2f).toInt()
                params.topMargin = (yPx - h / 2f).toInt()
                playerView.layoutParams = params

                fieldLayout.addView(playerView)
            }
        }
    }
}
