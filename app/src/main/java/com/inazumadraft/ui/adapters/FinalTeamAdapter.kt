package com.inazumadraft.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.inazumadraft.R
import com.inazumadraft.model.Player // Asegúrate de que Player exista y sea Parcelable
import android.widget.ImageView

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
        val player = players[position]
        holder.txtPlayerName.text = player.name
        holder.txtPlayerPosition.text = player.position
        holder.imgPlayer.setImageResource(player.image)
        holder.imgElement.setImageResource(player.element)
        holder.txtStats.text =
            "Tiro: ${player.kick} | Vel: ${player.speed} | Ctrl: ${player.control} | Def: ${player.defense}"
    }
}
