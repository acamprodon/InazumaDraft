package com.inazumadraft.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.inazumadraft.R
import com.inazumadraft.model.Player
import android.widget.ImageView
import com.inazumadraft.ui.loadPlayerImage
class FinalTeamAdapter(private val players: List<Player>) :
    RecyclerView.Adapter<FinalTeamAdapter.PlayerViewHolder>() {

    class PlayerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtPlayerName: TextView = itemView.findViewById(R.id.txtPlayerName)
        val txtPlayerPosition: TextView = itemView.findViewById(R.id.txtPlayerPosition)
        val txtStats: TextView = itemView.findViewById(R.id.txtStats)
        val imgPlayer: ImageView = itemView.findViewById(R.id.imgPlayer)
        val imgElement: ImageView = itemView.findViewById(R.id.imgElement)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_final_team, parent, false)
        return PlayerViewHolder(view)
    }

    override fun getItemCount(): Int = players.size

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        val p = players[position]

        // Imagen y nombre
        holder.imgPlayer.loadPlayerImage(p.image)
        holder.imgElement.setImageResource(p.element)
        holder.txtPlayerName.text = p.name

        // ---- Posición principal + secundarias ----
        fun label(code: String): String = when (code.trim().uppercase()) {
            "PT" -> "PT"
            "DF" -> "DF"
            "MC" -> "MC"
            "DL" -> "DL"
            else -> code.uppercase()
        }

        val primary = label(p.position)
        val seconds = p.secondaryPositions
            .filter { it.isNotBlank() }
            .map { label(it) }

        holder.txtPlayerPosition.text =
            if (seconds.isNotEmpty()) "$primary (${seconds.joinToString(", ")})"
            else primary

        // Stats
        holder.txtStats.text =
            "Tiro: ${p.kick} | Vel: ${p.speed} | Ctrl: ${p.control} | Def: ${p.defense}"
    }
}
