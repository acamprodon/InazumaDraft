package com.inazumadraft.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.inazumadraft.R
import com.inazumadraft.model.Player

class TeamAdapter(
    private val players: List<Player>
) : RecyclerView.Adapter<TeamAdapter.TeamViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeamViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_player_field, parent, false)
        return TeamViewHolder(view)
    }

    override fun onBindViewHolder(holder: TeamViewHolder, position: Int) {
        holder.bind(players[position])
    }

    override fun getItemCount(): Int = players.size

    inner class TeamViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtPlayerName: TextView = itemView.findViewById(R.id.txtPlayerName)
        private val imgPlayer: ImageView = itemView.findViewById(R.id.imgPlayer)

        fun bind(player: Player) {
            txtPlayerName.text = player.name
            imgPlayer.setImageResource(player.image)
        }
    }
}
