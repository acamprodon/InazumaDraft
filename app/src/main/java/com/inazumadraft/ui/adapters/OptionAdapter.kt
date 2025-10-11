package com.inazumadraft.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.inazumadraft.R
import com.inazumadraft.model.Player

class OptionAdapter(
    private val players: List<Player>,
    private val onClick: (Player) -> Unit
) : RecyclerView.Adapter<OptionAdapter.OptionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_player_option, parent, false)
        return OptionViewHolder(view)
    }

    override fun onBindViewHolder(holder: OptionViewHolder, position: Int) {
        holder.bind(players[position])
    }

    override fun getItemCount(): Int = players.size

    inner class OptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtPlayerName: TextView = itemView.findViewById(R.id.txtPlayerName)
        private val imgPlayer: ImageView = itemView.findViewById(R.id.imgPlayer)

        fun bind(player: Player) {
            txtPlayerName.text = player.name
            imgPlayer.setImageResource(player.image) // Asegúrate de que Player tenga imageRes: Int
            itemView.setOnClickListener { onClick(player) }
        }
    }
}
